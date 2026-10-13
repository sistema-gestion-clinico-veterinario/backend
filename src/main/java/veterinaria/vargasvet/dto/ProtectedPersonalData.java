package veterinaria.vargasvet.dto;

/**
 * Respuesta que lleva documento, teléfono o correo de otra persona. Se entrega completa solo a quien puede
 * registrar o modificar esa información en {@link #authorizingView()}; al resto se le oculta parcialmente.
 */
public interface ProtectedPersonalData {

    String authorizingView();

    void maskSensitiveData();
}
