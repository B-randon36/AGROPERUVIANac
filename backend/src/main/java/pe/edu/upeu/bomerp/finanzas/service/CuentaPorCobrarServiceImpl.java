package pe.edu.upeu.bomerp.finanzas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.finanzas.dto.AbonoResponse;
import pe.edu.upeu.bomerp.finanzas.dto.CrearCreditoRequest;
import pe.edu.upeu.bomerp.finanzas.dto.CuentaPorCobrarDetalleResponse;
import pe.edu.upeu.bomerp.finanzas.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.bomerp.finanzas.dto.MovimientoCajaRequest;
import pe.edu.upeu.bomerp.finanzas.dto.RegistrarAbonoRequest;
import pe.edu.upeu.bomerp.finanzas.dto.SesionCajaResponse;
import pe.edu.upeu.bomerp.finanzas.entity.AbonoCuentaPorCobrar;
import pe.edu.upeu.bomerp.finanzas.entity.CategoriaFinanciera;
import pe.edu.upeu.bomerp.finanzas.entity.CuentaPorCobrar;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoCredito;
import pe.edu.upeu.bomerp.finanzas.entity.TipoFinanciero;
import pe.edu.upeu.bomerp.finanzas.repository.AbonoCuentaPorCobrarRepository;
import pe.edu.upeu.bomerp.finanzas.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.bomerp.finanzas.repository.CuentaPorCobrarRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaPorCobrarServiceImpl implements CuentaPorCobrarService {

    private final CuentaPorCobrarRepository cuentaRepository;
    private final AbonoCuentaPorCobrarRepository abonoRepository;
    private final SesionCajaService sesionCajaService;
    private final CategoriaFinancieraRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorCobrarResponse> listarTodos(EstadoCredito estado) {
        List<CuentaPorCobrar> lista = (estado != null)
                ? cuentaRepository.findByEstadoOrderByFechaEmisionDesc(estado)
                : cuentaRepository.findAllByOrderByFechaEmisionDesc();

        return lista.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorCobrarDetalleResponse obtenerPorId(Long id) {
        CuentaPorCobrar cuenta = buscarCuentaOFallar(id);
        return mapToDetalleResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaPorCobrarResponse registrarCredito(CrearCreditoRequest req) {
        // [RN-FIN-08] Cuentas por Cobrar a Crédito Comercial:
        // Registra la obligación de pago para ventas mayoristas. Define plazo (por defecto 30 días, hasta 60 días),
        // inicializa el saldo pendiente al 100% del monto total y asigna estado inicial PENDIENTE.
        String codigo = req.getCodigo();
        if (codigo == null || codigo.isBlank()) {
            long total = cuentaRepository.count() + 1;
            codigo = String.format("CRE-%03d", total);
            if (cuentaRepository.existsByCodigo(codigo)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"));
                codigo = "CRE-" + timestamp;
            }
        } else if (cuentaRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe una cuenta por cobrar con el código: " + codigo);
        }

        LocalDate fechaEmision = req.getFechaEmision() != null ? req.getFechaEmision() : LocalDate.now();
        LocalDate fechaVencimiento = req.getFechaVencimiento() != null
                ? req.getFechaVencimiento()
                : fechaEmision.plusDays(30);

        if (fechaVencimiento.isBefore(fechaEmision)) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de emisión.");
        }

        CuentaPorCobrar cuenta = CuentaPorCobrar.builder()
                .codigo(codigo)
                .clienteNombre(req.getClienteNombre().trim())
                .clienteDocumento(req.getClienteDocumento() != null ? req.getClienteDocumento().trim() : null)
                .montoTotal(req.getMontoTotal())
                .saldoPendiente(req.getMontoTotal())
                .fechaEmision(fechaEmision)
                .fechaVencimiento(fechaVencimiento)
                .estado(EstadoCredito.PENDIENTE)
                .build();

        CuentaPorCobrar guardada = cuentaRepository.save(cuenta);
        log.info("Crédito registrado exitosamente: {} para cliente {}", guardada.getCodigo(), guardada.getClienteNombre());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public CuentaPorCobrarDetalleResponse registrarAbono(Long creditoId, RegistrarAbonoRequest req) {
        // [RN-FIN-08] Amortización y Liquidación de Crédito:
        // 1. Valida que el monto no exceda el saldo pendiente ni sea menor o igual a cero.
        // 2. Transiciona a PAGADO_PARCIAL o PAGADO_TOTAL según el remanente.
        // 3. Impacta automáticamente como INGRESO en la sesión de caja activa del cajero.
        CuentaPorCobrar cuenta = buscarCuentaOFallar(creditoId);

        if (cuenta.getEstado() == EstadoCredito.PAGADO_TOTAL) {
            throw new IllegalStateException("El crédito " + cuenta.getCodigo() + " ya se encuentra totalmente PAGADO.");
        }

        if (req.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a abonar debe ser mayor a cero.");
        }

        if (req.getMonto().compareTo(cuenta.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "El monto a abonar (S/ %.2f) supera el saldo pendiente de la cuenta (S/ %.2f).",
                    req.getMonto(), cuenta.getSaldoPendiente()
            ));
        }

        // 1. Amortización de deuda
        BigDecimal nuevoSaldo = cuenta.getSaldoPendiente().subtract(req.getMonto());
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0) {
            cuenta.setSaldoPendiente(BigDecimal.ZERO);
            cuenta.setEstado(EstadoCredito.PAGADO_TOTAL);
        } else {
            cuenta.setSaldoPendiente(nuevoSaldo);
            cuenta.setEstado(EstadoCredito.PAGADO_PARCIAL);
        }

        // 2. Impacto en Turno / Sesión de Caja Activa
        Long sesionId = req.getSesionCajaId();
        if (sesionId == null) {
            sesionId = sesionCajaService.buscarUltimaSesionActiva()
                    .map(SesionCajaResponse::getId)
                    .orElse(null);
        }

        if (sesionId != null) {
            try {
                CategoriaFinanciera catCobro = categoriaRepository.findByNombre("Cobros de Créditos y Fiados")
                        .or(() -> categoriaRepository.findByTipoDefault(TipoFinanciero.INGRESO).stream().findFirst())
                        .orElseGet(() -> {
                            CategoriaFinanciera nueva = new CategoriaFinanciera();
                            nueva.setNombre("Cobros de Créditos y Fiados");
                            nueva.setDescripcion("Ingresos por amortización y cobro de cuentas por cobrar");
                            nueva.setTipoDefault(TipoFinanciero.INGRESO);
                            return categoriaRepository.save(nueva);
                        });

                MovimientoCajaRequest movReq = MovimientoCajaRequest.builder()
                        .tipo(TipoFinanciero.INGRESO)
                        .metodoPago(req.getMetodoPago())
                        .monto(req.getMonto())
                        .descripcion(String.format("Cobro crédito %s - %s", cuenta.getCodigo(), cuenta.getClienteNombre()))
                        .referenciaExterna(cuenta.getCodigo())
                        .categoriaFinancieraId(catCobro.getId())
                        .build();

                sesionCajaService.registrarMovimiento(sesionId, movReq);
                log.info("Abono de S/ {} para crédito {} impactó la sesión de caja ID {}", req.getMonto(), cuenta.getCodigo(), sesionId);
            } catch (Exception e) {
                log.warn("No se pudo registrar el movimiento en caja para la sesión {}: {}", sesionId, e.getMessage());
            }
        }

        // 3. Crear registro de abono
        AbonoCuentaPorCobrar abono = AbonoCuentaPorCobrar.builder()
                .cuentaPorCobrar(cuenta)
                .sesionCajaId(sesionId)
                .monto(req.getMonto())
                .metodoPago(req.getMetodoPago())
                .fecha(LocalDateTime.now())
                .nota(req.getNota() != null && !req.getNota().isBlank()
                        ? req.getNota().trim()
                        : "Amortización de crédito " + cuenta.getCodigo())
                .build();

        cuenta.agregarAbono(abono);
        CuentaPorCobrar guardada = cuentaRepository.save(cuenta);

        log.info("Abono de S/ {} registrado con éxito en crédito {}. Saldo restante: S/ {}",
                req.getMonto(), guardada.getCodigo(), guardada.getSaldoPendiente());

        return mapToDetalleResponse(guardada);
    }

    private CuentaPorCobrar buscarCuentaOFallar(Long id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada con ID: " + id));
    }

    private CuentaPorCobrarResponse mapToResponse(CuentaPorCobrar c) {
        BigDecimal montoTotal = c.getMontoTotal() != null ? c.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal saldoPendiente = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal montoPagado = montoTotal.subtract(saldoPendiente);
        if (montoPagado.compareTo(BigDecimal.ZERO) < 0) {
            montoPagado = BigDecimal.ZERO;
        }

        double pct = 0.0;
        if (montoTotal.compareTo(BigDecimal.ZERO) > 0) {
            pct = montoPagado.multiply(BigDecimal.valueOf(100))
                    .divide(montoTotal, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        int abonosCount = c.getAbonos() != null ? c.getAbonos().size() : 0;

        return CuentaPorCobrarResponse.builder()
                .id(c.getId())
                .codigo(c.getCodigo())
                .clienteNombre(c.getClienteNombre())
                .clienteDocumento(c.getClienteDocumento())
                .montoTotal(montoTotal)
                .saldoPendiente(saldoPendiente)
                .montoPagado(montoPagado)
                .porcentajePagado(pct)
                .fechaEmision(c.getFechaEmision())
                .fechaVencimiento(c.getFechaVencimiento())
                .estado(c.getEstado())
                .cantidadAbonos(abonosCount)
                .build();
    }

    private CuentaPorCobrarDetalleResponse mapToDetalleResponse(CuentaPorCobrar c) {
        CuentaPorCobrarResponse base = mapToResponse(c);

        List<AbonoResponse> abonosDto = (c.getAbonos() != null)
                ? c.getAbonos().stream().map(a -> AbonoResponse.builder()
                        .id(a.getId())
                        .cuentaPorCobrarId(c.getId())
                        .sesionCajaId(a.getSesionCajaId())
                        .monto(a.getMonto())
                        .metodoPago(a.getMetodoPago())
                        .fecha(a.getFecha())
                        .nota(a.getNota())
                        .build()).toList()
                : List.of();

        return CuentaPorCobrarDetalleResponse.builder()
                .id(base.getId())
                .codigo(base.getCodigo())
                .clienteNombre(base.getClienteNombre())
                .clienteDocumento(base.getClienteDocumento())
                .montoTotal(base.getMontoTotal())
                .saldoPendiente(base.getSaldoPendiente())
                .montoPagado(base.getMontoPagado())
                .porcentajePagado(base.getPorcentajePagado())
                .fechaEmision(base.getFechaEmision())
                .fechaVencimiento(base.getFechaVencimiento())
                .estado(base.getEstado())
                .abonos(abonosDto)
                .build();
    }
}
