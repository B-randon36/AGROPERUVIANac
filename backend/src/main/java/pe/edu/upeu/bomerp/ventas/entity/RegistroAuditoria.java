package pe.edu.upeu.bomerp.ventas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUDITORIA", schema = "BOM_VENTAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACTOR", nullable = false, length = 36)
    private String actor; // sub del JWT -- quien ejecuto la accion

    @Column(name = "ACCION", nullable = false, length = 20)
    private String accion; // "CREAR", "ANULAR"

    @Column(name = "ENTIDAD", nullable = false, length = 20)
    private String entidad; // "VENTA"

    @Column(name = "ENTIDAD_ID", nullable = false)
    private Long entidadId;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "DETALLE", length = 200)
    private String detalle;
}

