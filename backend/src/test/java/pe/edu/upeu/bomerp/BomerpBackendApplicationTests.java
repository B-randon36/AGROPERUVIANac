package pe.edu.upeu.bomerp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.upeu.bomerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.bomerp.ventas.repository.VentaRepository;
import pe.edu.upeu.bomerp.finanzas.repository.SesionCajaRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("h2")
class BomerpBackendApplicationTests {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private SesionCajaRepository sesionCajaRepository;

    @Test
    @DisplayName("El contexto de Spring Boot y los datos semilla en H2 deben inicializarse correctamente")
    void contextLoadsAndSeedDataPopulated() {
        assertTrue(productoRepository.count() >= 16, "Deben haberse insertado los 16 productos oficiales desde data.sql");
        assertTrue(ventaRepository.count() >= 0, "El esquema de ventas debe estar inicializado (inicia limpio para el POS)");
        assertTrue(sesionCajaRepository.count() >= 1, "Debe existir al menos 1 sesion de caja semilla");
    }
}
