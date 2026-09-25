package veterinaria.vargasvet.domain.enums;

public enum TipoDescarga {

    HORARIO_PDF("Horarios", "DESCARGAR_PDF_HORARIO", "descargó el PDF del cuadrante de horarios"),
    HORARIO_EXCEL("Horarios", "DESCARGAR_EXCEL_HORARIO", "descargó el Excel del cuadrante de horarios"),
    CARTILLA_VACUNACION("Cartilla", "DESCARGAR_CARTILLA_VACUNACION", "descargó la cartilla de vacunación"),
    CARTILLA_DESPARASITACION("Cartilla", "DESCARGAR_CARTILLA_DESPARASITACION", "descargó la cartilla de desparasitación"),
    RECETA_PDF("Recetas", "IMPRIMIR_RECETA", "imprimió/descargó una receta médica"),
    HISTORIA_CLINICA_PDF("Historias Clínicas", "DESCARGAR_HISTORIA_CLINICA_PDF", "descargó la historia clínica completa en PDF"),
    NOTA_VENTA_PDF("Facturación", "DESCARGAR_NOTA_VENTA_PDF", "descargó la nota de venta de un pago"),
    REPORTE_CLINICO_PDF("Reportes", "DESCARGAR_REPORTE_PDF", "descargó el reporte clínico en PDF"),
    REPORTE_CLINICO_EXCEL("Reportes", "DESCARGAR_REPORTE_EXCEL", "descargó el reporte clínico en Excel");

    private final String modulo;
    private final String accion;
    private final String descripcion;

    TipoDescarga(String modulo, String accion, String descripcion) {
        this.modulo = modulo;
        this.accion = accion;
        this.descripcion = descripcion;
    }

    public String getModulo() {
        return modulo;
    }

    public String getAccion() {
        return accion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
