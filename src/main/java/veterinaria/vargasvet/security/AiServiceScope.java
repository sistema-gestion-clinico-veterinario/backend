package veterinaria.vargasvet.security;

/** Operaciones que el backend puede delegar al microservicio de IA. */
public enum AiServiceScope {
    RADIOGRAFIA("radiografia"),
    LABORATORIO("laboratorio"),
    DIAGNOSTICO("diagnostico");

    private final String value;

    AiServiceScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
