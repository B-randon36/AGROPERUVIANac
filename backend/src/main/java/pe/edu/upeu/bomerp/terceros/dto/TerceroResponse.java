package pe.edu.upeu.bomerp.terceros.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerceroResponse {
    private Long id;
    private String tipoDocumento;
    private String numeroDocumento;
    private String razonSocial;
    private String tipoTercero;
    private String telefono;
    private String direccion;
    private String correo;
    private Boolean activo;
}
