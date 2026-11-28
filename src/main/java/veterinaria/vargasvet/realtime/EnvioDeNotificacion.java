package veterinaria.vargasvet.realtime;

@FunctionalInterface
public interface EnvioDeNotificacion {

    void enviar(String canal, String carga);
}
