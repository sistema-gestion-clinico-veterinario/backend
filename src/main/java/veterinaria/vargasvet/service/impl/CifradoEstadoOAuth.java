package veterinaria.vargasvet.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CifradoEstadoOAuth {

    private static final int BYTES_NONCE = 12;
    private static final int BITS_ETIQUETA = 128;
    private static final String DOMINIO_DE_DERIVACION = "oauth-google-estado-v1";

    private final SecretKeySpec clave;
    private final SecureRandom azar = new SecureRandom();

    @Autowired
    public CifradoEstadoOAuth(@Value("${app.oauth-state.key:}") String claveConfigurada,
                              @Value("${jwt.private-key:classpath:keys/private_key.pem}") String ubicacionLlaveJwt,
                              ResourceLoader cargador) throws IOException {
        this(claveConfigurada == null || claveConfigurada.isBlank()
                ? derivarDeLaLlaveJwt(cargador.getResource(ubicacionLlaveJwt))
                : decodificar(claveConfigurada));
    }

    CifradoEstadoOAuth(byte[] clave) {
        if (clave == null || clave.length != 32) {
            throw new IllegalStateException("La clave del estado de OAuth debe tener 32 bytes");
        }
        this.clave = new SecretKeySpec(clave, "AES");
    }

    public String cifrar(String texto, String contexto) {
        try {
            byte[] nonce = new byte[BYTES_NONCE];
            azar.nextBytes(nonce);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, nonce));
            cifrador.updateAAD(contexto.getBytes(StandardCharsets.UTF_8));
            byte[] cifrado = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] unido = new byte[nonce.length + cifrado.length];
            System.arraycopy(nonce, 0, unido, 0, nonce.length);
            System.arraycopy(cifrado, 0, unido, nonce.length, cifrado.length);
            return Base64.getEncoder().encodeToString(unido);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cifrar el estado de OAuth", ex);
        }
    }

    public String descifrar(String contenido, String contexto) {
        try {
            byte[] unido = Base64.getDecoder().decode(contenido);
            if (unido.length <= BYTES_NONCE) {
                throw new IllegalArgumentException("Contenido cifrado demasiado corto");
            }
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, unido, 0, BYTES_NONCE));
            cifrador.updateAAD(contexto.getBytes(StandardCharsets.UTF_8));
            return new String(cifrador.doFinal(unido, BYTES_NONCE, unido.length - BYTES_NONCE), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("El estado de OAuth no se pudo descifrar", ex);
        }
    }

    private static byte[] decodificar(String base64) {
        try {
            return Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.oauth-state.key debe estar en Base64 (openssl rand -base64 32)", ex);
        }
    }

    private static byte[] derivarDeLaLlaveJwt(Resource recurso) throws IOException {
        try (InputStream entrada = recurso.getInputStream()) {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            resumen.update(DOMINIO_DE_DERIVACION.getBytes(StandardCharsets.UTF_8));
            resumen.update(entrada.readAllBytes());
            return resumen.digest();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}
