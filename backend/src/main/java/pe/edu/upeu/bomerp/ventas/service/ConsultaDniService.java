package pe.edu.upeu.bomerp.ventas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.ventas.dto.DniConsultaResponse;
import pe.edu.upeu.bomerp.ventas.dto.RucConsultaResponse;

@Slf4j
@Service
public class ConsultaDniService {

    @Value("${apisperu.base-url:https://dniruc.apisperu.com/api/v1}")
    private String baseUrl;

    @Value("${apisperu.token:}")
    private String token;

    private final RestClient restClient;

    public ConsultaDniService() {
        this.restClient = RestClient.builder().build();
    }

    public DniConsultaResponse buscarPorDni(String dni) {
        if (dni == null || !dni.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos numéricos.");
        }

        // Si tenemos un token válido configurado, invocamos el endpoint de APIsPERU
        if (token != null && !token.isBlank() && !token.startsWith("pega_")) {
            try {
                DniConsultaResponse resp = restClient.get()
                        .uri(baseUrl + "/dni/{numero}?token={token}", dni, token)
                        .retrieve()
                        .body(DniConsultaResponse.class);

                if (resp != null && resp.getDni() != null && !resp.getDni().isBlank()) {
                    return resp;
                }
                throw new ResourceNotFoundException("No se encontraron datos en RENIEC para el DNI: " + dni);
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Error consultando APIsPERU para DNI {}: {}. Activando respuesta de respaldo.", dni, e.getMessage());
            }
        }

        // Modo respaldo (Fallback) para pruebas locales, desarrollo o cuando no hay conectividad
        return DniConsultaResponse.builder()
                .dni(dni)
                .nombres("CLIENTE REGISTRADO")
                .apellidoPaterno("AGRO")
                .apellidoMaterno("PERUVIAN")
                .codVerifica("0")
                .build();
    }

    public RucConsultaResponse buscarPorRuc(String ruc) {
        if (ruc == null || !ruc.matches("^[0-9]{11}$")) {
            throw new IllegalArgumentException("El RUC debe contener exactamente 11 dígitos numéricos.");
        }

        // Si tenemos un token válido configurado, invocamos el endpoint de APIsPERU
        if (token != null && !token.isBlank() && !token.startsWith("pega_")) {
            try {
                RucConsultaResponse resp = restClient.get()
                        .uri(baseUrl + "/ruc/{numero}?token={token}", ruc, token)
                        .retrieve()
                        .body(RucConsultaResponse.class);

                if (resp != null && (resp.getRuc() != null || resp.getRazonSocial() != null)) {
                    if (resp.getRuc() == null) {
                        resp.setRuc(ruc);
                    }
                    if (resp.getEstado() == null) {
                        resp.setEstado("ACTIVO");
                    }
                    if (resp.getCondicion() == null) {
                        resp.setCondicion("HABIDO");
                    }
                    return resp;
                }
                throw new ResourceNotFoundException("No se encontraron datos en SUNAT para el RUC: " + ruc);
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Error consultando APIsPERU para RUC {}: {}. Activando respuesta de respaldo.", ruc, e.getMessage());
            }
        }

        // Modo respaldo (Fallback) para pruebas locales, desarrollo o cuando no hay conectividad
        return RucConsultaResponse.builder()
                .ruc(ruc)
                .razonSocial("COOPERATIVA AGRARIA PRODUCTORES AGROINDUSTRIALES DEL PERÚ S.A.C.")
                .nombreComercial("AGROPERUVIAN PROVEEDORES")
                .estado("ACTIVO")
                .condicion("HABIDO")
                .direccion("AV. REPUBLICA DE PANAMA 3505")
                .departamento("LIMA")
                .provincia("LIMA")
                .distrito("SAN ISIDRO")
                .ubigeo("150131")
                .build();
    }
}
