package veterinaria.vargasvet.ers.personal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HorarioOverlapRepositoryIntegrationTest {

    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private HorarioEmpleadoRepository horarioRepository;

    @Test
    void cpRf1302_detectaCruceRealYNoRequierePersistirUnSegundoTurno() {
        Empleado empleado = new Empleado();
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad("99999992");
        empleado.setGenero(Genero.FEMENINO);
        empleado.setEstado(true);
        empleado = empleadoRepository.saveAndFlush(empleado);

        LocalDate fecha = LocalDate.of(2026, 9, 15);
        HorarioEmpleado existente = new HorarioEmpleado();
        existente.setEmpleado(empleado);
        existente.setFecha(fecha);
        existente.setDiaSemana(DiaSemana.MARTES);
        existente.setHoraInicio(LocalTime.of(9, 0));
        existente.setHoraFin(LocalTime.of(13, 0));
        existente.setActivo(true);
        existente.setCreatedAt(AppClock.now());
        horarioRepository.saveAndFlush(existente);

        boolean cruza = horarioRepository.existsOverlap(
                empleado.getId(), fecha, LocalTime.of(12, 0), LocalTime.of(14, 0));

        assertThat(cruza).isTrue();
        assertThat(horarioRepository.findByEmpleadoIdAndFecha(empleado.getId(), fecha)).hasSize(1);
    }
}
