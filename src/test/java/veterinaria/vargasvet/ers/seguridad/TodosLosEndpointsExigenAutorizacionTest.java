package veterinaria.vargasvet.ers.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.controller.CitaController;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CitaService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * RNF-02. Ningún endpoint queda sin una regla de autorización explícita: o declara @PreAuthorize, o está en las listas
 * de {@link CatalogoEndpoints} con su razón. Un endpoint nuevo sin regla hace fallar el build hasta que se justifique.
 */
class TodosLosEndpointsExigenAutorizacionTest {

    /** Se protegen dentro del método con AccesoValidator; la segunda prueba lo demuestra para cada uno. */
    private static final Class<?> CONTROLADOR_IMPERATIVO = CitaController.class;

    @Test
    @DisplayName("[CP-RNF02-02] Todo endpoint declara su autorización o está justificado en la lista")
    void todoEndpointTieneAutorizacionOJustificacion() {
        List<CatalogoEndpoints.Endpoint> endpoints = CatalogoEndpoints.todos();
        Set<String> sinJustificar = new TreeSet<>();
        for (CatalogoEndpoints.Endpoint endpoint : endpoints) {
            if (endpoint.regla() != null) {
                assertThat(endpoint.regla()).as("%s tiene una regla vacía o abierta", endpoint.clave())
                        .isNotBlank().doesNotContain("permitAll");
            } else if (endpoint.controlador() != CONTROLADOR_IMPERATIVO && !endpoint.esPublico() && !endpoint.esAutoservicio()) {
                sinJustificar.add(endpoint.descripcion());
            }
        }

        assertThat(sinJustificar).as("Endpoints sin @PreAuthorize y sin justificar").isEmpty();
        Set<String> existentes = endpoints.stream().map(CatalogoEndpoints.Endpoint::clave).collect(Collectors.toSet());
        assertThat(existentes).as("Las listas de justificados no deben tener entradas que ya no existen")
                .containsAll(CatalogoEndpoints.PUBLICOS).containsAll(CatalogoEndpoints.AUTOSERVICIO);
    }

    @Test
    @DisplayName("[CP-RNF02-03] Sin permiso, ningún endpoint de citas llega al servicio")
    void sinPermisoElControladorDeCitasNuncaTocaElServicio() throws Exception {
        CitaService servicio = mock(CitaService.class);
        AuditLogService auditoria = mock(AuditLogService.class);
        AccesoValidator validador = mock(AccesoValidator.class);
        doThrow(new AccessDeniedException("Sin permiso")).when(validador).validarLeer(anyString());
        doThrow(new AccessDeniedException("Sin permiso")).when(validador).validarEscribir(anyString());
        doThrow(new AccessDeniedException("Sin permiso")).when(validador).validarModificar(anyString());
        doThrow(new AccessDeniedException("Sin permiso")).when(validador).validarEliminar(anyString());
        CitaController controlador = new CitaController(servicio, auditoria, validador);

        List<Method> manejadores = Arrays.stream(CitaController.class.getDeclaredMethods())
                .filter(m -> CatalogoEndpoints.verbo(m) != null).toList();
        assertThat(manejadores).hasSizeGreaterThanOrEqualTo(15);

        for (Method manejador : manejadores) {
            Throwable causa = null;
            try {
                manejador.invoke(controlador, argumentosPorDefecto(manejador));
            } catch (InvocationTargetException ex) {
                causa = ex.getCause();
            }
            assertThat(causa).as("%s debe rechazar sin permiso", manejador.getName()).isInstanceOf(AccessDeniedException.class);
        }
        verifyNoInteractions(servicio);
        verifyNoInteractions(auditoria);
    }

    private Object[] argumentosPorDefecto(Method metodo) {
        Parameter[] parametros = metodo.getParameters();
        Object[] valores = new Object[parametros.length];
        for (int i = 0; i < parametros.length; i++) {
            Class<?> tipo = parametros[i].getType();
            if (tipo == long.class || tipo == Long.class) valores[i] = 1L;
            else if (tipo == int.class || tipo == Integer.class) valores[i] = 1;
            else if (tipo == boolean.class) valores[i] = false;
            else valores[i] = null;
        }
        return valores;
    }
}
