package veterinaria.vargasvet;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las pruebas de migraciones vacían el esquema público de la base que se les indica. Antes de hacerlo se exige
 * que sea una base local y con nombre de prueba, para que una variable mal apuntada nunca alcance producción.
 */
public final class BaseDesechableDePruebas {

    private static final Pattern URL_POSTGRES = Pattern.compile("^jdbc:postgresql://(\\[[^\\]]+\\]|[^/:?]+)(?::\\d+)?/([^?]+).*$");
    private static final Set<String> SERVIDORES_LOCALES = Set.of("localhost", "127.0.0.1", "[::1]");
    private static final Pattern NOMBRE_DE_PRUEBA = Pattern.compile("(?i).*(prueba|test|flyway).*");

    private BaseDesechableDePruebas() {
    }

    public static void exigir(String url) {
        if (url == null) {
            throw new IllegalStateException("No se indicó la dirección de la base de pruebas");
        }
        Matcher coincidencia = URL_POSTGRES.matcher(url.trim());
        if (!coincidencia.matches()) {
            throw new IllegalStateException("Dirección de base no reconocida; solo se admite jdbc:postgresql://servidor/base");
        }
        String servidor = coincidencia.group(1).toLowerCase();
        String base = coincidencia.group(2);
        if (!SERVIDORES_LOCALES.contains(servidor)) {
            throw new IllegalStateException("Se negó a vaciar la base de «" + servidor
                    + "»: las pruebas de migraciones solo se ejecutan en un servidor local");
        }
        if (!NOMBRE_DE_PRUEBA.matcher(base).matches()) {
            throw new IllegalStateException("Se negó a vaciar la base «" + base
                    + "»: su nombre debe contener «prueba», «test» o «flyway»");
        }
    }
}
