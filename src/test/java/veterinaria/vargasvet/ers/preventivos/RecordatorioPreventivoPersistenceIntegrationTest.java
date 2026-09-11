package veterinaria.vargasvet.ers.preventivos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.RecordatorioPreventivo;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoRecordatorio;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.impl.RecordatorioPreventivoServiceImpl;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
class RecordatorioPreventivoPersistenceIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ControlPreventivoRepository controlRepository;
    @Autowired private RecordatorioPreventivoRepository recordatorioRepository;

    @Test
    void cpRf3304_laBaseDeDatosImpideDuplicarLaClaveDelRecordatorio() {
        Company company = new Company();
        company.setName("Empresa QA recordatorios");
        company.setRuc("20999999992");
        company.setActivo(true);
        company = companyRepository.saveAndFlush(company);

        Usuario usuario = new Usuario();
        usuario.setEmail("qa-recordatorio-" + UUID.randomUUID() + "@example.test");
        usuario.setPassword("hash-no-real");
        usuario.setNombre("QA");
        usuario.setApellido("Recordatorio");
        usuario.setCompany(company);
        usuario = usuarioRepository.saveAndFlush(usuario);

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento("99" + System.nanoTime());
        apoderado.setGenero(Genero.MASCULINO);
        apoderado = apoderadoRepository.saveAndFlush(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Paciente QA recordatorio");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.saveAndFlush(mascota);

        ControlPreventivo control = new ControlPreventivo();
        control.setMascota(mascota);
        control.setTipo(TipoControlPreventivo.VACUNACION);
        control.setNombreControl("Antirrabica QA");
        control.setFechaRecomendada(AppClock.today().plusDays(3));
        control.setEstado(EstadoControlPreventivo.PROXIMO);
        control.setCreatedBy("qa@test");
        control.setUpdatedBy("qa@test");
        control = controlRepository.saveAndFlush(control);

        LocalDate fechaProgramada = control.getFechaRecomendada();
        recordatorioRepository.saveAndFlush(recordatorio(apoderado, control, fechaProgramada));

        RecordatorioPreventivo duplicado = recordatorio(apoderado, control, fechaProgramada);
        assertThatThrownBy(() -> recordatorioRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cpRf3303_elProcesoProgramadoEnviaYPersisteUnaSolaVez() throws Exception {
        ControlPreventivo control = persistirControlCandidato();
        EmailService emailService = mock(EmailService.class);
        when(emailService.createMail(anyString(), anyString(), anyMap()))
                .thenReturn(new veterinaria.vargasvet.dto.Mail());
        when(emailService.sendEmail(any(), eq("email/recordatorio-preventivo-template")))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));
        RecordatorioPreventivoServiceImpl service = new RecordatorioPreventivoServiceImpl(
                controlRepository, recordatorioRepository, emailService);

        service.procesarRecordatorios();
        recordatorioRepository.flush();
        service.procesarRecordatorios();
        recordatorioRepository.flush();

        assertThat(recordatorioRepository.count()).isEqualTo(1L);
        assertThat(recordatorioRepository.findAll().getFirst().getControlPreventivo().getId())
                .isEqualTo(control.getId());
        verify(emailService, times(1))
                .sendEmail(any(), eq("email/recordatorio-preventivo-template"));

        Scheduled scheduled = RecordatorioPreventivoServiceImpl.class
                .getMethod("procesarRecordatorios")
                .getAnnotation(Scheduled.class);
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).contains("app.reminders.cron");
        assertThat(scheduled.zone()).contains("app.reminders.zone");
    }

    private ControlPreventivo persistirControlCandidato() {
        Company company = new Company();
        company.setName("Empresa QA proceso recordatorios");
        company.setRuc("20" + System.nanoTime());
        company.setActivo(true);
        company = companyRepository.saveAndFlush(company);

        Usuario usuario = new Usuario();
        usuario.setEmail("qa-proceso-" + UUID.randomUUID() + "@example.test");
        usuario.setPassword("hash-no-real");
        usuario.setNombre("QA");
        usuario.setApellido("Proceso");
        usuario.setCompany(company);
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario = usuarioRepository.saveAndFlush(usuario);

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento("98" + System.nanoTime());
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.saveAndFlush(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Paciente QA proceso");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.saveAndFlush(mascota);

        ControlPreventivo control = new ControlPreventivo();
        control.setMascota(mascota);
        control.setTipo(TipoControlPreventivo.VACUNACION);
        control.setNombreControl("Antirrabica QA proceso");
        control.setFechaRecomendada(AppClock.today().plusDays(3));
        control.setEstado(EstadoControlPreventivo.PROGRAMADO);
        control.setCreatedBy("qa@test");
        control.setUpdatedBy("qa@test");
        return controlRepository.saveAndFlush(control);
    }

    private RecordatorioPreventivo recordatorio(
            Apoderado apoderado,
            ControlPreventivo control,
            LocalDate fechaProgramada) {
        RecordatorioPreventivo recordatorio = new RecordatorioPreventivo();
        recordatorio.setApoderado(apoderado);
        recordatorio.setControlPreventivo(control);
        recordatorio.setTipoAviso(TipoAvisoRecordatorio.PROXIMO);
        recordatorio.setFechaProgramada(fechaProgramada);
        recordatorio.setFechaEnvio(LocalDateTime.now());
        recordatorio.setEstado(EstadoRecordatorio.ENVIADO);
        recordatorio.setCreatedBy("qa@test");
        recordatorio.setUpdatedBy("qa@test");
        return recordatorio;
    }
}
