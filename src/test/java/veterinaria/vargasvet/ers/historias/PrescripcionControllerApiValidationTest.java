package veterinaria.vargasvet.ers.historias;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import veterinaria.vargasvet.controller.PrescripcionController;
import veterinaria.vargasvet.dto.request.PrescripcionRequest;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.PrescripcionService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrescripcionControllerApiValidationTest {

    private final PrescripcionService prescripcionService = mock(PrescripcionService.class);
    private final AccesoValidator accesoValidator = mock(AccesoValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void configureMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PrescripcionController(prescripcionService, accesoValidator))
                .setValidator(validator)
                .build();
        when(prescripcionService.crear(eq(10L), any(PrescripcionRequest.class)))
                .thenReturn(new PrescripcionResumenResponse());
    }

    @Test
    @DisplayName("[CP-RF31-01] La API acepta una receta completa")
    void apiAceptaRecetaCompleta() throws Exception {
        mockMvc.perform(post("/prescriptions/consultation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(completeRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[CP-RF31-02] La API rechaza una receta sin duración")
    void apiRechazaRecetaSinDuracion() throws Exception {
        PrescripcionRequest request = completeRequest();
        request.setDuracionDias(null);

        mockMvc.perform(post("/prescriptions/consultation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[CP-RF31-02] La API rechaza una receta sin indicaciones")
    void apiRechazaRecetaSinIndicaciones() throws Exception {
        PrescripcionRequest request = completeRequest();
        request.setInstrucciones(null);

        mockMvc.perform(post("/prescriptions/consultation/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    private PrescripcionRequest completeRequest() {
        PrescripcionRequest request = new PrescripcionRequest();
        request.setMedicamento("Amoxicilina");
        request.setDosis("Una tableta");
        request.setFrecuencia("Cada doce horas");
        request.setDuracionDias(7);
        request.setViaAdministracion("Oral");
        request.setInstrucciones("Administrar después de los alimentos");
        request.setFechaInicio(LocalDate.now());
        return request;
    }
}
