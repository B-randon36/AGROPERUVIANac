package pe.edu.upeu.bomerp.terceros.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TERCEROS", schema = "BOM_CRM")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tercero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TIPO_DOCUMENTO", nullable = false, length = 20)
    private String tipoDocumento;

    @Column(name = "NUMERO_DOCUMENTO", nullable = false, unique = true, length = 30)
    private String numeroDocumento;

    @Column(name = "RAZON_SOCIAL", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "TIPO_TERCERO", nullable = false, length = 20)
    private String tipoTercero; // 'CLIENTE', 'PROVEEDOR', 'AMBOS'

    @Column(name = "TELEFONO", length = 30)
    private String telefono;

    @Column(name = "DIRECCION", length = 250)
    private String direccion;

    @Column(name = "CORREO", length = 120)
    private String correo;

    @Builder.Default
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;

    @Column(name = "FECHA_CREACION")
    private LocalDateTime fechaCreacion;
}
