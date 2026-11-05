package pe.edu.upeu.bomerp.terceros.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.edu.upeu.bomerp.terceros.entity.Tercero;
import pe.edu.upeu.bomerp.terceros.repository.TerceroRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class TerceroDataInitializer implements CommandLineRunner {

    private final TerceroRepository terceroRepository;

    @Override
    public void run(String... args) {
        if (terceroRepository.count() == 0) {
            log.info("Inicializando directorio semilla de Clientes y Proveedores (Terceros)...");
            List<Tercero> semilla = List.of(
                    Tercero.builder()
                            .tipoDocumento("RUC")
                            .numeroDocumento("20608974512")
                            .razonSocial("Agro Peruvian Foods S.A.C. (Planta Central)")
                            .tipoTercero("AMBOS")
                            .telefono("01-4458920")
                            .direccion("Av. Las Lomas 240, Lurigancho, Lima")
                            .correo("operaciones@agroperu.com")
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build(),
                    Tercero.builder()
                            .tipoDocumento("RUC")
                            .numeroDocumento("20100070970")
                            .razonSocial("Supermercados Peruanos S.A. (Plaza Vea / Vivanda)")
                            .tipoTercero("CLIENTE")
                            .telefono("01-6188000")
                            .direccion("Calle Morelli 181, San Borja, Lima")
                            .correo("compras.corporativas@spsa.com.pe")
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build(),
                    Tercero.builder()
                            .tipoDocumento("RUC")
                            .numeroDocumento("20512807490")
                            .razonSocial("Cencosud Retail Perú S.A. (Wong / Metro)")
                            .tipoTercero("CLIENTE")
                            .telefono("01-6138888")
                            .direccion("Av. Raúl Ferrero 1205, La Molina, Lima")
                            .correo("proveedores@cencosud.com.pe")
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build(),
                    Tercero.builder()
                            .tipoDocumento("RUC")
                            .numeroDocumento("20456123789")
                            .razonSocial("Distribuidora Gourmet Miraflores S.A.C.")
                            .tipoTercero("CLIENTE")
                            .telefono("987654321")
                            .direccion("Av. José Pardo 450, Miraflores, Lima")
                            .correo("ventas@gourmetlima.pe")
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build(),
                    Tercero.builder()
                            .tipoDocumento("RUC")
                            .numeroDocumento("20330456781")
                            .razonSocial("Cooperativa Agraria Cafetalera Chuncho de Quillabamba")
                            .tipoTercero("PROVEEDOR")
                            .telefono("084-281090")
                            .direccion("Jr. Espinar 450, Quillabamba, La Convención, Cusco")
                            .correo("ventas@cacaochuncho.org.pe")
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build(),
                    Tercero.builder()
                            .tipoDocumento("RUC")
                            .numeroDocumento("20100234567")
                            .razonSocial("Agrícola del Valle del Chira S.A.C.")
                            .tipoTercero("PROVEEDOR")
                            .telefono("073-501234")
                            .direccion("Carretera Sullana - Tambogrande Km 14, Piura")
                            .correo("abastecimiento@cocosdelchira.pe")
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build(),
                    Tercero.builder()
                            .tipoDocumento("RUC")
                            .numeroDocumento("20501984521")
                            .razonSocial("Envases y Cristales del Perú S.A.")
                            .tipoTercero("PROVEEDOR")
                            .telefono("01-5740090")
                            .direccion("Av. Elmer Faucett 3450, Callao")
                            .correo("pedidos@envasesperu.com")
                            .activo(true)
                            .fechaCreacion(LocalDateTime.now())
                            .build()
            );
            terceroRepository.saveAll(semilla);
            log.info("Directorio de Terceros inicializado con {} registros.", semilla.size());
        }
    }
}
