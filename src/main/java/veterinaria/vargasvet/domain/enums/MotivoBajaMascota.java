package veterinaria.vargasvet.domain.enums;

public enum MotivoBajaMascota {
    FALLECIMIENTO,
    DEJA_ASISTIR,
    CAMBIO_PROPIETARIO,
    OTRO,
    /** Solo lo asigna el sistema: la mascota quedó sin ninguna persona activa que autorice su atención. */
    SUSPENSION_DEL_PROPIETARIO,
    BAJA_DEL_PROPIETARIO;

    /** Los motivos que el sistema pone y retira solo; el personal no puede elegirlos. */
    public boolean esAutomatico() {
        return this == SUSPENSION_DEL_PROPIETARIO || this == BAJA_DEL_PROPIETARIO;
    }
}
