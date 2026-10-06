package veterinaria.vargasvet.domain.enums;

public enum FinalidadDatos {
    ENTERADO("Fue informada del aviso de privacidad", false),
    RECORDATORIOS_PREVENTIVOS("Recordatorios de vacunas y desparasitaciones por correo", true),
    USO_IA_CLINICA("Uso de inteligencia artificial con los datos clínicos de sus mascotas", true);

    private final String descripcion;
    private final boolean opcional;

    FinalidadDatos(String descripcion, boolean opcional) {
        this.descripcion = descripcion;
        this.opcional = opcional;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isOpcional() {
        return opcional;
    }
}
