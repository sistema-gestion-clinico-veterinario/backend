package veterinaria.vargasvet.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.dto.request.EmpresaReferencia;
import veterinaria.vargasvet.dto.request.EspecialidadRequest;
import veterinaria.vargasvet.dto.request.TipoEmpleadoRequest;
import veterinaria.vargasvet.integration.DiagnosticoIAClient.ArchivoParte;
import veterinaria.vargasvet.util.LegalText;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CorreccionesDeCalidadTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean slugValido(String slug) {
        CompanyDTO dto = new CompanyDTO();
        dto.setSlug(slug);
        return validator.validateProperty(dto, "slug").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "clinica", "clinica-vargas-vet", "vet-24", "9-a"})
    void aceptaIdentificadoresDeClinicaBienFormados(String slug) {
        assertThat(slugValido(slug)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-clinica", "clinica-", "clinica--vet", "Clinica", "clinica_vet", "clinica vet", "-", "--"})
    void rechazaIdentificadoresDeClinicaMalFormados(String slug) {
        assertThat(slugValido(slug)).isFalse();
    }

    @Test
    void elIdentificadorDeUnaCadenaMuyLargaSeRechazaSinDesbordarLaPila() {
        assertThat(slugValido("a-".repeat(60000) + "a")).isFalse();
    }

    @Test
    void losArchivosDeLaConsultaSeComparanPorSuContenido() {
        ArchivoParte uno = new ArchivoParte("imagen", "a.png", new byte[] {1, 2, 3});
        ArchivoParte igual = new ArchivoParte("imagen", "a.png", new byte[] {1, 2, 3});
        ArchivoParte otro = new ArchivoParte("imagen", "a.png", new byte[] {1, 2, 4});

        assertThat(uno).isEqualTo(igual).hasSameHashCodeAs(igual).isNotEqualTo(otro);
        assertThat(uno.toString()).contains("bytes=3");
    }

    @Test
    void sumarDiasHabilesSaltaLosFinesDeSemana() {
        LocalDate viernes = LocalDate.of(2026, 10, 9);

        assertThat(CancelacionDatosService.sumarDiasHabiles(viernes, 1)).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(CancelacionDatosService.sumarDiasHabiles(viernes, 5)).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(CancelacionDatosService.sumarDiasHabiles(LocalDate.of(2026, 10, 10), 1)).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void detectaUnBorradorAunqueLleveMayusculasConTilde() {
        assertThat(LegalText.draftMarkers("PENDIENTE DE VALIDACIÓN")).isNotEmpty();
    }

    @Test
    void laEspecialidadSoloTomaDelCuerpoLoQueLeCorresponde() {
        EspecialidadRequest request = new EspecialidadRequest();
        request.setNombre("Cardiología");
        request.setDescripcion("Corazón y vasos");
        request.setCompany(new EmpresaReferencia(7));

        var entidad = request.aEntidad();

        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getNombre()).isEqualTo("Cardiología");
        assertThat(entidad.getCompany().getId()).isEqualTo(7);
    }

    @Test
    void elTipoDeEmpleadoNoPermiteFijarElEstadoDesdeElCuerpo() {
        TipoEmpleadoRequest request = new TipoEmpleadoRequest();
        request.setNombre("Veterinario");
        request.setPermiteEspecialidades(true);
        request.setEsVeterinario(false);

        var entidad = request.aEntidad();

        assertThat(entidad.getEstado()).isTrue();
        assertThat(entidad.getPermiteEspecialidades()).isTrue();
        assertThat(entidad.ejerceComoVeterinario()).isFalse();
        assertThat(entidad.getCompany()).isNull();
    }

    @Test
    void laEspecialidadMantieneLasMismasValidacionesDeNombre() {
        EspecialidadRequest request = new EspecialidadRequest();
        request.setNombre("Cardio123");

        assertThat(validator.validate(request)).isNotEmpty();
        request.setNombre("Cardiología");
        assertThat(validator.validate(request)).isEmpty();
    }
}
