package veterinaria.vargasvet.domain.enums;

public enum TipoDerechoDatos {
    ACCESO(20),
    RECTIFICACION(10),
    OPOSICION(10),
    PORTABILIDAD(20);

    private final int diasHabiles;

    TipoDerechoDatos(int diasHabiles) {
        this.diasHabiles = diasHabiles;
    }

    public int getDiasHabiles() {
        return diasHabiles;
    }
}

