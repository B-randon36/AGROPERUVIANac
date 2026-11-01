package pe.edu.upeu.bomerp.produccion.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.bomerp.exception.ResourceNotFoundException;
import pe.edu.upeu.bomerp.produccion.entity.CentroTrabajo;
import pe.edu.upeu.bomerp.produccion.entity.DetalleConsumoInsumo;
import pe.edu.upeu.bomerp.produccion.entity.OrdenProduccion;
import pe.edu.upeu.bomerp.produccion.repository.CentroTrabajoRepository;
import pe.edu.upeu.bomerp.produccion.repository.OrdenProduccionRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReporteOrdenPdfService {

    private static final Color COLOR_PRIMARIO = new Color(30, 81, 40);
    private static final Color COLOR_SECUNDARIO = new Color(79, 111, 82);
    private static final Color COLOR_FONDO_TABLA = new Color(245, 247, 248);
    private static final Color COLOR_TEXTO_OSCURO = new Color(33, 37, 41);
    private static final Color COLOR_BORDE = new Color(222, 226, 230);

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARIO);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_SECUNDARIO);
    private static final Font FONT_HEADER_TABLA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXTO_OSCURO);
    private static final Font FONT_VALOR = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXTO_OSCURO);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final CentroTrabajoRepository centroTrabajoRepository;

    @Transactional(readOnly = true)
    public byte[] generarReportePdf(Long ordenId) {
        OrdenProduccion orden = ordenProduccionRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + ordenId));

        CentroTrabajo centro = null;
        if (orden.getCentroTrabajoId() != null) {
            centro = centroTrabajoRepository.findById(orden.getCentroTrabajoId()).orElse(null);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 42);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new AuditoriaPageEvent());

            document.open();

            agregarCabecera(document, orden);
            document.add(new Paragraph(" "));

            agregarSeccionTrazabilidad(document, orden);
            document.add(new Paragraph(" "));

            agregarSeccionCentroTrabajo(document, orden, centro);
            document.add(new Paragraph(" "));

            agregarSeccionMateriales(document, orden);
            document.add(new Paragraph(" "));

            if (orden.getObservaciones() != null && !orden.getObservaciones().isBlank()) {
                agregarSeccionObservaciones(document, orden.getObservaciones());
                document.add(new Paragraph(" "));
            }

            agregarFirmasAuditoria(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el reporte PDF para la orden #" + ordenId, e);
        }
    }

    private void agregarCabecera(Document doc, OrdenProduccion orden) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{65f, 35f});

        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);
        cellLeft.addElement(new Paragraph("AGRO PERUVIAN FOODS S.A.C.", FONT_SUBTITULO));
        cellLeft.addElement(new Paragraph("Orden de Producción: " + orden.getCodigoOrden(), FONT_TITULO));
        cellLeft.addElement(new Paragraph("RUC: 20601234567 | Planta Agroindustrial Certificada HACCP", FONT_FOOTER));
        table.addCell(cellLeft);

        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph emision = new Paragraph("Emisión: " + LocalDateTime.now().format(DATE_FORMATTER), FONT_VALOR);
        emision.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(emision);

        String estadoTexto = orden.getEstado() != null ? orden.getEstado().name() : "N/D";
        Paragraph estado = new Paragraph("Estado: " + estadoTexto, FONT_LABEL);
        estado.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(estado);

        Paragraph version = new Paragraph("Control Rev.: v" + (orden.getVersion() != null ? orden.getVersion() : 0L), FONT_FOOTER);
        version.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(version);

        table.addCell(cellRight);
        doc.add(table);

        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        separator.setSpacingBefore(6f);
        PdfPCell sepCell = new PdfPCell();
        sepCell.setFixedHeight(2.5f);
        sepCell.setBackgroundColor(COLOR_PRIMARIO);
        sepCell.setBorder(Rectangle.NO_BORDER);
        separator.addCell(sepCell);
        doc.add(separator);
    }

    private void agregarSeccionTrazabilidad(Document doc, OrdenProduccion orden) throws DocumentException {
        doc.add(new Paragraph("1. TRAZABILIDAD Y RENDIMIENTO DE LOTE", FONT_SUBTITULO));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{34f, 22f, 22f, 22f});

        agregarHeaderCelda(table, "Producto Terminado");
        agregarHeaderCelda(table, "Cant. Programada");
        agregarHeaderCelda(table, "Cant. Producida");
        agregarHeaderCelda(table, "Eficiencia de Planta");

        String eficienciaTexto = "Pendiente";
        if (orden.getCantidadProducida() != null && orden.getCantidadProgramada() != null && orden.getCantidadProgramada() > 0) {
            double ef = (orden.getCantidadProducida() * 100.0) / orden.getCantidadProgramada();
            eficienciaTexto = String.format("%.2f %%", ef);
        }

        agregarDatoCelda(table, orden.getNombreProducto());
        agregarDatoCeldaCentrada(table, orden.getCantidadProgramada() != null ? orden.getCantidadProgramada() + " unid." : "-");
        agregarDatoCeldaCentrada(table, orden.getCantidadProducida() != null ? orden.getCantidadProducida() + " unid." : "En ejecución");
        agregarDatoCeldaCentrada(table, eficienciaTexto);

        agregarHeaderCelda(table, "Centro de Trabajo / Línea");
        agregarHeaderCelda(table, "Fecha / Hora Inicio");
        agregarHeaderCelda(table, "Fecha / Hora Cierre");
        agregarHeaderCelda(table, "Estado Operativo");

        String nombreLinea = orden.getCentroTrabajoNombre() != null && !orden.getCentroTrabajoNombre().isBlank()
                ? orden.getCentroTrabajoNombre()
                : "Línea Principal de Procesamiento";
        agregarDatoCelda(table, nombreLinea);
        agregarDatoCeldaCentrada(table, orden.getFechaInicio() != null ? orden.getFechaInicio().format(DATE_FORMATTER) : "N/D");
        agregarDatoCeldaCentrada(table, orden.getFechaFin() != null ? orden.getFechaFin().format(DATE_FORMATTER) : "Abierta");
        agregarDatoCeldaCentrada(table, orden.getEstado() != null ? orden.getEstado().name() : "-");

        doc.add(table);
    }

    private void agregarSeccionCentroTrabajo(Document doc, OrdenProduccion orden, CentroTrabajo centro) throws DocumentException {
        doc.add(new Paragraph("2. PARÁMETROS DE CENTRO DE TRABAJO", FONT_SUBTITULO));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40f, 30f, 30f});

        agregarHeaderCelda(table, "Línea Asignada");
        agregarHeaderCelda(table, "Capacidad Nominal");
        agregarHeaderCelda(table, "Costo Operativo / Hora");

        String linea = centro != null
                ? (centro.getCodigo() + " - " + centro.getNombre())
                : (orden.getCentroTrabajoNombre() != null ? orden.getCentroTrabajoNombre() : "Línea General Agroindustrial");

        String cap = centro != null && centro.getCapacidad() != null
                ? (centro.getCapacidad().setScale(2, RoundingMode.HALF_UP) + " " + (centro.getUnidadCapacidad() != null ? centro.getUnidadCapacidad() : "UNID/HORA"))
                : "Estándar de Planta";

        String tarifa = centro != null && centro.getCostoHora() != null
                ? ("S/ " + centro.getCostoHora().setScale(2, RoundingMode.HALF_UP))
                : "S/ 45.00";

        agregarDatoCelda(table, linea);
        agregarDatoCeldaCentrada(table, cap);
        agregarDatoCeldaCentrada(table, tarifa);

        doc.add(table);
    }

    private void agregarSeccionMateriales(Document doc, OrdenProduccion orden) throws DocumentException {
        doc.add(new Paragraph("3. LISTA DE MATERIALES E INSUMOS CONSUMIDOS (RECETA BOM)", FONT_SUBTITULO));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{12f, 40f, 16f, 16f, 16f});

        agregarHeaderCelda(table, "ID Insumo");
        agregarHeaderCelda(table, "Insumo / Materia Prima / Envase");
        agregarHeaderCelda(table, "Cant. Requerida");
        agregarHeaderCelda(table, "Cant. Consumida");
        agregarHeaderCelda(table, "Unidad");

        if (orden.getInsumosConsumidos() == null || orden.getInsumosConsumidos().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No se registraron insumos asociados a esta orden.", FONT_VALOR));
            empty.setColspan(5);
            empty.setPadding(6f);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setBorderColor(COLOR_BORDE);
            table.addCell(empty);
        } else {
            for (DetalleConsumoInsumo det : orden.getInsumosConsumidos()) {
                agregarDatoCeldaCentrada(table, det.getInsumoId() != null ? ("#" + det.getInsumoId()) : "-");
                agregarDatoCelda(table, det.getNombreInsumo());
                agregarDatoCeldaDerecha(table, formatearDecimal(det.getCantidadRequerida()));
                agregarDatoCeldaDerecha(table, formatearDecimal(det.getCantidadConsumida()));
                agregarDatoCeldaCentrada(table, det.getUnidadMedida());
            }
        }

        doc.add(table);
    }

    private void agregarSeccionObservaciones(Document doc, String observaciones) throws DocumentException {
        doc.add(new Paragraph("4. OBSERVACIONES TÉCNICAS Y CONTROL SANITARIO", FONT_SUBTITULO));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(observaciones, FONT_VALOR));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setPadding(6f);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
        doc.add(table);
    }

    private void agregarFirmasAuditoria(Document doc) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(34f);
        table.setWidths(new float[]{50f, 50f});

        PdfPCell firma1 = new PdfPCell();
        firma1.setBorder(Rectangle.NO_BORDER);
        Paragraph l1 = new Paragraph("____________________________________", FONT_VALOR);
        l1.setAlignment(Element.ALIGN_CENTER);
        Paragraph t1 = new Paragraph("Supervisor de Producción y Planta", FONT_LABEL);
        t1.setAlignment(Element.ALIGN_CENTER);
        Paragraph s1 = new Paragraph("Agro Peruvian Foods S.A.C.", FONT_FOOTER);
        s1.setAlignment(Element.ALIGN_CENTER);
        firma1.addElement(l1);
        firma1.addElement(t1);
        firma1.addElement(s1);

        PdfPCell firma2 = new PdfPCell();
        firma2.setBorder(Rectangle.NO_BORDER);
        Paragraph l2 = new Paragraph("____________________________________", FONT_VALOR);
        l2.setAlignment(Element.ALIGN_CENTER);
        Paragraph t2 = new Paragraph("Auditor de Control de Calidad HACCP", FONT_LABEL);
        t2.setAlignment(Element.ALIGN_CENTER);
        Paragraph s2 = new Paragraph("Aseguramiento de Calidad e Inocuidad", FONT_FOOTER);
        s2.setAlignment(Element.ALIGN_CENTER);
        firma2.addElement(l2);
        firma2.addElement(t2);
        firma2.addElement(s2);

        table.addCell(firma1);
        table.addCell(firma2);
        doc.add(table);
    }

    private String formatearDecimal(BigDecimal val) {
        if (val == null) {
            return "0.000";
        }
        return val.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private void agregarHeaderCelda(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_HEADER_TABLA));
        cell.setBackgroundColor(COLOR_SECUNDARIO);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    private void agregarDatoCelda(PdfPTable table, String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor != null ? valor : "-", FONT_VALOR));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setPadding(5f);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    private void agregarDatoCeldaCentrada(PdfPTable table, String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor != null ? valor : "-", FONT_VALOR));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    private void agregarDatoCeldaDerecha(PdfPTable table, String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor != null ? valor : "-", FONT_VALOR));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    private static class AuditoriaPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase(
                    String.format("Documento Oficial de Trazabilidad BOM - Pág. %d | Agro Peruvian Foods S.A.C. - ERP Modulith", writer.getPageNumber()),
                    FONT_FOOTER
            );
            ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 16,
                    0
            );
        }
    }
}

