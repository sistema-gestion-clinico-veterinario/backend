package veterinaria.vargasvet.ers.historias;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import veterinaria.vargasvet.controller.ConsultaController;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.dto.request.CerrarConsultaRequest;
import veterinaria.vargasvet.dto.response.ConsultaResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.ConsultaService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsultaControllerApiValidationTest {

    private final ConsultaService consultaService = mock(ConsultaService.class);
    private final AccesoValidator accesoValidator = mock(AccesoValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void configureMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ConsultaController(consultaService, accesoValidator))
                .setValidator(validator)
                .build();
        when(consultaService.cerrarConsulta(eq(1L), any(CerrarConsultaRequest.class)))
                .thenReturn(new ConsultaResponse());
    }

    @Test
    @DisplayName("[CP-RF30-02] La API rechaza el cierre sin versión")
    void apiRechazaCierreSinVersion() throws Exception {
        CerrarConsultaRequest request = baseRequest();
        request.setVersion(null);

        mockMvc.perform(patch("/consultations/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[CP-RF30-03] La API rechaza peso con más de dos decimales")
    void apiRechazaPesoConMasDeDosDecimales() throws Exception {
        CerrarConsultaRequest request = baseRequest();
        request.setPesoEnConsulta(10.123);

        mockMvc.perform(patch("/consultations/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[CP-RF30-04] La API exige decisiones explícitas de vacunación y desparasitación")
    void apiRechazaDecisionesPreventivasAusentes() throws Exception {
        CerrarConsultaRequest request = baseRequest();
        request.setVacunacionAplicada(null);
        request.setDesparasitacionAplicada(null);

        mockMvc.perform(patch("/consultations/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());
    }

    private CerrarConsultaRequest baseRequest() {
        CerrarConsultaRequest request = new CerrarConsultaRequest();
        request.setVersion(0L);
        request.setTipoConsulta(TipoConsulta.PRIMERA_VEZ);
        request.setPesoEnConsulta(10.50);
        request.setVacunacionAplicada(false);
        request.setDesparasitacionAplicada(false);
        return request;
    }
}
