package veterinaria.vargasvet.domain.enums;

public enum ControlCumplimientoClinica {
    AVISO_PRIVACIDAD(true, ModoGestionCumplimiento.SOFTVET, true),
    DERECHOS_DATOS(true, ModoGestionCumplimiento.SOFTVET, false),
    USO_IA(true, ModoGestionCumplimiento.SOFTVET, true),
    RECORDATORIOS_CORREO(true, ModoGestionCumplimiento.SOFTVET, false),
    CONSENTIMIENTO_VETERINARIO(false, ModoGestionCumplimiento.NO_APLICA, false);

    private final boolean habilitadoPorDefecto;
    private final ModoGestionCumplimiento modoPorDefecto;
    private final boolean bloqueantePorDefecto;

    ControlCumplimientoClinica(boolean habilitadoPorDefecto, ModoGestionCumplimiento modoPorDefecto,
                               boolean bloqueantePorDefecto) {
        this.habilitadoPorDefecto = habilitadoPorDefecto;
        this.modoPorDefecto = modoPorDefecto;
        this.bloqueantePorDefecto = bloqueantePorDefecto;
    }

    public boolean isHabilitadoPorDefecto() { return habilitadoPorDefecto; }
    public ModoGestionCumplimiento getModoPorDefecto() { return modoPorDefecto; }
    public boolean isBloqueantePorDefecto() { return bloqueantePorDefecto; }
}
