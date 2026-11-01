package pe.edu.upeu.bomerp.finanzas.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.finanzas.dto.CrearCreditoRequest;
import pe.edu.upeu.bomerp.finanzas.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.bomerp.finanzas.dto.MovimientoCajaRequest;
import pe.edu.upeu.bomerp.finanzas.dto.MovimientoCajaResponse;
import pe.edu.upeu.bomerp.finanzas.entity.CategoriaFinanciera;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoCaja;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoFinanciero;
import pe.edu.upeu.bomerp.finanzas.entity.MetodoPago;
import pe.edu.upeu.bomerp.finanzas.entity.SesionCaja;
import pe.edu.upeu.bomerp.finanzas.entity.TipoFinanciero;
import pe.edu.upeu.bomerp.finanzas.entity.TransaccionFinanciera;
import pe.edu.upeu.bomerp.finanzas.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.bomerp.finanzas.repository.CuentaPorCobrarRepository;
import pe.edu.upeu.bomerp.finanzas.repository.SesionCajaRepository;
import pe.edu.upeu.bomerp.finanzas.repository.TransaccionFinancieraRepository;
import pe.edu.upeu.bomerp.finanzas.service.CuentaPorCobrarService;
import pe.edu.upeu.bomerp.finanzas.service.SesionCajaService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
public class FinanzasApiImpl implements FinanzasApi {

    private final SesionCajaRepository sesionRepository;
    private final SesionCajaService sesionService;
    private final CategoriaFinancieraRepository categoriaRepository;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;

    public FinanzasApiImpl(
            SesionCajaRepository sesionRepository,
            SesionCajaService sesionService,
            CategoriaFinancieraRepository categoriaRepository,
            TransaccionFinancieraRepository transaccionRepository,
            CuentaPorCobrarService cuentaPorCobrarService,
            CuentaPorCobrarRepository cuentaPorCobrarRepository) {
        this.sesionRepository = sesionRepository;
        this.sesionService = sesionService;
        this.categoriaRepository = categoriaRepository;
        this.transaccionRepository = transaccionRepository;
        this.cuentaPorCobrarService = cuentaPorCobrarService;
        this.cuentaPorCobrarRepository = cuentaPorCobrarRepository;
    }

    @Override
    @Transactional
    public Long registrarCobroVenta(Long usuarioId, BigDecimal monto, MetodoPago metodoPago, String referenciaVenta, Long categoriaId) {
        SesionCaja sesion = sesionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new IllegalStateException(
                        "No se puede registrar el cobro de la venta " + referenciaVenta +
                                " porque el usuario " + usuarioId + " no tiene un turno de caja ABIERTO."
                ));

        Long catId = categoriaId != null
                ? categoriaId
                : obtenerOCrearCategoria(TipoFinanciero.INGRESO, "Ventas POS y Comercialización").getId();

        MovimientoCajaRequest movReq = MovimientoCajaRequest.builder()
                .tipo(TipoFinanciero.INGRESO)
                .metodoPago(metodoPago)
                .monto(monto)
                .descripcion("Cobro de Venta " + (referenciaVenta != null ? referenciaVenta : ""))
                .referenciaExterna(referenciaVenta)
                .categoriaFinancieraId(catId)
                .build();

