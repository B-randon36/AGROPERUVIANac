package pe.edu.upeu.bomerp.finanzas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.exception.SolvenciaInsuficienteException;
import pe.edu.upeu.bomerp.finanzas.dto.AperturaSesionRequest;
import pe.edu.upeu.bomerp.finanzas.dto.CierreCajaReporteDto;
import pe.edu.upeu.bomerp.finanzas.dto.CierreSesionRequest;
import pe.edu.upeu.bomerp.finanzas.dto.MetodoPagoResumenDto;
import pe.edu.upeu.bomerp.finanzas.dto.MovimientoCajaRequest;
import pe.edu.upeu.bomerp.finanzas.dto.MovimientoCajaResponse;
import pe.edu.upeu.bomerp.finanzas.dto.SesionCajaDetalleResponse;
import pe.edu.upeu.bomerp.finanzas.dto.SesionCajaResponse;
import pe.edu.upeu.bomerp.finanzas.entity.CategoriaFinanciera;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoCaja;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.finanzas.entity.MovimientoCaja;
import pe.edu.upeu.bomerp.finanzas.entity.SesionCaja;
import pe.edu.upeu.bomerp.finanzas.entity.TipoFinanciero;
import pe.edu.upeu.bomerp.finanzas.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.bomerp.finanzas.repository.MovimientoCajaRepository;
import pe.edu.upeu.bomerp.finanzas.repository.SesionCajaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SesionCajaServiceImpl implements SesionCajaService {

    private final SesionCajaRepository sesionRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final CategoriaFinancieraRepository categoriaRepository;
    private final pe.edu.upeu.bomerp.finanzas.repository.DenominacionMonedaRepository denominacionRepository;
    private final pe.edu.upeu.bomerp.finanzas.repository.DetalleArqueoEfectivoRepository detalleArqueoRepository;

    public SesionCajaServiceImpl(
            SesionCajaRepository sesionRepository,
            MovimientoCajaRepository movimientoRepository,
            CategoriaFinancieraRepository categoriaRepository,
            pe.edu.upeu.bomerp.finanzas.repository.DenominacionMonedaRepository denominacionRepository,
            pe.edu.upeu.bomerp.finanzas.repository.DetalleArqueoEfectivoRepository detalleArqueoRepository) {
        this.sesionRepository = sesionRepository;
        this.movimientoRepository = movimientoRepository;
        this.categoriaRepository = categoriaRepository;
        this.denominacionRepository = denominacionRepository;
        this.detalleArqueoRepository = detalleArqueoRepository;
    }

    @Override
    @Transactional
    public SesionCajaResponse abrirSesion(AperturaSesionRequest request) {
        // [RN-FIN-01] / [RN-FIN-02] Unicidad de Turno Activo por Usuario:
        // Cada cajero/vendedor solo puede operar un turno de caja simultáneamente en estado ABIERTA.
        if (sesionRepository.existsByUsuarioIdAndEstado(request.getUsuarioId(), EstadoCaja.ABIERTA)) {
            throw new IllegalStateException("RN-FIN-02: El usuario ya posee un turno de caja abierto. Debe cerrarlo antes de iniciar otro.");
        }

        String codigo = request.getCodigoSesion();
        if (codigo == null || codigo.isBlank()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            codigo = "SES-" + timestamp;
        }

        if (sesionRepository.existsByCodigoSesion(codigo)) {
            throw new IllegalArgumentException("Ya existe una sesión con el código: " + codigo);
        }

        String rol = request.getCajeroRol();
        if (rol == null || rol.isBlank()) {
            rol = "CAJERO";
        }

        SesionCaja sesion = SesionCaja.builder()
                .codigoSesion(codigo)
                .usuarioId(request.getUsuarioId())
                .cajero(request.getCajero())
                .cajeroRol(rol)
                .fechaApertura(LocalDateTime.now())
                .saldoInicial(request.getSaldoInicial())
                .totalIngresosEfectivo(BigDecimal.ZERO)
                .totalEgresosEfectivo(BigDecimal.ZERO)
                .saldoEsperadoEfectivo(request.getSaldoInicial())
                .saldoRealEfectivo(null)
                .diferencia(BigDecimal.ZERO)
                .estado(EstadoCaja.ABIERTA)
                .build();

        SesionCaja guardada = sesionRepository.save(sesion);
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public SesionCajaResponse cerrarSesion(Long id, CierreSesionRequest request) {
        SesionCaja sesion = buscarSesionOFallar(id);

        // [RN-FIN-02] Inmutabilidad del Turno Cerrado:
        // Un turno sellado queda protegido contra modificaciones; no puede volver a cerrarse ni recibir nuevos cobros
        if (sesion.getEstado() != EstadoCaja.ABIERTA) {
            throw new IllegalStateException("RN-FIN-02: La sesión " + sesion.getCodigoSesion() + " ya se encuentra CERRADA.");
        }

        BigDecimal saldoReal = request.getSaldoRealEfectivo();

        // [RN-FIN-03] Arqueo Físico Obligatorio con Desglose de Billetes y Monedas:
        // El servidor recalcula el total a partir de cada denominación (fuente de verdad)
        // y calcula automáticamente el descuadre (Diferencia = Saldo Real - Saldo Teórico Esperado)
        if (request.getDesglose() != null && !request.getDesglose().isEmpty()) {
            BigDecimal totalDesglose = BigDecimal.ZERO;
            sesion.getDetallesArqueo().clear();
            for (pe.edu.upeu.bomerp.finanzas.dto.DesgloseEfectivoItemDto item : request.getDesglose()) {
                if (item.getCantidad() != null && item.getCantidad() > 0) {
                    pe.edu.upeu.bomerp.finanzas.entity.DenominacionMoneda denom = denominacionRepository.findById(item.getDenominacionId())
                            .orElseThrow(() -> new ResourceNotFoundException("Denominación no encontrada con id: " + item.getDenominacionId()));
                    BigDecimal subtotal = denom.getValorFacial().multiply(BigDecimal.valueOf(item.getCantidad()));
                    totalDesglose = totalDesglose.add(subtotal);

                    pe.edu.upeu.bomerp.finanzas.entity.DetalleArqueoEfectivo detalle = pe.edu.upeu.bomerp.finanzas.entity.DetalleArqueoEfectivo.builder()
                            .id(new pe.edu.upeu.bomerp.finanzas.entity.DetalleArqueoEfectivoId(sesion.getId(), denom.getId()))
                            .sesionCaja(sesion)
                            .denominacionMoneda(denom)
                            .cantidad(item.getCantidad())
                            .subtotal(subtotal)
                            .build();
                    sesion.agregarDetalleArqueo(detalle);
                }
            }
            saldoReal = totalDesglose;
        }

        sesion.cerrarSesion(saldoReal, request.getObservaciones());
        SesionCaja guardada = sesionRepository.save(sesion);
        return mapToResponse(guardada);
    }

    /**
     * ========================================================================
     * REGISTRO TRANSACCIONAL DE MOVIMIENTO
     * ========================================================================
     * 1. [RN-FIN-02] Valida que la sesión esté ABIERTA; prohíbe operaciones en turnos sellados.
     * 2. [RN-FIN-04] Control de Solvencia y Liquidez en Gaveta de Efectivo.
     *    Si el movimiento es un EGRESO en EFECTIVO y supera el saldoEsperadoEfectivo,
     *    se lanza SolvenciaInsuficienteException, provocando rollback @Transactional inmediato.
     * 3. Actualiza atómicamente los totales de ingresos/egresos y saldo en gaveta.
     * ========================================================================
     */
    @Override
    @Transactional
    public MovimientoCajaResponse registrarMovimiento(Long sesionId, MovimientoCajaRequest request) {
        SesionCaja sesion = buscarSesionOFallar(sesionId);

        if (sesion.getEstado() != EstadoCaja.ABIERTA) {
            throw new IllegalStateException("RN-FIN-02: La sesión " + sesion.getCodigoSesion() + " no está ABIERTA. No se pueden registrar movimientos.");
        }

        // Validación de la Categoría Financiera
        CategoriaFinanciera categoria = categoriaRepository.findById(request.getCategoriaFinancieraId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría financiera no encontrada con id: " + request.getCategoriaFinancieraId()));

        // ====================================================================
        // [RN-FIN-04] Solvencia de Gaveta en Egresos de Efectivo
        // ====================================================================
        if (request.getTipo() == TipoFinanciero.EGRESO && request.getMetodoPago() == MetodoPago.EFECTIVO) {
            BigDecimal saldoDisponible = sesion.getSaldoEsperadoEfectivo() != null ? sesion.getSaldoEsperadoEfectivo() : BigDecimal.ZERO;
            if (request.getMonto().compareTo(saldoDisponible) > 0) {
                throw new SolvenciaInsuficienteException(
                        "RN-FIN-01: El monto del egreso en efectivo (S/ " + request.getMonto() +
                        ") excede el saldo en efectivo disponible en gaveta (S/ " + saldoDisponible + ")"
                );
            }
        }

        MovimientoCaja movimiento = MovimientoCaja.builder()
                .fechaHora(LocalDateTime.now())
                .tipo(request.getTipo())
                .metodoPago(request.getMetodoPago())
                .monto(request.getMonto())
                .descripcion(request.getDescripcion())
                .referenciaExterna(request.getReferenciaExterna())
                .categoriaFinanciera(categoria)
                .sesionCaja(sesion)
                .build();

        sesion.agregarMovimiento(movimiento);
        sesionRepository.save(sesion);

        MovimientoCaja movGuardado = movimientoRepository.save(movimiento);
        return mapToMovimientoResponse(movGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionCajaResponse> listarSesiones() {
        return sesionRepository.findAllByOrderByFechaAperturaDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaDetalleResponse obtenerDetalle(Long id) {
        SesionCaja sesion = buscarSesionOFallar(id);
        List<MovimientoCajaResponse> movs = sesion.getMovimientos().stream()
                .map(this::mapToMovimientoResponse)
                .toList();

        List<pe.edu.upeu.bomerp.finanzas.dto.DesgloseEfectivoItemDto> arqueo = sesion.getDetallesArqueo() != null ?
                sesion.getDetallesArqueo().stream()
                        .map(d -> pe.edu.upeu.bomerp.finanzas.dto.DesgloseEfectivoItemDto.builder()
                                .denominacionId(d.getDenominacionMoneda().getId())
                                .valorFacial(d.getDenominacionMoneda().getValorFacial())
                                .tipo(d.getDenominacionMoneda().getTipo())
                                .cantidad(d.getCantidad())
                                .subtotal(d.getSubtotal())
                                .build())
                        .toList() : java.util.Collections.emptyList();

        return SesionCajaDetalleResponse.builder()
                .id(sesion.getId())
                .codigoSesion(sesion.getCodigoSesion())
                .usuarioId(sesion.getUsuarioId())
                .cajero(sesion.getCajero())
                .cajeroRol(sesion.getCajeroRol() != null ? sesion.getCajeroRol() : "CAJERO")
                .fechaApertura(sesion.getFechaApertura())
                .fechaCierre(sesion.getFechaCierre())
                .saldoInicial(sesion.getSaldoInicial())
                .totalIngresosEfectivo(sesion.getTotalIngresosEfectivo())
                .totalEgresosEfectivo(sesion.getTotalEgresosEfectivo())
                .saldoEsperadoEfectivo(sesion.getSaldoEsperadoEfectivo())
                .saldoRealEfectivo(sesion.getSaldoRealEfectivo())
                .diferencia(sesion.getDiferencia())
                .estado(sesion.getEstado())
                .observaciones(sesion.getObservaciones())
                .movimientos(movs)
                .detallesArqueo(arqueo)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaResponse obtenerSesionActivaUsuario(Long usuarioId) {
        SesionCaja sesion = sesionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException("No existe sesión abierta para el usuario id: " + usuarioId));
        return mapToResponse(sesion);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SesionCajaResponse> buscarUltimaSesionActiva() {
        return sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaResponse obtenerUltimaSesionActiva() {
        return buscarUltimaSesionActiva()
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna sesión de caja abierta actualmente."));
    }

    @Override
    @Transactional(readOnly = true)
    public CierreCajaReporteDto generarDatosReporte(Long id) {
        // [RN-FIN-05] Conciliación Multicanal de Métodos de Pago:
        // Agrupa de forma separada los totales de ingresos, egresos y neto para cada medio
        // (Efectivo, Yape, Plin, Tarjeta, Transferencia) permitiendo cruces contra estados bancarios y apps POS.
        SesionCaja sesion = buscarSesionOFallar(id);
        List<MovimientoCajaResponse> movs = sesion.getMovimientos().stream()
                .map(this::mapToMovimientoResponse)
                .toList();

        Map<MetodoPago, BigDecimal[]> acumuladores = new EnumMap<>(MetodoPago.class);
        for (MetodoPago m : MetodoPago.values()) {
            acumuladores.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (MovimientoCaja mov : sesion.getMovimientos()) {
            MetodoPago mp = mov.getMetodoPago();
            BigDecimal[] par = acumuladores.get(mp);
            if (par != null && mov.getMonto() != null) {
                if (mov.getTipo() == TipoFinanciero.INGRESO) {
                    par[0] = par[0].add(mov.getMonto());
                } else if (mov.getTipo() == TipoFinanciero.EGRESO) {
                    par[1] = par[1].add(mov.getMonto());
                }
            }
        }

        List<MetodoPagoResumenDto> resumenMetodos = new ArrayList<>();
        for (Map.Entry<MetodoPago, BigDecimal[]> entry : acumuladores.entrySet()) {
            BigDecimal ing = entry.getValue()[0];
            BigDecimal egr = entry.getValue()[1];
            resumenMetodos.add(MetodoPagoResumenDto.builder()
                    .metodoPago(entry.getKey())
                    .totalIngresos(ing)
                    .totalEgresos(egr)
                    .neto(ing.subtract(egr))
                    .build());
        }

        return CierreCajaReporteDto.builder()
                .fechaGeneracion(LocalDateTime.now())
                .sesion(mapToResponse(sesion))
                .movimientos(movs)
                .resumenPorMetodo(resumenMetodos)
                .build();
    }

    private SesionCaja buscarSesionOFallar(Long id) {
        return sesionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja no encontrada con id: " + id));
    }

    private SesionCajaResponse mapToResponse(SesionCaja s) {
        int count = s.getMovimientos() != null ? s.getMovimientos().size() : 0;
        return SesionCajaResponse.builder()
                .id(s.getId())
                .codigoSesion(s.getCodigoSesion())
                .usuarioId(s.getUsuarioId())
                .cajero(s.getCajero())
                .cajeroRol(s.getCajeroRol() != null ? s.getCajeroRol() : "CAJERO")
                .fechaApertura(s.getFechaApertura())
                .fechaCierre(s.getFechaCierre())
                .saldoInicial(s.getSaldoInicial())
                .totalIngresosEfectivo(s.getTotalIngresosEfectivo())
                .totalEgresosEfectivo(s.getTotalEgresosEfectivo())
                .saldoEsperadoEfectivo(s.getSaldoEsperadoEfectivo())
                .saldoRealEfectivo(s.getSaldoRealEfectivo())
                .diferencia(s.getDiferencia())
                .estado(s.getEstado())
                .observaciones(s.getObservaciones())
                .cantidadMovimientos(count)
                .build();
    }

    private MovimientoCajaResponse mapToMovimientoResponse(MovimientoCaja m) {
        Long catId = m.getCategoriaFinanciera() != null ? m.getCategoriaFinanciera().getId() : null;
        String catNombre = m.getCategoriaFinanciera() != null ? m.getCategoriaFinanciera().getNombre() : null;
        Long sesionId = m.getSesionCaja() != null ? m.getSesionCaja().getId() : null;

        return MovimientoCajaResponse.builder()
                .id(m.getId())
                .fechaHora(m.getFechaHora())
                .tipo(m.getTipo())
                .metodoPago(m.getMetodoPago())
                .monto(m.getMonto())
                .descripcion(m.getDescripcion())
                .referenciaExterna(m.getReferenciaExterna())
                .categoriaFinancieraId(catId)
                .categoriaFinancieraNombre(catNombre)
                .sesionCajaId(sesionId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<pe.edu.upeu.bomerp.finanzas.dto.DenominacionMonedaResponse> listarDenominaciones() {
        return denominacionRepository.findByActivoTrueOrderByValorFacialDesc().stream()
                .map(d -> pe.edu.upeu.bomerp.finanzas.dto.DenominacionMonedaResponse.builder()
                        .id(d.getId())
                        .valorFacial(d.getValorFacial())
                        .tipo(d.getTipo())
                        .orden(d.getOrden())
                        .activo(d.getActivo())
                        .build())
                .toList();
    }
}
