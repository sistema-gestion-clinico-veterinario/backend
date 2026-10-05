package veterinaria.vargasvet.util;

public final class AuditDetails {

    private AuditDetails() {
    }

    /** Sufijo para el detalle de una auditoría; vacío si no se indicó motivo. */
    public static String reasonSuffix(String reason) {
        return reason == null || reason.isBlank() ? "" : ". Motivo: " + reason.trim();
    }
}
