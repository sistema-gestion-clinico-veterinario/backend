package veterinaria.vargasvet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import veterinaria.vargasvet.realtime.DifusionPostgres;
import veterinaria.vargasvet.realtime.EntregaLocalTiempoReal;
import veterinaria.vargasvet.realtime.EscuchaPostgres;

import java.util.Properties;
import java.util.UUID;

@Configuration
@ConditionalOnProperty(name = "app.realtime.distribuido", havingValue = "true")
public class DifusionTiempoRealConfig {

    static final String CANAL = "vetsoft_tiempo_real";
    private static final int CAPACIDAD_COLA = 1000;

    public record IdentidadDeInstancia(String valor) {
    }

    @Bean
    public IdentidadDeInstancia identidadDeInstancia() {
        return new IdentidadDeInstancia(UUID.randomUUID().toString());
    }

    @Bean(destroyMethod = "detener")
    public DifusionPostgres difusionEntreInstancias(SimpMessagingTemplate mensajeria, JdbcTemplate jdbc,
                                                    IdentidadDeInstancia identidad) {
        return new DifusionPostgres(mensajeria.getMessageConverter(),
                (canal, carga) -> jdbc.query("SELECT pg_notify(?, ?)", resultado -> { }, canal, carga),
                CANAL, identidad.valor(), CAPACIDAD_COLA);
    }

    @Bean(initMethod = "iniciar", destroyMethod = "detener")
    public EscuchaPostgres escuchaDeTiempoReal(SimpMessagingTemplate mensajeria, IdentidadDeInstancia identidad,
                                               @Value("${spring.datasource.url}") String url,
                                               @Value("${spring.datasource.username}") String usuario,
                                               @Value("${spring.datasource.password}") String clave,
                                               @Value("${spring.datasource.hikari.data-source-properties.sslmode:require}") String sslmode) {
        Properties propiedades = new Properties();
        propiedades.setProperty("sslmode", sslmode);
        return new EscuchaPostgres(url, usuario, clave, propiedades, CANAL, identidad.valor(),
                new EntregaLocalTiempoReal(mensajeria));
    }
}
