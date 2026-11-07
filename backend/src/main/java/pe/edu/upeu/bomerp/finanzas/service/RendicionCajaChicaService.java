package pe.edu.upeu.bomerp.finanzas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.edu.upeu.bomerp.finanzas.dto.CajaChicaResponse;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.bomerp.finanzas.dto.RendicionResumenDto;
import pe.edu.upeu.bomerp.finanzas.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.bomerp.finanzas.entity.EstadoRendicion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RendicionCajaChicaService {
    CajaChicaResponse crearCajaChica(String codigo, String responsable, BigDecimal limiteMaximo, BigDecimal saldoInicial);
    CajaChicaResponse obtenerCajaChica(Long id);

    RendicionCajaChicaResponse registrarRendicion(RendicionCajaChicaRequest request);
    RendicionCajaChicaResponse obtenerRendicion(Long id);
    List<RendicionCajaChicaResponse> listarRendicionesPorCaja(Long cajaChicaId);
    Page<RendicionCajaChicaResponse> listarPaginado(Pageable pageable);

    Page<RendicionCajaChicaResponse> buscarConFiltros(
            Long cajaChicaId,
            EstadoRendicion estado,
            String responsable,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax,
            Pageable pageable
    );

    List<CajaChicaResponse> listarCajasChicas();
    RendicionCajaChicaResponse anularRendicion(Long id);

    List<RendicionResumenDto> obtenerResumen(Long cajaChicaId);

    ReporteRendicionesAgregadoDto obtenerReporteAgregado(Long cajaChicaId);

    pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse asignarPresupuestoCentroCosto(Long cajaChicaId, Long centroCostoId, BigDecimal montoAsignado);

    List<pe.edu.upeu.bomerp.finanzas.dto.AsignacionCajaCentroCostoResponse> listarPresupuestosPorCaja(Long cajaChicaId);

    List<pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse> listarCentrosCosto();

    pe.edu.upeu.bomerp.finanzas.dto.CentroCostoResponse crearCentroCosto(pe.edu.upeu.bomerp.finanzas.dto.CentroCostoRequest request);
}

