package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import veterinaria.vargasvet.domain.entity.EmailChangeRequest;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.util.AppClock;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pedir otro cambio de correo mientras hay uno vigente: la solicitud anterior se borra y se
 * crea la nueva. Hibernate ejecuta el INSERT (clave autogenerada) antes que el DELETE
 * pendiente, así que sin vaciar el contexto entre ambos se viola el índice único de usuario.
 */
@DataJpaTest
class EmailChangeRequestRepositoryIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmailChangeRequestRepository repository;

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("u-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername("u-" + UUID.randomUUID());
        usuario.setNombre("Ana");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private EmailChangeRequest solicitud(Usuario usuario, String nuevoCorreo) {
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setNewEmail(nuevoCorreo);
        request.setOldEmailTokenHash(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        request.setNewEmailTokenHash(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        request.setCreatedAt(AppClock.now());
        request.setExpiresAt(AppClock.now().plusMinutes(30));
        return request;
    }

    @Test
    void sinVaciarElContextoReemplazarUnaSolicitudVigenteViolaElIndiceUnico() {
        Usuario usuario = usuario();
        repository.saveAndFlush(solicitud(usuario, "primero@vargasvet.test"));

        repository.deleteByUsuario(usuario);

        assertThatThrownBy(() -> repository.saveAndFlush(solicitud(usuario, "segundo@vargasvet.test")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void vaciandoElContextoDespuesDeBorrarSePuedeReemplazarLaSolicitud() {
        Usuario usuario = usuario();
        repository.saveAndFlush(solicitud(usuario, "primero@vargasvet.test"));

        repository.deleteByUsuario(usuario);
        repository.flush();
        repository.saveAndFlush(solicitud(usuario, "segundo@vargasvet.test"));

        assertThat(repository.findAll())
                .extracting(EmailChangeRequest::getNewEmail)
                .containsExactly("segundo@vargasvet.test");
    }
}
