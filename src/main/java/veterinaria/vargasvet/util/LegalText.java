package veterinaria.vargasvet.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades para publicar documentos legales: huella estable del texto y detección de borradores. */
public final class LegalText {

    private static final Pattern CAMPO_ENTRE_CORCHETES = Pattern.compile("\\[(?!\\s*\\d+\\s*\\])[^\\]\\n]{1,120}\\]");
    private static final Pattern MARCADOR_DE_PLANTILLA = Pattern.compile(
            "\\{\\{[^}\\n]*\\}\\}|\\$\\{[^}\\n]*\\}|<[A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ _]{1,40}>|\\b[Xx]{4,}\\b|_{4,}|\\b0{8,}\\b");
    private static final Pattern TEXTO_DE_RELLENO = Pattern.compile("\\b(lorem ipsum|fixme)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BORRADOR = Pattern.compile(
            "borrador\\s+(provisional|preliminar|inicial|no\\s+definitivo)|pendiente\\s+de\\s+(validaci[oó]n|revisi[oó]n)"
                    + "|\\b(draft|sin\\s+valor\\s+legal)\\b",
            Pattern.CASE_INSENSITIVE);

    private LegalText() {
    }

    /** Normaliza saltos de línea y espacios de los extremos, para que la misma redacción dé siempre la misma huella. */
    public static String normalize(String contenido) {
        return contenido == null ? "" : contenido.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    public static String sha256Hex(String contenido) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    /** Marcas que indican que el texto todavía no es el definitivo: campos por completar, marcadores de plantilla o frases de borrador. */
    public static Set<String> draftMarkers(String contenido) {
        Set<String> marcas = new LinkedHashSet<>();
        if (contenido == null) {
            return marcas;
        }
        for (Pattern patron : List.of(CAMPO_ENTRE_CORCHETES, MARCADOR_DE_PLANTILLA, TEXTO_DE_RELLENO, BORRADOR)) {
            Matcher coincidencias = patron.matcher(contenido);
            while (coincidencias.find()) {
                marcas.add(coincidencias.group());
            }
        }
        return marcas;
    }
}
