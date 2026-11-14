package veterinaria.vargasvet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.ClinicalConcept;
import veterinaria.vargasvet.domain.entity.ClinicalTemplate;
import veterinaria.vargasvet.domain.entity.ClinicalTemplateField;
import veterinaria.vargasvet.domain.entity.ClinicalTemplateSection;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.ConsultationFieldAnswer;
import veterinaria.vargasvet.domain.enums.ClinicalTemplateStatus;
import veterinaria.vargasvet.domain.enums.ClinicalValueType;
import veterinaria.vargasvet.dto.request.ClinicalFieldAnswerRequest;
import veterinaria.vargasvet.dto.request.ClinicalTemplateDraftRequest;
import veterinaria.vargasvet.dto.response.ClinicalTemplateResponse;
import veterinaria.vargasvet.repository.ClinicalConceptRepository;
import veterinaria.vargasvet.repository.ClinicalTemplateRepository;
import veterinaria.vargasvet.repository.ClinicalTemplateSectionRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultationFieldAnswerRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalTemplateGuardadoTest {

    private static final Integer EMPRESA = 7;
    private static final Long BORRADOR = 50L;
    private static final Long CONSULTA = 90L;

    @Mock private ClinicalConceptRepository conceptRepository;
    @Mock private ClinicalTemplateRepository templateRepository;
    @Mock private ClinicalTemplateSectionRepository sectionRepository;
    @Mock private ConsultationFieldAnswerRepository answerRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ServiciosVeterinariosRepository serviceRepository;
    @Mock private AuditLogService auditLogService;

    private ClinicalTemplateService service;
    private Company empresa;

    @BeforeEach
    void preparar() {
        service = new ClinicalTemplateService(conceptRepository, templateRepository, sectionRepository, answerRepository,
                companyRepository, serviceRepository, new ObjectMapper(), auditLogService);
        empresa = new Company();
        empresa.setId(EMPRESA);
        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "admin@clinica.test", "", autoridades, EMPRESA);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null, autoridades));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    // ---------- guardar el borrador ----------

    @Test
    void losConceptosDeTodoElBorradorSeBuscanEnUnaSolaConsultaSinImportarCuantosCamposTenga() {
        borradorVacio();
        ClinicalConcept peso = concepto(1L, "vital.weight");
        ClinicalConcept temperatura = concepto(2L, "vital.temperature");
        when(conceptRepository.findAccessibleByIds(Set.of(1L, 2L), EMPRESA)).thenReturn(List.of(peso, temperatura));
        ClinicalTemplateDraftRequest solicitud = solicitud(
                seccion("Signos vitales", 0, campo(1L, "Peso", 0), campo(2L, "Temperatura", 1)),
                seccion("Evaluación", 1, campo(1L, "Peso otra vez", 0), campo(2L, "Temperatura otra vez", 1)));

        service.updateDraft(BORRADOR, solicitud);

        verify(conceptRepository, times(1)).findAccessibleByIds(anyCollection(), any());
    }

    @Test
    void armaLasSeccionesYLosCamposConLosDatosPedidosYSusConceptos() {
        ClinicalTemplate borrador = borradorVacio();
        ClinicalConcept peso = concepto(1L, "vital.weight");
        ClinicalConcept temperatura = concepto(2L, "vital.temperature");
        when(conceptRepository.findAccessibleByIds(Set.of(1L, 2L), EMPRESA)).thenReturn(List.of(temperatura, peso));
        ClinicalTemplateDraftRequest solicitud = solicitud(
                seccion("  Signos vitales ", 3, campo(1L, " Peso ", 0), campo(2L, "Temperatura", 1)));

        ClinicalTemplateResponse respuesta = service.updateDraft(BORRADOR, solicitud);

        assertThat(borrador.getName()).isEqualTo("Plantilla editada");
        assertThat(borrador.getSections()).hasSize(1);
        ClinicalTemplateSection seccion = borrador.getSections().getFirst();
        assertThat(seccion.getTitle()).isEqualTo("Signos vitales");
        assertThat(seccion.getDisplayOrder()).isEqualTo(3);
        assertThat(seccion.getFields()).extracting(ClinicalTemplateField::getConcept).containsExactly(peso, temperatura);
        assertThat(seccion.getFields()).extracting(ClinicalTemplateField::getVisibleLabel).containsExactly("Peso", "Temperatura");
        assertThat(respuesta.getSections()).hasSize(1);
        assertThat(respuesta.getSections().getFirst().getFields()).extracting(ClinicalTemplateResponse.Field::getConceptCode)
                .containsExactly("vital.weight", "vital.temperature");
        verify(auditLogService).log(any(), any(), any(), any());
    }

    @Test
    void unConceptoQueNoPerteneceALaEmpresaSeRechazaYNoSeGuardaNada() {
        borradorVacio();
        when(conceptRepository.findAccessibleByIds(Set.of(1L, 99L), EMPRESA)).thenReturn(List.of(concepto(1L, "vital.weight")));
        ClinicalTemplateDraftRequest solicitud = solicitud(
                seccion("Signos vitales", 0, campo(1L, "Peso", 0), campo(99L, "Ajeno", 1)));

        assertThatThrownBy(() -> service.updateDraft(BORRADOR, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El campo clínico seleccionado no pertenece a la empresa");

        verify(templateRepository, never()).saveAndFlush(any());
    }

    @Test
    void unConceptoRepetidoEnLaMismaSeccionSeRechazaAntesDeValidarLosSiguientes() {
        borradorVacio();
        when(conceptRepository.findAccessibleByIds(Set.of(1L), EMPRESA)).thenReturn(List.of(concepto(1L, "vital.weight")));
        ClinicalTemplateDraftRequest solicitud = solicitud(
                seccion("Signos vitales", 0, campo(1L, "Peso", 0), campo(1L, "Peso repetido", 1)));

        assertThatThrownBy(() -> service.updateDraft(BORRADOR, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Un concepto no puede repetirse dentro de la misma sección");
    }

    @Test
    void losOrdenesRepetidosSeRechazanSinConsultarConceptos() {
        borradorVacio();
        ClinicalTemplateDraftRequest solicitud = solicitud(
                seccion("Uno", 0, campo(1L, "Peso", 0)),
                seccion("Dos", 0, campo(2L, "Temperatura", 0)));

        assertThatThrownBy(() -> service.updateDraft(BORRADOR, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Las secciones deben tener órdenes únicos y no negativos");

        verify(conceptRepository, never()).findAccessibleByIds(any(), any());
    }

    @Test
    void unBorradorSinCamposNoConsultaConceptos() {
        ClinicalTemplate borrador = borradorVacio();
        ClinicalTemplateDraftRequest solicitud = solicitud(seccion("Vacía", 0));

        service.updateDraft(BORRADOR, solicitud);

        assertThat(borrador.getSections()).hasSize(1);
        verify(conceptRepository, never()).findAccessibleByIds(any(), any());
    }

    @Test
    void unaPlantillaPublicadaNoSePuedeEditar() {
        ClinicalTemplate publicada = borradorVacio();
        publicada.setStatus(ClinicalTemplateStatus.PUBLISHED);

        assertThatThrownBy(() -> service.updateDraft(BORRADOR, solicitud(seccion("Uno", 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Una plantilla publicada o archivada no puede modificarse");
    }

    // ---------- guardar las respuestas de una consulta ----------

    @Test
    void lasRespuestasYaGuardadasSeLeenUnaVezYNoUnaConsultaPorCampo() {
        Plantilla plantilla = plantilla(3, false);
        Consulta consulta = consulta(plantilla.plantilla());
        when(answerRepository.findAllByConsultationId(CONSULTA)).thenReturn(List.of());

        service.saveAnswers(consulta, respuestasPara(plantilla), false);

        verify(answerRepository, times(1)).findAllByConsultationId(CONSULTA);
    }

    @Test
    void creaLasRespuestasNuevasYActualizaLasQueYaExistian() {
        Plantilla plantilla = plantilla(2, false);
        Consulta consulta = consulta(plantilla.plantilla());
        ConsultationFieldAnswer existente = respuestaGuardada(plantilla.campos().get(0), "valor anterior");
        when(answerRepository.findAllByConsultationId(CONSULTA)).thenReturn(List.of(existente));

        service.saveAnswers(consulta, respuestasPara(plantilla), false);

        ArgumentCaptor<ConsultationFieldAnswer> guardadas = ArgumentCaptor.forClass(ConsultationFieldAnswer.class);
        verify(answerRepository, times(2)).save(guardadas.capture());
        assertThat(guardadas.getAllValues()).contains(existente);
        assertThat(existente.getValueText()).isEqualTo("respuesta " + plantilla.campos().get(0).getId());
        ConsultationFieldAnswer nueva = guardadas.getAllValues().stream().filter(r -> r != existente).findFirst().orElseThrow();
        assertThat(nueva.getTemplateField()).isSameAs(plantilla.campos().get(1));
        assertThat(nueva.getConsultation()).isSameAs(consulta);
        assertThat(nueva.getValueText()).isEqualTo("respuesta " + plantilla.campos().get(1).getId());
    }

    @Test
    void alActualizarUnaRespuestaSeLimpiaElValorAnteriorAntesDeEscribirElNuevo() {
        Plantilla plantilla = plantilla(1, false);
        Consulta consulta = consulta(plantilla.plantilla());
        ConsultationFieldAnswer existente = respuestaGuardada(plantilla.campos().getFirst(), "valor anterior");
        existente.setValueJson("[\"restos\"]");
        when(answerRepository.findAllByConsultationId(CONSULTA)).thenReturn(List.of(existente));

        service.saveAnswers(consulta, respuestasPara(plantilla), false);

        assertThat(existente.getValueJson()).isNull();
        assertThat(existente.getValueText()).startsWith("respuesta ");
    }

    @Test
    void unaRespuestaParaUnCampoQueNoEsDeLaFichaSeRechaza() {
        Plantilla plantilla = plantilla(1, false);
        Consulta consulta = consulta(plantilla.plantilla());
        when(answerRepository.findAllByConsultationId(CONSULTA)).thenReturn(List.of());
        ClinicalFieldAnswerRequest ajena = respuesta(9999L, "valor");

        assertThatThrownBy(() -> service.saveAnswers(consulta, List.of(ajena), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La respuesta contiene un campo que no pertenece a la ficha de esta consulta");

        verify(answerRepository, never()).save(any());
    }

    @Test
    void alCerrarExigeLosCamposObligatoriosQueNoTienenRespuestaNiNueva() {
        Plantilla plantilla = plantilla(2, true);
        Consulta consulta = consulta(plantilla.plantilla());
        when(answerRepository.findAllByConsultationId(CONSULTA)).thenReturn(List.of());
        List<ClinicalFieldAnswerRequest> soloLaPrimera = List.of(respuesta(plantilla.campos().get(0).getId(), "valor"));

        assertThatThrownBy(() -> service.saveAnswers(consulta, soloLaPrimera, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("es obligatorio para cerrar la consulta");

        verify(answerRepository, never()).save(any());
    }

    @Test
    void alCerrarUnObligatorioYaGuardadoCuentaComoRespondido() {
        Plantilla plantilla = plantilla(2, true);
        Consulta consulta = consulta(plantilla.plantilla());
        when(answerRepository.findAllByConsultationId(CONSULTA))
                .thenReturn(List.of(respuestaGuardada(plantilla.campos().get(1), "ya respondido")));
        List<ClinicalFieldAnswerRequest> soloLaPrimera = List.of(respuesta(plantilla.campos().get(0).getId(), "valor"));

        service.saveAnswers(consulta, soloLaPrimera, true);

        verify(answerRepository, times(1)).save(any());
    }

    @Test
    void losCamposNucleoNoSeGuardanComoRespuestaYNoSeExigenAlCerrar() {
        Plantilla plantilla = plantilla(2, true);
        plantilla.campos().get(0).getConcept().setValidationConfig("{\"coreField\":\"consultation.reason\"}");
        Consulta consulta = consulta(plantilla.plantilla());
        when(answerRepository.findAllByConsultationId(CONSULTA)).thenReturn(List.of());
        List<ClinicalFieldAnswerRequest> respuestas = List.of(respuesta(plantilla.campos().get(1).getId(), "valor"));

        service.saveAnswers(consulta, respuestas, true);

        ArgumentCaptor<ConsultationFieldAnswer> guardada = ArgumentCaptor.forClass(ConsultationFieldAnswer.class);
        verify(answerRepository, times(1)).save(guardada.capture());
        assertThat(guardada.getValue().getTemplateField()).isSameAs(plantilla.campos().get(1));
    }

    @Test
    void unValorQueNoCoincideConElTipoDelConceptoSeRechaza() {
        Plantilla plantilla = plantilla(1, false);
        plantilla.campos().getFirst().getConcept().setValueType(ClinicalValueType.INTEGER);
        Consulta consulta = consulta(plantilla.plantilla());
        when(answerRepository.findAllByConsultationId(CONSULTA)).thenReturn(List.of());

        assertThatThrownBy(() -> service.saveAnswers(consulta, List.of(respuesta(plantilla.campos().getFirst().getId(), "no es número")), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide con su tipo de dato");
    }

    @Test
    void sinFichaAsignadaNoHaceNada() {
        Consulta sinFicha = new Consulta();
        sinFicha.setId(CONSULTA);

        service.saveAnswers(sinFicha, List.of(respuesta(1L, "valor")), false);

        verify(answerRepository, never()).findAllByConsultationId(any());
        verify(answerRepository, never()).save(any());
    }

    // ---------- datos de prueba ----------

    private ClinicalTemplate borradorVacio() {
        ClinicalTemplate borrador = new ClinicalTemplate();
        borrador.setId(BORRADOR);
        borrador.setCompany(empresa);
        borrador.setStatus(ClinicalTemplateStatus.DRAFT);
        borrador.setVersion(1);
        borrador.setTemplateKey("PLANTILLA");
        when(templateRepository.findByIdAndCompanyId(BORRADOR, EMPRESA)).thenReturn(Optional.of(borrador));
        lenientGuardado();
        return borrador;
    }

    private void lenientGuardado() {
        org.mockito.Mockito.lenient().when(templateRepository.saveAndFlush(any(ClinicalTemplate.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private ClinicalConcept concepto(Long id, String codigo) {
        ClinicalConcept concepto = new ClinicalConcept();
        concepto.setId(id);
        concepto.setCode(codigo);
        concepto.setBaseName(codigo);
        concepto.setValueType(ClinicalValueType.TEXT_SHORT);
        concepto.setValidationConfig("{}");
        return concepto;
    }

    private ClinicalTemplateDraftRequest solicitud(ClinicalTemplateDraftRequest.Section... secciones) {
        ClinicalTemplateDraftRequest solicitud = new ClinicalTemplateDraftRequest();
        solicitud.setName("Plantilla editada");
        solicitud.setSections(new ArrayList<>(List.of(secciones)));
        return solicitud;
    }

    private ClinicalTemplateDraftRequest.Section seccion(String titulo, int orden, ClinicalTemplateDraftRequest.Field... campos) {
        ClinicalTemplateDraftRequest.Section seccion = new ClinicalTemplateDraftRequest.Section();
        seccion.setTitle(titulo);
        seccion.setOrder(orden);
        seccion.setFields(new ArrayList<>(List.of(campos)));
        return seccion;
    }

    private ClinicalTemplateDraftRequest.Field campo(Long conceptoId, String etiqueta, int orden) {
        ClinicalTemplateDraftRequest.Field campo = new ClinicalTemplateDraftRequest.Field();
        campo.setConceptId(conceptoId);
        campo.setLabel(etiqueta);
        campo.setOrder(orden);
        campo.setVisible(true);
        return campo;
    }

    private record Plantilla(ClinicalTemplate plantilla, List<ClinicalTemplateField> campos) { }

    private Plantilla plantilla(int cantidadDeCampos, boolean obligatorios) {
        ClinicalTemplate plantilla = new ClinicalTemplate();
        plantilla.setId(60L);
        plantilla.setCompany(empresa);
        ClinicalTemplateSection seccion = new ClinicalTemplateSection();
        seccion.setTemplate(plantilla);
        List<ClinicalTemplateField> campos = new ArrayList<>();
        for (int i = 0; i < cantidadDeCampos; i++) {
            ClinicalTemplateField campo = new ClinicalTemplateField();
            campo.setId(100L + i);
            campo.setSection(seccion);
            campo.setConcept(concepto(10L + i, "campo." + i));
            campo.setVisibleLabel("Campo " + i);
            campo.setRequired(obligatorios);
            seccion.getFields().add(campo);
            campos.add(campo);
        }
        plantilla.getSections().add(seccion);
        return new Plantilla(plantilla, campos);
    }

    private Consulta consulta(ClinicalTemplate plantilla) {
        Consulta consulta = new Consulta();
        consulta.setId(CONSULTA);
        consulta.setClinicalTemplate(plantilla);
        return consulta;
    }

    private List<ClinicalFieldAnswerRequest> respuestasPara(Plantilla plantilla) {
        return plantilla.campos().stream().map(campo -> respuesta(campo.getId(), "respuesta " + campo.getId())).toList();
    }

    private ClinicalFieldAnswerRequest respuesta(Long campoId, String valor) {
        ClinicalFieldAnswerRequest respuesta = new ClinicalFieldAnswerRequest();
        respuesta.setFieldId(campoId);
        respuesta.setValue(valor);
        return respuesta;
    }

    private ConsultationFieldAnswer respuestaGuardada(ClinicalTemplateField campo, String valor) {
        ConsultationFieldAnswer respuesta = new ConsultationFieldAnswer();
        respuesta.setTemplateField(campo);
        respuesta.setValueText(valor);
        return respuesta;
    }
}
