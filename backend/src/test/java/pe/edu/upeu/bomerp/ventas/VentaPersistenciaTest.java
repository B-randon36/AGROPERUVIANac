package pe.edu.upeu.bomerp.ventas;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import pe.edu.upeu.bomerp.ventas.entity.DetalleVenta;
import pe.edu.upeu.bomerp.ventas.entity.EstadoVenta;
import pe.edu.upeu.bomerp.ventas.entity.MetodoPagoVenta;
import pe.edu.upeu.bomerp.ventas.entity.Venta;
import pe.edu.upeu.bomerp.ventas.repository.VentaRepository;
import pe.edu.upeu.bomerp.ventas.service.NumeroTicketService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class VentaPersistenciaTest {
    private static SessionFactory factory;

    @BeforeAll
    static void iniciarBaseAislada() {
        factory = new Configuration()
                .addAnnotatedClass(Venta.class).addAnnotatedClass(DetalleVenta.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:ventas_correcciones;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS BOM_VENTAS")
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("CREATE SEQUENCE BOM_VENTAS.SEQ_TICKET_VENTA START WITH 1 INCREMENT BY 1 MAXVALUE 999999999999999999")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    @AfterAll
    static void cerrarBase() {
        if (factory != null) factory.close();
    }

    @Test
    void consultasReales_separanVendedoresYExcluyenAnuladasDelMonto() {
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(venta("TKT-TEST-A", "uno", "118.00", EstadoVenta.REGISTRADA));
            em.persist(venta("TKT-TEST-B", "dos", "236.00", EstadoVenta.REGISTRADA));
            em.persist(venta("TKT-TEST-C", "uno", "59.00", EstadoVenta.ANULADA));
            em.flush();
            VentaRepository repo = new JpaRepositoryFactory(em).getRepository(VentaRepository.class);
            var uno = repo.agregados(null, null, null, null, "uno");
            assertEquals(2L, uno.getTotalVentas());
            assertEquals(1L, uno.getTotalAnuladas());
            assertEquals(0, new BigDecimal("118.00").compareTo(uno.getMontoTotal()));
            var filas = repo.buscarResumen(null, null, null, null, "uno", Sort.by("fecha"));
            assertEquals(2, filas.size());
            assertEquals(3L, repo.agregados(null, null, null, null, null).getTotalVentas());
            assertEquals(0L, repo.agregados(null, null, null, null, "sin-ventas").getTotalVentas());
            assertTrue(repo.buscarResumen(null, null, null, null, "sin-ventas", Sort.by("fecha")).isEmpty());
            em.getTransaction().rollback();
        }
    }

    @Test
    void secuencia_con100SolicitudesConcurrentes_noRepiteTickets() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tareas = IntStream.range(0, 100).mapToObj(i -> (Callable<String>) () -> {
                try (EntityManager em = factory.createEntityManager()) {
                    return new NumeroTicketService(em).siguiente();
                }
            }).toList();
            var tickets = new HashSet<String>();
            for (var resultado : executor.invokeAll(tareas)) {
                String ticket = resultado.get();
                assertTrue(ticket.matches("TKT-S-[0-9]{10,18}"));
                assertTrue(tickets.add(ticket), "Ticket duplicado: " + ticket);
            }
            assertEquals(100, tickets.size());
        }
    }

    @Test
    void rollback_noReutilizaNumeroDeTicket() {
        String descartado;
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            descartado = new NumeroTicketService(em).siguiente();
            em.getTransaction().rollback();
        }
        try (EntityManager em = factory.createEntityManager()) {
            assertNotEquals(descartado, new NumeroTicketService(em).siguiente());
        }
    }

    @Test
    void restriccionEnBase_rechazaTicketsDuplicados() {
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(venta("TKT-DUPLICADO", "uno", "118.00", EstadoVenta.REGISTRADA));
            em.flush();
            assertThrows(PersistenceException.class, () -> {
                em.persist(venta("TKT-DUPLICADO", "dos", "118.00", EstadoVenta.REGISTRADA));
                em.flush();
            });
            em.getTransaction().rollback();
        }
    }

    private Venta venta(String ticket, String vendedor, String total, EstadoVenta estado) {
        return Venta.builder().numeroTicket(ticket).vendedorId(vendedor).fecha(LocalDateTime.now())
                .estado(estado).metodoPago(MetodoPagoVenta.EFECTIVO)
                .total(new BigDecimal(total)).subtotal(BigDecimal.ZERO).igv(BigDecimal.ZERO).build();
    }
}
