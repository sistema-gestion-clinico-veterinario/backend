package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.repository.VentanaRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VentanaServiceAuditoriaTest {

    private static final String MODULO = "Catálogo de vistas";

    private final VentanaRepository repositorio = mock(VentanaRepository.class);
    private final AuditLogService auditoria = mock(AuditLogService.class);
    private final VentanaService servicio = new VentanaService(repositorio, auditoria);

    private Ventana ventana(String nombre, int orden, boolean activa) {
        Ventana ventana = new Ventana();
        ventana.setId(5);
        ventana.setCodigo("CLINICA");
        ventana.setNombre(nombre);
        ventana.setOrden(orden);
        ventana.setActivo(activa);
        return ventana;
    }

    private void devolverLoGuardado() {
        when(repositorio.save(any(Ventana.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registrarUnModuloDejaConstancia() {
        devolverLoGuardado();

        servicio.crear(ventana("Clínica", 4, true));

        verify(auditoria).log(eq("REGISTRAR_MODULO"), eq(MODULO), contains("CLINICA \"Clínica\""));
    }

    @Test
    void renombrarYReordenarUnModuloDejaConstanciaDeAntesYDespues() {
        devolverLoGuardado();
        when(repositorio.findById(5)).thenReturn(Optional.of(ventana("Clínica", 4, true)));

        servicio.actualizar(5, ventana("Atención clínica", 9, true));

        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(auditoria).log(eq("MODIFICAR_MODULO"), eq(MODULO), detalle.capture());
        assertThat(detalle.getValue())
                .contains("nombre \"Clínica\" → \"Atención clínica\"")
                .contains("posición 4 → 9");
    }

    @Test
    void guardarUnModuloSinCambiosNoGeneraConstancia() {
        devolverLoGuardado();
        when(repositorio.findById(5)).thenReturn(Optional.of(ventana("Clínica", 4, true)));

        servicio.actualizar(5, ventana("Clínica", 4, true));

        verify(auditoria, never()).log(anyString(), anyString(), anyString());
    }

    @Test
    void retirarUnModuloDejaConstanciaUnaSolaVez() {
        devolverLoGuardado();
        when(repositorio.findById(5)).thenReturn(Optional.of(ventana("Clínica", 4, true)));

        servicio.eliminar(5);
        servicio.eliminar(5);

        verify(auditoria, times(1)).log(eq("RETIRAR_MODULO"), eq(MODULO), contains("CLINICA"));
    }
}
