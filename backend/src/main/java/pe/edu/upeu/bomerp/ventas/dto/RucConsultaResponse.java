package pe.edu.upeu.bomerp.ventas.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RucConsultaResponse {

    private Boolean success;
    private String message;
    private String ruc;

    @JsonAlias({"nombre_o_razon_social", "razon_social", "nombre"})
    private String razonSocial;

    @JsonAlias({"nombre_comercial"})
    private String nombreComercial;

    private String estado;
    private String condicion;

    @JsonAlias({"direccion_completa"})
    private String direccion;

    private String departamento;
    private String provincia;
    private String distrito;
    private String ubigeo;

    public String getDireccionCompleta() {
        if (direccion != null && !direccion.isBlank()) {
            StringBuilder sb = new StringBuilder(direccion.trim());
            if (distrito != null && !distrito.isBlank()) sb.append(" - ").append(distrito.trim());
            if (provincia != null && !provincia.isBlank()) sb.append(", ").append(provincia.trim());
            return sb.toString();
        }
        return "";
    }
}

