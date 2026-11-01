package pe.edu.upeu.bomerp.inventario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.bomerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.bomerp.catalogo.producto.service.ProductoService;
import pe.edu.upeu.bomerp.exception.BusinessConflictException;
import pe.edu.upeu.bomerp.inventario.dto.DescuentoFefoRequest;
import pe.edu.upeu.bomerp.inventario.dto.DescuentoFefoResponse;
import pe.edu.upeu.bomerp.inventario.dto.LoteCreateDto;
import pe.edu.upeu.bomerp.inventario.dto.LoteDescontadoItem;
import pe.edu.upeu.bomerp.inventario.dto.LoteDto;
import pe.edu.upeu.bomerp.inventario.dto.MovimientoKardexDto;
import pe.edu.upeu.bomerp.inventario.entity.EstadoLote;
import pe.edu.upeu.bomerp.inventario.entity.Lote;
import pe.edu.upeu.bomerp.inventario.entity.MovimientoKardex;
import pe.edu.upeu.bomerp.inventario.entity.TipoMovimientoKardex;
import pe.edu.upeu.bomerp.inventario.repository.LoteRepository;
import pe.edu.upeu.bomerp.inventario.repository.MovimientoKardexRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventarioServiceImpl implements InventarioService {

    private final LoteRepository loteRepository;
    private final MovimientoKardexRepository kardexRepository;
    private final ProductoService productoService;

    @Override
    public LoteDto registrarLote(LoteCreateDto dto) {
        ProductoResponse producto = productoService.obtener(dto.getProductoId());

        if (loteRepository.findByCodigoLote(dto.getCodigoLote()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un lote registrado con el código: " + dto.getCodigoLote());
        }

        if (dto.getFechaVencimiento().isBefore(dto.getFechaFabricacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de vencimiento no puede ser anterior a la fecha de fabricación");
        }

        EstadoLote estado = calcularEstadoLote(dto.getFechaVencimiento(), dto.getStockInicial());

        Lote lote = Lote.builder()
                .codigoLote(dto.getCodigoLote().trim().toUpperCase())
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .fechaFabricacion(dto.getFechaFabricacion())
                .fechaVencimiento(dto.getFechaVencimiento())
                .stockInicial(dto.getStockInicial())
                .stockActual(dto.getStockInicial())
                .costoUnitario(dto.getCostoUnitario())
                .estado(estado)
                .observaciones(dto.getObservaciones())
                .build();

        Lote guardado = loteRepository.save(lote);

        // Registro en Kardex (ENTRADA por ingreso de lote a almacén)
        MovimientoKardex kardex = MovimientoKardex.builder()
                .tipo(TipoMovimientoKardex.ENTRADA)
                .lote(guardado)
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .cantidad(dto.getStockInicial())
                .saldoStockResultante(dto.getStockInicial())
                .motivo("INGRESO_LOTE_PRODUCCION")
                .documentoReferencia("LOTE-" + guardado.getCodigoLote())
                .fechaHora(LocalDateTime.now())
                .build();
        kardexRepository.save(kardex);

        // Actualizar stock consolidado del producto mediante la interfaz pública del módulo catálogo
        productoService.aumentarStock(producto.getId(), dto.getStockInicial().intValue());

        log.info("Lote {} creado exitosamente para el producto '{}'. Stock inicial: {}",
                guardado.getCodigoLote(), producto.getNombre(), guardado.getStockInicial());

        return mapToDto(guardado);
    }

    @Override
    public DescuentoFefoResponse descontarStockFEFO(DescuentoFefoRequest request) {
        ProductoResponse producto = productoService.obtener(request.getProductoId());

        BigDecimal cantidadSolicitada = request.getCantidad();
        if (cantidadSolicitada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad a descontar debe ser mayor a 0");
        }

        // Consultar lotes disponibles ordenados por FEFO (First Expired, First Out)
        List<Lote> lotesFefo = loteRepository.findLotesParaDespachoFEFO(
                producto.getId(),
                List.of(EstadoLote.DISPONIBLE, EstadoLote.PROXIMO_A_VENCER)
        );

        BigDecimal stockDisponibleTotal = lotesFefo.stream()
                .map(Lote::getStockActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (stockDisponibleTotal.compareTo(cantidadSolicitada) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Stock insuficiente bajo regla FEFO para '%s'. Solicitado: %s, Disponible en lotes vigentes: %s",
                            producto.getNombre(), cantidadSolicitada, stockDisponibleTotal));
        }

        BigDecimal pendiente = cantidadSolicitada;
        List<LoteDescontadoItem> lotesAfectados = new ArrayList<>();

        for (Lote lote : lotesFefo) {
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal disponibleEnLote = lote.getStockActual();
            BigDecimal aDescontar;
            boolean loteQuedaAgotado;

            if (disponibleEnLote.compareTo(pendiente) <= 0) {
                aDescontar = disponibleEnLote;
                lote.setStockActual(BigDecimal.ZERO);
                lote.setEstado(EstadoLote.AGOTADO);
                loteQuedaAgotado = true;
                pendiente = pendiente.subtract(aDescontar);
            } else {
                aDescontar = pendiente;
                BigDecimal nuevoStock = disponibleEnLote.subtract(aDescontar);
                lote.setStockActual(nuevoStock);
                loteQuedaAgotado = false;
                pendiente = BigDecimal.ZERO;
            }

            loteRepository.save(lote);

            MovimientoKardex kardex = MovimientoKardex.builder()
                    .tipo(TipoMovimientoKardex.SALIDA)
                    .lote(lote)
                    .productoId(producto.getId())
                    .productoNombre(producto.getNombre())
                    .cantidad(aDescontar)
                    .saldoStockResultante(lote.getStockActual())
                    .motivo(request.getMotivo() != null ? request.getMotivo() : "DESPACHO_FEFO")
                    .documentoReferencia(request.getDocumentoReferencia())
                    .fechaHora(LocalDateTime.now())
                    .build();
            kardexRepository.save(kardex);

            lotesAfectados.add(LoteDescontadoItem.builder()
                    .loteId(lote.getId())
                    .codigoLote(lote.getCodigoLote())
                    .fechaVencimiento(lote.getFechaVencimiento())
                    .cantidadDescontada(aDescontar)
                    .stockRestanteLote(lote.getStockActual())
                    .loteAgotado(loteQuedaAgotado)
                    .build());
        }

        int aDescontarCatalogo = Math.min(
                producto.getStock() != null ? producto.getStock() : 0,
                cantidadSolicitada.intValue()
        );
        if (aDescontarCatalogo > 0) {
            productoService.descontarStock(producto.getId(), aDescontarCatalogo);
        }

        BigDecimal stockTotalRestanteLotes = stockDisponibleTotal.subtract(cantidadSolicitada);

        log.info("Despacho FEFO completado para producto '{}'. Cantidad: {}. Lotes impactados: {}",
                producto.getNombre(), cantidadSolicitada, lotesAfectados.size());

        return DescuentoFefoResponse.builder()
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .cantidadTotalSolicitada(cantidadSolicitada)
                .cantidadTotalDescontada(cantidadSolicitada)
                .stockTotalRestante(stockTotalRestanteLotes)
                .lotesAfectados(lotesAfectados)
                .build();
    }

    @Override
    public Long registrarSalidaVentaFEFO(Long productoId, String productoNombre, int cantidad, String numeroTicket) {
        if (cantidad <= 0) {
            return null;
        }
        String nombreSeguro = truncarNombre(productoNombre != null ? productoNombre : ("Producto #" + productoId));
        BigDecimal pendiente = BigDecimal.valueOf(cantidad);

        List<Lote> todosLotesProducto = loteRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId);
        List<Lote> lotesVigentes = loteRepository.findLotesParaDespachoFEFO(
                productoId,
                List.of(EstadoLote.DISPONIBLE, EstadoLote.PROXIMO_A_VENCER)
        );

        // Si el producto solo tiene lotes vencidos con saldo y ningún lote vigente suficiente, bloquear por regla sanitaria FEFO
        BigDecimal stockVencido = todosLotesProducto.stream()
                .filter(l -> l.getEstado() == EstadoLote.VENCIDO && l.getStockActual().compareTo(BigDecimal.ZERO) > 0)
                .map(Lote::getStockActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal stockVigente = lotesVigentes.stream()
                .map(Lote::getStockActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (stockVencido.compareTo(BigDecimal.ZERO) > 0 && stockVigente.compareTo(pendiente) < 0) {
            throw new BusinessConflictException(String.format(
                    "Bloqueo sanitario FEFO para '%s': existen unidades en lotes VENCIDOS y el stock en lotes vigentes (%s) no cubre lo solicitado (%d).",
                    nombreSeguro, stockVigente, cantidad
            ));
        }

        Long lotePrincipalId = null;

        for (Lote lote : lotesVigentes) {
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (lotePrincipalId == null) {
                lotePrincipalId = lote.getId();
            }

            BigDecimal disponible = lote.getStockActual();
            BigDecimal aDescontar;
            if (disponible.compareTo(pendiente) <= 0) {
                aDescontar = disponible;
                lote.setStockActual(BigDecimal.ZERO);
                lote.setEstado(EstadoLote.AGOTADO);
                pendiente = pendiente.subtract(aDescontar);
            } else {
                aDescontar = pendiente;
                lote.setStockActual(disponible.subtract(aDescontar));
                lote.setEstado(calcularEstadoLote(lote.getFechaVencimiento(), lote.getStockActual()));
                pendiente = BigDecimal.ZERO;
            }
            loteRepository.save(lote);

            kardexRepository.save(MovimientoKardex.builder()
                    .tipo(TipoMovimientoKardex.SALIDA)
                    .lote(lote)
                    .productoId(productoId)
                    .productoNombre(nombreSeguro)
                    .cantidad(aDescontar)
                    .saldoStockResultante(lote.getStockActual())
                    .motivo("VENTA_POS_FEFO")
                    .documentoReferencia(numeroTicket)
                    .fechaHora(LocalDateTime.now())
                    .build());
        }

        // Si el producto tenía stock base en catálogo sin lote asignado aún, asentar igualmente el movimiento en Kardex
        if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal saldoCatalogo = BigDecimal.ZERO;
            try {
                ProductoResponse p = productoService.obtener(productoId);
                if (p.getStock() != null) {
                    saldoCatalogo = BigDecimal.valueOf(p.getStock());
                }
            } catch (Exception ignored) {
                // fallback silencioso
            }

            kardexRepository.save(MovimientoKardex.builder()
                    .tipo(TipoMovimientoKardex.SALIDA)
                    .lote(null)
                    .productoId(productoId)
                    .productoNombre(nombreSeguro)
                    .cantidad(pendiente)
                    .saldoStockResultante(saldoCatalogo)
                    .motivo("VENTA_POS")
                    .documentoReferencia(numeroTicket)
                    .fechaHora(LocalDateTime.now())
                    .build());
        }

        return lotePrincipalId;
    }

    @Override
    public void revertirSalidaVenta(Long productoId, String productoNombre, Long loteId, int cantidad, String numeroTicket) {
        if (cantidad <= 0) {
            return;
        }
        String nombreSeguro = truncarNombre(productoNombre != null ? productoNombre : ("Producto #" + productoId));
        BigDecimal cant = BigDecimal.valueOf(cantidad);

        Lote loteRestaurado = null;
        BigDecimal saldoResultante = cant;

        if (loteId != null) {
            loteRestaurado = loteRepository.findById(loteId).orElse(null);
            if (loteRestaurado != null) {
                BigDecimal nuevoStock = loteRestaurado.getStockActual().add(cant);
                loteRestaurado.setStockActual(nuevoStock);
                loteRestaurado.setEstado(calcularEstadoLote(loteRestaurado.getFechaVencimiento(), nuevoStock));
                loteRepository.save(loteRestaurado);
                saldoResultante = nuevoStock;
            }
        } else {
            try {
                ProductoResponse p = productoService.obtener(productoId);
                if (p.getStock() != null) {
                    saldoResultante = BigDecimal.valueOf(p.getStock());
                }
            } catch (Exception ignored) {
                // fallback
            }
        }

        kardexRepository.save(MovimientoKardex.builder()
                .tipo(TipoMovimientoKardex.ENTRADA)
                .lote(loteRestaurado)
                .productoId(productoId)
                .productoNombre(nombreSeguro)
                .cantidad(cant)
                .saldoStockResultante(saldoResultante)
                .motivo("ANULACION_VENTA_POS")
                .documentoReferencia(numeroTicket)
                .fechaHora(LocalDateTime.now())
                .build());
    }

    @Override
    public LoteDto registrarIngresoProduccion(
            Long productoId,
            String productoNombre,
            String codigoLote,
            int cantidadProducida,
            LocalDate fechaVencimiento,
            String codigoOrden) {

        String nombreSeguro = truncarNombre(productoNombre != null ? productoNombre : ("Producto #" + productoId));
        BigDecimal stock = BigDecimal.valueOf(cantidadProducida);
        LocalDate hoy = LocalDate.now();
        LocalDate vencimiento = (fechaVencimiento != null && !fechaVencimiento.isBefore(hoy))
                ? fechaVencimiento
                : hoy.plusYears(1);

        BigDecimal costoEstimado = new BigDecimal("15.00");
        try {
            ProductoResponse p = productoService.obtener(productoId);
            if (p.getPrecio() != null) {
                costoEstimado = p.getPrecio().multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
            }
        } catch (Exception ignored) {
            // Usa costo base si no está en catálogo
        }

        String codUnico = codigoLote != null ? codigoLote.trim().toUpperCase() : ("LOT-" + System.currentTimeMillis());
        if (loteRepository.findByCodigoLote(codUnico).isPresent()) {
            codUnico = codUnico + "-P";
        }

        EstadoLote estado = calcularEstadoLote(vencimiento, stock);

        Lote lote = Lote.builder()
                .codigoLote(codUnico)
                .productoId(productoId)
                .productoNombre(nombreSeguro)
                .fechaFabricacion(hoy)
                .fechaVencimiento(vencimiento)
                .stockInicial(stock)
                .stockActual(stock)
                .costoUnitario(costoEstimado)
                .estado(estado)
                .observaciones("Ingreso automático desde Orden de Producción " + codigoOrden)
                .build();

        Lote guardado = loteRepository.save(lote);

        kardexRepository.save(MovimientoKardex.builder()
                .tipo(TipoMovimientoKardex.ENTRADA)
                .lote(guardado)
                .productoId(productoId)
                .productoNombre(nombreSeguro)
                .cantidad(stock)
                .saldoStockResultante(stock)
                .motivo("INGRESO_PLANTA_BOM")
                .documentoReferencia(codigoOrden)
                .fechaHora(LocalDateTime.now())
                .build());

        log.info("Lote FEFO {} unificado en Inventario desde Orden de Producción {}", guardado.getCodigoLote(), codigoOrden);
        return mapToDto(guardado);
    }

    @Override
    public void registrarConsumoInsumoBom(
            Long insumoId,
            String nombreInsumo,
            BigDecimal cantidadConsumida,
            String unidadMedida,
            String codigoOrden) {

        if (cantidadConsumida == null || cantidadConsumida.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String etiqueta = truncarNombre(nombreInsumo + (unidadMedida != null ? " [" + unidadMedida + "]" : ""));
        BigDecimal pendiente = cantidadConsumida;

        List<Lote> lotesInsumo = loteRepository.findLotesParaDespachoFEFO(
                insumoId,
                List.of(EstadoLote.DISPONIBLE, EstadoLote.PROXIMO_A_VENCER)
        );

        for (Lote lote : lotesInsumo) {
            if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal disp = lote.getStockActual();
            BigDecimal desc;
            if (disp.compareTo(pendiente) <= 0) {
                desc = disp;
                lote.setStockActual(BigDecimal.ZERO);
                lote.setEstado(EstadoLote.AGOTADO);
                pendiente = pendiente.subtract(desc);
            } else {
                desc = pendiente;
                lote.setStockActual(disp.subtract(desc));
                lote.setEstado(calcularEstadoLote(lote.getFechaVencimiento(), lote.getStockActual()));
                pendiente = BigDecimal.ZERO;
            }
            loteRepository.save(lote);

            kardexRepository.save(MovimientoKardex.builder()
                    .tipo(TipoMovimientoKardex.SALIDA)
                    .lote(lote)
                    .productoId(insumoId)
                    .productoNombre(etiqueta)
                    .cantidad(desc)
                    .saldoStockResultante(lote.getStockActual())
                    .motivo("CONSUMO_INSUMO_BOM")
                    .documentoReferencia(codigoOrden)
                    .fechaHora(LocalDateTime.now())
                    .build());
        }

        if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
            kardexRepository.save(MovimientoKardex.builder()
                    .tipo(TipoMovimientoKardex.SALIDA)
                    .lote(null)
                    .productoId(insumoId)
                    .productoNombre(etiqueta)
                    .cantidad(pendiente)
                    .saldoStockResultante(BigDecimal.ZERO)
                    .motivo("CONSUMO_INSUMO_BOM")
                    .documentoReferencia(codigoOrden)
                    .fechaHora(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    public LoteDto registrarIngresoCompraInsumo(
            Long insumoId,
            String nombreInsumo,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            String numeroComprobante) {

        String nombreSeguro = truncarNombre(nombreInsumo != null ? nombreInsumo : ("Insumo #" + insumoId));
        LocalDate hoy = LocalDate.now();
        LocalDate vencimiento = hoy.plusMonths(12);

        String baseComprobante = (numeroComprobante != null ? numeroComprobante.replaceAll("[^A-Za-z0-9]", "") : "COMP");
        String codigoLote = ("LOT-INS-" + baseComprobante + "-" + insumoId + "-" + (int) (Math.random() * 900 + 100));
        if (codigoLote.length() > 48) {
            codigoLote = codigoLote.substring(0, 48);
        }

        Lote lote = Lote.builder()
                .codigoLote(codigoLote.toUpperCase())
                .productoId(insumoId)
                .productoNombre(nombreSeguro)
                .fechaFabricacion(hoy)
                .fechaVencimiento(vencimiento)
                .stockInicial(cantidad)
                .stockActual(cantidad)
                .costoUnitario(costoUnitario != null ? costoUnitario : BigDecimal.ZERO)
                .estado(EstadoLote.DISPONIBLE)
                .observaciones("Ingreso por Orden de Compra " + numeroComprobante)
                .build();

        Lote guardado = loteRepository.save(lote);

        kardexRepository.save(MovimientoKardex.builder()
                .tipo(TipoMovimientoKardex.ENTRADA)
                .lote(guardado)
                .productoId(insumoId)
                .productoNombre(nombreSeguro)
                .cantidad(cantidad)
                .saldoStockResultante(cantidad)
                .motivo("COMPRA_INSUMO_PROVEEDOR")
                .documentoReferencia(numeroComprobante)
                .fechaHora(LocalDateTime.now())
                .build());

        log.info("Ingreso en almacén y Kardex registrado para insumo '{}' ({} unidades) desde comprobante {}",
                nombreSeguro, cantidad, numeroComprobante);
        return mapToDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteDto> listarLotes(Long productoId) {
        List<Lote> list = (productoId != null)
                ? loteRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId)
                : loteRepository.findAllByOrderByFechaVencimientoAsc();

        return list.stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteDto> listarLotesProximosAVencer(int diasUmbral) {
        LocalDate limite = LocalDate.now().plusDays(diasUmbral);
        return loteRepository.findAllByOrderByFechaVencimientoAsc().stream()
                .filter(l -> l.getStockActual().compareTo(BigDecimal.ZERO) > 0)
                .filter(l -> !l.getFechaVencimiento().isAfter(limite))
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoKardexDto> listarKardex(Long productoId) {
        List<MovimientoKardex> list = (productoId != null)
                ? kardexRepository.findByProductoIdOrderByFechaHoraDesc(productoId)
                : kardexRepository.findTop100ByOrderByFechaHoraDesc();

        return list.stream().map(k -> MovimientoKardexDto.builder()
                .id(k.getId())
                .tipo(k.getTipo())
                .loteId(k.getLote() != null ? k.getLote().getId() : null)
                .codigoLote(k.getLote() != null ? k.getLote().getCodigoLote() : "SIN_LOTE")
                .productoId(k.getProductoId())
                .productoNombre(k.getProductoNombre())
                .cantidad(k.getCantidad())
                .saldoStockResultante(k.getSaldoStockResultante())
                .motivo(k.getMotivo())
                .documentoReferencia(k.getDocumentoReferencia())
                .fechaHora(k.getFechaHora())
                .build()
        ).toList();
    }

    @Override
    public void evaluarEstadosVencimiento() {
        List<Lote> lotes = loteRepository.findAll();
        for (Lote l : lotes) {
            EstadoLote nuevoEstado = calcularEstadoLote(l.getFechaVencimiento(), l.getStockActual());
            if (l.getEstado() != nuevoEstado) {
                l.setEstado(nuevoEstado);
                loteRepository.save(l);
            }
        }
    }

    private String truncarNombre(String texto) {
        if (texto == null) {
            return "Item Inventario";
        }
        return texto.length() > 145 ? texto.substring(0, 145) : texto;
    }

    private EstadoLote calcularEstadoLote(LocalDate fechaVencimiento, BigDecimal stockActual) {
        if (stockActual == null || stockActual.compareTo(BigDecimal.ZERO) <= 0) {
            return EstadoLote.AGOTADO;
        }
        LocalDate hoy = LocalDate.now();
        if (fechaVencimiento.isBefore(hoy)) {
            return EstadoLote.VENCIDO;
        }
        if (fechaVencimiento.isBefore(hoy.plusDays(30))) {
            return EstadoLote.PROXIMO_A_VENCER;
        }
        return EstadoLote.DISPONIBLE;
    }

    private LoteDto mapToDto(Lote l) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), l.getFechaVencimiento());
        return LoteDto.builder()
                .id(l.getId())
                .codigoLote(l.getCodigoLote())
                .productoId(l.getProductoId())
                .productoNombre(l.getProductoNombre())
                .fechaFabricacion(l.getFechaFabricacion())
                .fechaVencimiento(l.getFechaVencimiento())
                .stockInicial(l.getStockInicial())
                .stockActual(l.getStockActual())
                .costoUnitario(l.getCostoUnitario())
                .estado(l.getEstado())
                .observaciones(l.getObservaciones())
                .diasParaVencer(dias)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public pe.edu.upeu.bomerp.inventario.dto.ReporteSemaforoInventarioResponse obtenerReporteSemaforoInventario() {
        List<Lote> todos = loteRepository.findAllByOrderByFechaVencimientoAsc();
        LocalDate hoy = LocalDate.now();

        BigDecimal valorizacionTotal = BigDecimal.ZERO;
        long criticos = 0;
        long preventivos = 0;
        long optimos = 0;
        long vencidos = 0;

        List<pe.edu.upeu.bomerp.inventario.dto.LoteSemaforoDto> dtos = new ArrayList<>();

        for (Lote l : todos) {
            BigDecimal stock = l.getStockActual() != null ? l.getStockActual() : BigDecimal.ZERO;
            BigDecimal costo = l.getCostoUnitario() != null ? l.getCostoUnitario() : BigDecimal.ZERO;
            BigDecimal valLote = stock.multiply(costo);
            valorizacionTotal = valorizacionTotal.add(valLote);

            long dias = ChronoUnit.DAYS.between(hoy, l.getFechaVencimiento());
            String semaforo;
            if (dias < 0) {
                semaforo = "VENCIDO";
                vencidos++;
            } else if (dias <= 30) {
                semaforo = "CRITICO";
                criticos++;
            } else if (dias <= 60) {
                semaforo = "PREVENTIVO";
                preventivos++;
            } else {
                semaforo = "OPTIMO";
                optimos++;
            }

            dtos.add(pe.edu.upeu.bomerp.inventario.dto.LoteSemaforoDto.builder()
                    .loteId(l.getId())
                    .codigoLote(l.getCodigoLote())
                    .productoId(l.getProductoId())
                    .productoNombre(l.getProductoNombre())
                    .fechaFabricacion(l.getFechaFabricacion())
                    .fechaVencimiento(l.getFechaVencimiento())
                    .diasRestantes(dias)
                    .estadoSemaforo(semaforo)
                    .stockActual(stock)
                    .costoUnitario(costo)
                    .valorizacionTotal(valLote)
                    .build());
        }

        return pe.edu.upeu.bomerp.inventario.dto.ReporteSemaforoInventarioResponse.builder()
                .valorizacionTotalInventario(valorizacionTotal)
                .totalLotes((long) dtos.size())
                .lotesCriticos(criticos)
                .lotesPreventivos(preventivos)
                .lotesOptimos(optimos)
                .lotesVencidos(vencidos)
                .lotes(dtos)
                .build();
    }
}
