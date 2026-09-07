package veterinaria.vargasvet.domain.enums;

import java.util.Optional;

public enum ThesisPerformanceOperation {
    SEARCH_MEDICAL_RECORDS("BUSCAR_HISTORIAS_CLINICAS", "Buscar historias clínicas"),
    RETRIEVE_MEDICAL_RECORD("RECUPERAR_HISTORIA_CLINICA", "Recuperar historia clínica"),
    START_CLINICAL_ATTENTION("INICIAR_ATENCION_CLINICA", "Iniciar atención clínica"),
    SAVE_CLINICAL_ATTENTION("GUARDAR_ATENCION_CLINICA", "Guardar atención clínica");

    private final String code;
    private final String displayName;

    ThesisPerformanceOperation(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<ThesisPerformanceOperation> resolve(String method, String routePattern) {
        if (method == null || routePattern == null) {
            return Optional.empty();
        }

        if ("GET".equals(method) && "/medical-records".equals(routePattern)) {
            return Optional.of(SEARCH_MEDICAL_RECORDS);
        }
        if ("GET".equals(method) && (
                "/medical-records/{id}".equals(routePattern)
                        || "/medical-records/pet/{petId}".equals(routePattern)
                        || "/medical-records/numero/{numeroHc}".equals(routePattern))) {
            return Optional.of(RETRIEVE_MEDICAL_RECORD);
        }
        if ("PATCH".equals(method) && "/appointments/{id}/start".equals(routePattern)) {
            return Optional.of(START_CLINICAL_ATTENTION);
        }
        if (("PUT".equals(method) && "/consultations/{id}".equals(routePattern))
                || ("PATCH".equals(method) && "/consultations/{id}/close".equals(routePattern))) {
            return Optional.of(SAVE_CLINICAL_ATTENTION);
        }
        return Optional.empty();
    }
}