        MovimientoCajaResponse resp = sesionService.registrarMovimiento(sesion.getId(), movReq);
        return resp.getId();
    }

    @Override
    @Transactional
    public Long registrarIngresoVentaPos(BigDecimal monto, MetodoPago metodoPago, String numeroTicket, String clienteNombre) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        CategoriaFinanciera catIngreso = obtenerOCrearCategoria(
                TipoFinanciero.INGRESO,
                "Ventas POS y Comercialización"
        );

        Long movimientoId = null;
        SesionCaja sesionActiva = sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA).orElse(null);
        if (sesionActiva != null) {
            MovimientoCajaRequest movReq = MovimientoCajaRequest.builder()
                    .tipo(TipoFinanciero.INGRESO)
                    .metodoPago(metodoPago != null ? metodoPago : MetodoPago.EFECTIVO)
                    .monto(monto)
                    .descripcion(truncar("Venta POS " + numeroTicket + " · " + (clienteNombre != null ? clienteNombre : "Cliente General"), 200))
                    .referenciaExterna(numeroTicket)
                    .categoriaFinancieraId(catIngreso.getId())
                    .build();
            MovimientoCajaResponse resp = sesionService.registrarMovimiento(sesionActiva.getId(), movReq);
            movimientoId = resp.getId();
        }

        TransaccionFinanciera trx = new TransaccionFinanciera();
        trx.setCodigo(generarCodigoUnico("TRX-VTA", numeroTicket));
        trx.setConcepto(truncar("Venta POS " + numeroTicket + " - " + (clienteNombre != null ? clienteNombre : "Cliente General"), 195));
        trx.setMonto(monto);
        trx.setFechaTransaccion(LocalDate.now());
        trx.setTipo(TipoFinanciero.INGRESO);
        trx.setEstado(EstadoFinanciero.PAGADO);
        trx.setTipoComprobante("TICKET_POS");
        trx.setNumeroComprobante(numeroTicket);
        trx.setCategoriaFinanciera(catIngreso);
        transaccionRepository.save(trx);

        log.info("Ingreso financiero por Venta POS {} (S/ {}) asentado en Caja y Finanzas.", numeroTicket, monto);
        return movimientoId;
    }

    @Override
    @Transactional
    public Long registrarVentaACredito(
            String numeroTicket,
            String clienteNombre,
            String clienteDocumento,
            BigDecimal montoTotal,
            Integer diasCredito) {

        int plazoDias = (diasCredito != null && diasCredito > 0) ? diasCredito : 30;
        LocalDate hoy = LocalDate.now();
        String codigoCredito = truncar("CRE-" + numeroTicket, 48);

        CrearCreditoRequest creditoReq = CrearCreditoRequest.builder()
                .codigo(codigoCredito)
                .clienteNombre(clienteNombre != null && !clienteNombre.isBlank() ? clienteNombre.trim() : "Cliente Mayorista")
                .clienteDocumento(clienteDocumento)
                .montoTotal(montoTotal)
                .fechaEmision(hoy)
                .fechaVencimiento(hoy.plusDays(plazoDias))
                .build();

        CuentaPorCobrarResponse cuenta = cuentaPorCobrarService.registrarCredito(creditoReq);

        CategoriaFinanciera catIngreso = obtenerOCrearCategoria(
                TipoFinanciero.INGRESO,
                "Ventas al Crédito Mayorista"
        );

        TransaccionFinanciera trx = new TransaccionFinanciera();
        trx.setCodigo(generarCodigoUnico("TRX-CRE", numeroTicket));
        trx.setConcepto(truncar("Venta al Crédito " + numeroTicket + " - " + creditoReq.getClienteNombre(), 195));
        trx.setMonto(montoTotal);
        trx.setFechaTransaccion(hoy);
        trx.setTipo(TipoFinanciero.INGRESO);
        trx.setEstado(EstadoFinanciero.PENDIENTE);
        trx.setTipoComprobante("CREDITO_POS");
        trx.setNumeroComprobante(numeroTicket);
        trx.setCategoriaFinanciera(catIngreso);
        transaccionRepository.save(trx);

        log.info("Cuenta por Cobrar {} (S/ {}) generada automáticamente desde Venta POS {}",
                cuenta.getCodigo(), montoTotal, numeroTicket);
        return cuenta.getId();
    }

    @Override
    @Transactional
    public void revertirVentaPos(String numeroTicket, BigDecimal monto, MetodoPago metodoPago, boolean eraCredito) {
        if (numeroTicket == null) {
            return;
        }

        transaccionRepository.findFirstByNumeroComprobante(numeroTicket).ifPresent(trx -> {
            trx.setEstado(EstadoFinanciero.ANULADO);
            transaccionRepository.save(trx);
        });

        if (eraCredito) {
            String codigoCredito = truncar("CRE-" + numeroTicket, 48);
            cuentaPorCobrarRepository.findByCodigo(codigoCredito).ifPresent(cuentaPorCobrarRepository::delete);
            return;
        }

        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            SesionCaja sesionActiva = sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA).orElse(null);
            if (sesionActiva != null) {
                MetodoPago mp = metodoPago != null ? metodoPago : MetodoPago.EFECTIVO;
                // Verificar solvencia en efectivo antes de extornar para no romper anulación si el turno cambió
                boolean puedeExtornarCaja = mp != MetodoPago.EFECTIVO ||
                        (sesionActiva.getSaldoEsperadoEfectivo() != null
                                && sesionActiva.getSaldoEsperadoEfectivo().compareTo(monto) >= 0);

                if (puedeExtornarCaja) {
                    CategoriaFinanciera catEgreso = obtenerOCrearCategoria(
                            TipoFinanciero.EGRESO,
                            "Devoluciones y Anulaciones POS"
                    );
                    sesionService.registrarMovimiento(sesionActiva.getId(), MovimientoCajaRequest.builder()
                            .tipo(TipoFinanciero.EGRESO)
                            .metodoPago(mp)
                            .monto(monto)
                            .descripcion(truncar("Extorno por anulación de Venta " + numeroTicket, 200))
                            .referenciaExterna(numeroTicket)
                            .categoriaFinancieraId(catEgreso.getId())
                            .build());
                }
            }
        }
    }

    @Override
    @Transactional
    public void registrarFacturaCompraPendiente(
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoTotal,
            LocalDate fechaEmision) {

        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CategoriaFinanciera catCompra = obtenerOCrearCategoria(
                TipoFinanciero.EGRESO,
                "Insumos y Envases de Producción"
        );

        TransaccionFinanciera trx = new TransaccionFinanciera();
        trx.setCodigo(generarCodigoUnico("TRX-COM", numeroComprobante));
        trx.setConcepto(truncar("Factura de Compra " + numeroComprobante + " - " + razonSocialProveedor, 195));
        trx.setMonto(montoTotal);
        trx.setFechaTransaccion(fechaEmision != null ? fechaEmision : LocalDate.now());
        trx.setTipo(TipoFinanciero.EGRESO);
        trx.setEstado(EstadoFinanciero.PENDIENTE);
        trx.setTipoComprobante("FACTURA");
        trx.setNumeroComprobante(numeroComprobante);
        trx.setCategoriaFinanciera(catCompra);
        transaccionRepository.save(trx);
    }

    @Override
    @Transactional
    public void registrarPagoProveedorCompra(
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoPagado,
            MetodoPago metodoPago,
            boolean liquidadaTotalmente) {

        if (montoPagado == null || montoPagado.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CategoriaFinanciera catCompra = obtenerOCrearCategoria(
                TipoFinanciero.EGRESO,
                "Insumos y Envases de Producción"
        );

        // 1. Impactar Sesión de Caja Abierta (si el pago es EFECTIVO aplica RN-FIN-01 de solvencia en gaveta)
        SesionCaja sesionActiva = sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA).orElse(null);
        if (sesionActiva != null) {
            MetodoPago mp = metodoPago != null ? metodoPago : MetodoPago.TRANSFERENCIA;
            sesionService.registrarMovimiento(sesionActiva.getId(), MovimientoCajaRequest.builder()
                    .tipo(TipoFinanciero.EGRESO)
                    .metodoPago(mp)
                    .monto(montoPagado)
                    .descripcion(truncar("Pago proveedor " + razonSocialProveedor + " (" + numeroComprobante + ")", 200))
                    .referenciaExterna(numeroComprobante)
                    .categoriaFinancieraId(catCompra.getId())
                    .build());
        }

        // 2. Actualizar o registrar Transacción Financiera PAGADA
        TransaccionFinanciera existente = numeroComprobante != null
                ? transaccionRepository.findFirstByNumeroComprobante(numeroComprobante).orElse(null)
                : null;

        if (existente != null && liquidadaTotalmente) {
            existente.setEstado(EstadoFinanciero.PAGADO);
            transaccionRepository.save(existente);
        } else if (existente == null) {
            TransaccionFinanciera trxPago = new TransaccionFinanciera();
            trxPago.setCodigo(generarCodigoUnico("TRX-PAG", numeroComprobante));
            trxPago.setConcepto(truncar("Amortización compra " + numeroComprobante + " - " + razonSocialProveedor, 195));
            trxPago.setMonto(montoPagado);
            trxPago.setFechaTransaccion(LocalDate.now());
            trxPago.setTipo(TipoFinanciero.EGRESO);
            trxPago.setEstado(EstadoFinanciero.PAGADO);
            trxPago.setTipoComprobante("FACTURA");
            trxPago.setNumeroComprobante(numeroComprobante);
            trxPago.setCategoriaFinanciera(catCompra);
            transaccionRepository.save(trxPago);
        }
    }

    @Override
    @Transactional
    public void anularFacturaCompra(String numeroComprobante) {
        if (numeroComprobante == null) {
            return;
        }
        transaccionRepository.findFirstByNumeroComprobante(numeroComprobante).ifPresent(trx -> {
            trx.setEstado(EstadoFinanciero.ANULADO);
            transaccionRepository.save(trx);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneSesionAbierta(Long usuarioId) {
        return sesionRepository.existsByUsuarioIdAndEstado(usuarioId, EstadoCaja.ABIERTA);
    }

    @Override
    @Transactional(readOnly = true)
    public Long obtenerIdSesionActiva(Long usuarioId) {
        return sesionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoCaja.ABIERTA)
                .map(SesionCaja::getId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene una sesión de caja activa."));
    }

    private CategoriaFinanciera obtenerOCrearCategoria(TipoFinanciero tipo, String nombreFallback) {
        return categoriaRepository.findByTipoDefault(tipo).stream()
                .findFirst()
                .orElseGet(() -> {
                    CategoriaFinanciera nueva = new CategoriaFinanciera();
                    nueva.setNombre(nombreFallback);
                    nueva.setDescripcion("Categoría autogenerada para operaciones integradas del ERP");
                    nueva.setTipoDefault(tipo);
                    return categoriaRepository.save(nueva);
                });
    }

    private String generarCodigoUnico(String prefijo, String referencia) {
        String limpio = referencia != null ? referencia.replaceAll("[^A-Za-z0-9]", "") : "";
        if (limpio.length() > 18) {
            limpio = limpio.substring(limpio.length() - 18);
        }
        String candidato = prefijo + "-" + limpio + "-" + (int) (Math.random() * 900 + 100);
        if (candidato.length() > 48) {
            candidato = candidato.substring(0, 48);
        }
        if (transaccionRepository.existsByCodigo(candidato)) {
            candidato = prefijo + "-" + System.currentTimeMillis();
        }
        return candidato;
    }

    private String truncar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
