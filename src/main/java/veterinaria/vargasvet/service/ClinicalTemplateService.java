package veterinaria.vargasvet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.*;
import veterinaria.vargasvet.dto.request.ClinicalConceptRequest;
import veterinaria.vargasvet.dto.request.ClinicalFieldAnswerRequest;
import veterinaria.vargasvet.dto.request.ClinicalTemplateDraftRequest;
import veterinaria.vargasvet.dto.request.ClinicalTemplateCreateRequest;
import veterinaria.vargasvet.dto.response.*;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClinicalTemplateService {
    public static final String GENERAL_TEMPLATE_KEY = "GENERAL_CONSULTATION";

    private final ClinicalConceptRepository conceptRepository;
    private final ClinicalTemplateRepository templateRepository;
    private final ClinicalTemplateSectionRepository sectionRepository;
    private final ConsultationFieldAnswerRepository answerRepository;
    private final CompanyRepository companyRepository;
    private final ServiciosVeterinariosRepository serviceRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ClinicalTemplateResponse current(String species, Long serviceId) {
        ClinicalTemplate template = resolvePublishedTemplate(requiredCompanyId(), normalizeSpecies(species), serviceId);
        return toResponse(template, normalizeSpecies(species));
    }

    @Transactional(readOnly = true)
    public ClinicalTemplateResponse current(String species) {
        return current(species, null);
    }

    @Transactional(readOnly = true)
    public ClinicalTemplateResponse get(Long id) {
        ClinicalTemplate template = templateRepository.findByIdAndCompanyId(id, requiredCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla clínica no encontrada"));
        return toResponse(template, null);
    }

    @Transactional(readOnly = true)
    public List<ClinicalTemplateSummaryResponse> list() {
        return templateRepository.findByCompanyIdOrderByTemplateKeyAscVersionDesc(requiredCompanyId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalConceptResponse> concepts() {
        return conceptRepository.findAvailableForCompany(requiredCompanyId()).stream()
                .map(this::toConceptResponse)
                .toList();
    }

    @Transactional
    public ClinicalConceptResponse createConcept(ClinicalConceptRequest request) {
        Integer companyId = requiredCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        String localCode = buildLocalCode(request.getCode(), request.getName());
        if (conceptRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, localCode)) {
            throw new IllegalArgumentException("Ya existe un campo clínico con ese código en la empresa");
        }
        validateJsonObject(request.getValidationConfig(), "La configuración de validación no es válida");

        ClinicalConcept concept = new ClinicalConcept();
        concept.setCompany(company);
        concept.setCode(localCode);
        concept.setBaseName(request.getName().trim());
        concept.setDescription(trimToNull(request.getDescription()));
        concept.setValueType(request.getValueType());
        concept.setUnit(trimToNull(request.getUnit()));
        concept.setOrigin(ClinicalConceptOrigin.COMPANY);
        concept.setReportable(request.isReportable());
        concept.setActive(true);
        concept.setValidationConfig(defaultJson(request.getValidationConfig()));
        concept.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        ClinicalConcept saved = conceptRepository.save(concept);

        auditLogService.log(companyId, "CREAR_CONCEPTO_CLINICO", "Historias Clínicas",
                "Se creó el campo clínico configurable " + saved.getBaseName());
        return toConceptResponse(saved);
    }

    @Transactional
    public ClinicalTemplateResponse createDraft() {
        return createDraft(publishedEntity(requiredCompanyId()).getId());
    }

    @Transactional
    public ClinicalTemplateResponse createDraft(Long sourceId) {
        Integer companyId = requiredCompanyId();
        ClinicalTemplate published = templateRepository.findByIdAndCompanyId(sourceId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha clínica no encontrada"));
        if (published.getStatus() == ClinicalTemplateStatus.DRAFT) return toResponse(published, null);
        Optional<ClinicalTemplate> existingDraft = templateRepository
                .findFirstByCompanyIdAndTemplateKeyAndStatus(companyId, published.getTemplateKey(), ClinicalTemplateStatus.DRAFT);
        if (existingDraft.isPresent()) return toResponse(existingDraft.get(), null);

        ClinicalTemplate draft = new ClinicalTemplate();
        draft.setCompany(published.getCompany());
        draft.setTemplateKey(published.getTemplateKey());
        draft.setName(published.getName());
        copyTemplateMetadata(published, draft);
        draft.setVersion(templateRepository.countByCompanyIdAndTemplateKey(companyId, published.getTemplateKey()) + 1);
        draft.setStatus(ClinicalTemplateStatus.DRAFT);
        draft.setBasedOn(published);
        draft.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        copySections(published, draft);
        ClinicalTemplate saved = templateRepository.save(draft);
        return toResponse(saved, null);
    }

    @Transactional
    public ClinicalTemplateResponse createTemplate(ClinicalTemplateCreateRequest request) {
        Integer companyId = requiredCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        ClinicalTemplate source = request.getCopyFromTemplateId() == null
                ? publishedEntity(companyId)
                : templateRepository.findByIdAndCompanyId(request.getCopyFromTemplateId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ficha de origen no encontrada"));

        ClinicalTemplate draft = new ClinicalTemplate();
        draft.setCompany(company);
        draft.setTemplateKey(uniqueTemplateKey(companyId, request.getName()));
        draft.setName(request.getName().trim());
        draft.setDescription(trimToNull(request.getDescription()));
        draft.setApplicableSpecies(joinSpecies(request.getSpecies()));
        draft.setDefaultTemplate(false);
        draft.setActive(true);
        draft.setVersion(1);
        draft.setStatus(ClinicalTemplateStatus.DRAFT);
        draft.setBasedOn(source);
        draft.setCreatedBy(SecurityUtils.getCurrentUserEmail());
        draft.getServices().addAll(resolveServices(companyId, request.getServiceIds()));
        copySections(source, draft);
        ClinicalTemplate saved = templateRepository.saveAndFlush(draft);
        auditLogService.log(companyId, "CREAR_FICHA_CLINICA", "Historias Clínicas",
                "Se creó la ficha clínica " + saved.getName());
        return toResponse(saved, null);
    }

    @Transactional
    public ClinicalTemplateResponse makeDefault(Long id) {
        Integer companyId = requiredCompanyId();
        ClinicalTemplate selected = templateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha clínica no encontrada"));
        if (selected.getStatus() != ClinicalTemplateStatus.PUBLISHED || !selected.isActive()) {
            throw new IllegalArgumentException("Solo una ficha publicada y activa puede ser predeterminada");
        }
        List<ClinicalTemplate> templates = templateRepository.findByCompanyIdOrderByTemplateKeyAscVersionDesc(companyId);
        templates.forEach(template -> template.setDefaultTemplate(false));
        templateRepository.saveAllAndFlush(templates);
        templates.stream()
                .filter(template -> Objects.equals(template.getTemplateKey(), selected.getTemplateKey()))
                .forEach(template -> template.setDefaultTemplate(true));
        templateRepository.saveAllAndFlush(templates);
        auditLogService.log(companyId, "DEFINIR_FICHA_PREDETERMINADA", "Historias Clínicas",
                "Se estableció como predeterminada la ficha " + selected.getName());
        return toResponse(selected, null);
    }

    @Transactional
    public ClinicalTemplateResponse updateDraft(Long id, ClinicalTemplateDraftRequest request) {
        Integer companyId = requiredCompanyId();
        ClinicalTemplate draft = templateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla clínica no encontrada"));
        requireDraft(draft);
        validateUniqueOrders(request);

        draft.setName(request.getName().trim());
        draft.setDescription(trimToNull(request.getDescription()));
        draft.setApplicableSpecies(joinSpecies(request.getSpecies()));
        draft.getServices().clear();
        draft.getServices().addAll(resolveServices(companyId, request.getServiceIds()));
        draft.getSections().clear();
        for (ClinicalTemplateDraftRequest.Section sectionRequest : request.getSections()) {
            ClinicalTemplateSection section = new ClinicalTemplateSection();
            section.setTemplate(draft);
            section.setTitle(sectionRequest.getTitle().trim());
            section.setDisplayOrder(sectionRequest.getOrder());

            Set<Long> conceptsInSection = new HashSet<>();
            for (ClinicalTemplateDraftRequest.Field fieldRequest : sectionRequest.getFields()) {
                if (!conceptsInSection.add(fieldRequest.getConceptId())) {
                    throw new IllegalArgumentException("Un concepto no puede repetirse dentro de la misma sección");
                }
                ClinicalConcept concept = conceptRepository.findAccessibleById(fieldRequest.getConceptId(), companyId)
                        .orElseThrow(() -> new IllegalArgumentException("El campo clínico seleccionado no pertenece a la empresa"));
                validateJsonObject(fieldRequest.getVisibilityRules(), "Las reglas de visibilidad no son válidas");
                ClinicalTemplateField field = new ClinicalTemplateField();
                field.setSection(section);
                field.setConcept(concept);
                field.setVisibleLabel(fieldRequest.getLabel().trim());
                field.setHelpText(trimToNull(fieldRequest.getHelpText()));
                field.setRequired(fieldRequest.isRequired());
                field.setVisible(fieldRequest.isVisible());
                field.setDisplayOrder(fieldRequest.getOrder());
                field.setApplicableSpecies(joinSpecies(fieldRequest.getSpecies()));
                field.setVisibilityRules(defaultJson(fieldRequest.getVisibilityRules()));
                section.getFields().add(field);
            }
            draft.getSections().add(section);
        }
        ClinicalTemplate saved = templateRepository.saveAndFlush(draft);
        auditLogService.log(companyId, "EDITAR_PLANTILLA_CLINICA", "Historias Clínicas",
                "Se actualizó el borrador de la ficha clínica versión " + saved.getVersion());
        return toResponse(saved, null);
    }

    @Transactional
    public ClinicalTemplateResponse publish(Long id) {
        Integer companyId = requiredCompanyId();
        ClinicalTemplate draft = templateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla clínica no encontrada"));
        requireDraft(draft);
        if (draft.getSections().isEmpty() || draft.getSections().stream().allMatch(s -> s.getFields().isEmpty())) {
            throw new IllegalArgumentException("No se puede publicar una plantilla sin campos");
        }

        templateRepository.findFirstByCompanyIdAndTemplateKeyAndStatus(
                        companyId, draft.getTemplateKey(), ClinicalTemplateStatus.PUBLISHED)
                .ifPresent(current -> {
                    current.setStatus(ClinicalTemplateStatus.ARCHIVED);
                    templateRepository.saveAndFlush(current);
                });
        draft.setStatus(ClinicalTemplateStatus.PUBLISHED);
        draft.setPublishedAt(veterinaria.vargasvet.util.AppClock.now());
        draft.setPublishedBy(SecurityUtils.getCurrentUserEmail());
        ClinicalTemplate saved = templateRepository.saveAndFlush(draft);
        auditLogService.log(companyId, "PUBLICAR_PLANTILLA_CLINICA", "Historias Clínicas",
                "Se publicó la ficha clínica " + saved.getName() + " versión " + saved.getVersion());
        return toResponse(saved, null);
    }

    @Transactional
    public void provisionDefault(Company company) {
        if (templateRepository.countByCompanyIdAndTemplateKey(company.getId(), GENERAL_TEMPLATE_KEY) > 0) return;

        ClinicalTemplate template = new ClinicalTemplate();
        template.setCompany(company);
        template.setTemplateKey(GENERAL_TEMPLATE_KEY);
        template.setName("Ficha clínica general");
        template.setDescription("Ficha general utilizada cuando no existe una configuración más específica.");
        template.setDefaultTemplate(true);
        template.setActive(true);
        template.setVersion(1);
        template.setStatus(ClinicalTemplateStatus.PUBLISHED);
        template.setPublishedAt(veterinaria.vargasvet.util.AppClock.now());
        template.setPublishedBy("SYSTEM");
        template.setCreatedBy("SYSTEM");

        addDefaultSection(template, "Motivo y antecedentes", 0, List.of(
                defaultField("consultation.reason", "Motivo de consulta", true, 0),
                defaultField("consultation.anamnesis", "Anamnesis", true, 1)));
        addDefaultSection(template, "Signos vitales", 1, List.of(
                defaultField("vital.weight", "Peso", true, 0),
                defaultField("vital.temperature", "Temperatura", false, 1),
                defaultField("vital.heart_rate", "Frecuencia cardíaca", false, 2),
                defaultField("vital.respiratory_rate", "Frecuencia respiratoria", false, 3)));
        addDefaultSection(template, "Evaluación clínica", 2, List.of(
                defaultField("exam.mucosa", "Mucosas", false, 0),
                defaultField("exam.skin_turgor", "Turgencia de piel", false, 1),
                defaultField("consultation.physical_exam", "Examen físico", false, 2),
                defaultField("consultation.observations", "Observaciones", false, 3)));
        templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public ClinicalTemplate publishedEntity(Integer companyId) {
        return templateRepository.findByCompanyIdAndStatusOrderByDefaultTemplateDescNameAsc(
                        companyId, ClinicalTemplateStatus.PUBLISHED).stream()
                .filter(ClinicalTemplate::isActive)
                .filter(ClinicalTemplate::isDefaultTemplate)
                .findFirst()
                .orElseGet(() -> templateRepository.findFirstByCompanyIdAndTemplateKeyAndStatus(
                                companyId, GENERAL_TEMPLATE_KEY, ClinicalTemplateStatus.PUBLISHED)
                        .orElseThrow(() -> new ResourceNotFoundException("La empresa no tiene una ficha clínica publicada")));
    }

    public void assignPublishedTemplate(Consulta consultation, Integer companyId) {
        if (consultation.getClinicalTemplate() == null) {
            String species = consultation.getHistoriaClinica() == null || consultation.getHistoriaClinica().getMascota() == null
                    || consultation.getHistoriaClinica().getMascota().getEspecie() == null
                    ? null : consultation.getHistoriaClinica().getMascota().getEspecie().name();
            Long serviceId = consultation.getCita() == null || consultation.getCita().getServicio() == null
                    ? null : consultation.getCita().getServicio().getId();
            consultation.setClinicalTemplate(resolvePublishedTemplate(companyId, normalizeSpecies(species), serviceId));
        }
    }

    @Transactional
    public void saveAnswers(Consulta consultation, List<ClinicalFieldAnswerRequest> requests, boolean closing) {
        ClinicalTemplate template = consultation.getClinicalTemplate();
        if (template == null) return;
        Integer companyId = template.getCompany().getId();
        Map<Long, ClinicalTemplateField> fields = template.getSections().stream()
                .flatMap(section -> section.getFields().stream())
                .collect(Collectors.toMap(ClinicalTemplateField::getId, field -> field));
        Map<Long, ClinicalFieldAnswerRequest> incoming = requests == null ? Map.of() : requests.stream()
                .collect(Collectors.toMap(ClinicalFieldAnswerRequest::getFieldId, value -> value, (first, ignored) -> first));
        Map<Long, ConsultationFieldAnswer> savedAnswers = answerRepository.findAllByConsultationId(consultation.getId())
                .stream()
                .collect(Collectors.toMap(answer -> answer.getTemplateField().getId(), answer -> answer));

        for (Long fieldId : incoming.keySet()) {
            if (!fields.containsKey(fieldId)) {
                throw new IllegalArgumentException("La respuesta contiene un campo que no pertenece a la ficha de esta consulta");
            }
        }

        if (closing) {
            for (ClinicalTemplateField field : fields.values()) {
                if (!field.isRequired() || isCoreField(field)) continue;
                ClinicalFieldAnswerRequest answer = incoming.get(field.getId());
                String effectiveValue = answer != null
                        ? answer.getValue()
                        : Optional.ofNullable(savedAnswers.get(field.getId())).map(this::readTypedValue).orElse(null);
                if (effectiveValue == null || effectiveValue.isBlank()) {
                    throw new IllegalArgumentException("El campo " + field.getVisibleLabel() + " es obligatorio para cerrar la consulta");
                }
            }
        }

        for (ClinicalFieldAnswerRequest request : incoming.values()) {
            ClinicalTemplateField field = fields.get(request.getFieldId());
            if (isCoreField(field)) continue;
            ConsultationFieldAnswer answer = answerRepository
                    .findByConsultationIdAndTemplateFieldId(consultation.getId(), field.getId())
                    .orElseGet(ConsultationFieldAnswer::new);
            answer.setConsultation(consultation);
            answer.setTemplateField(field);
            clearValue(answer);
            writeTypedValue(answer, field.getConcept(), request.getValue());
            answer.setRecordedBy(SecurityUtils.getCurrentUserEmail());
            answerRepository.save(answer);
        }
    }

    @Transactional(readOnly = true)
    public List<ClinicalFieldAnswerResponse> answers(Long consultationId) {
        return answerRepository.findAllByConsultationId(consultationId).stream()
                .map(answer -> ClinicalFieldAnswerResponse.builder()
                        .fieldId(answer.getTemplateField().getId())
                        .conceptCode(answer.getTemplateField().getConcept().getCode())
                        .label(answer.getTemplateField().getVisibleLabel())
                        .value(readTypedValue(answer))
                        .build())
                .toList();
    }

    private void writeTypedValue(ConsultationFieldAnswer answer, ClinicalConcept concept, String raw) {
        if (raw == null || raw.isBlank()) return;
        String value = raw.trim();
        try {
            switch (concept.getValueType()) {
                case INTEGER -> answer.setValueNumber(new BigDecimal(Integer.parseInt(value)));
                case DECIMAL, SCALE -> answer.setValueNumber(new BigDecimal(value));
                case BOOLEAN -> {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("El campo " + concept.getBaseName() + " requiere una respuesta sí/no");
                    }
                    answer.setValueBoolean(Boolean.parseBoolean(value));
                }
                case DATE -> answer.setValueDate(LocalDate.parse(value));
                case SELECT_MULTIPLE -> {
                    JsonNode node = objectMapper.readTree(value);
                    if (!node.isArray()) throw new IllegalArgumentException("La selección múltiple debe enviarse como una lista");
                    answer.setValueJson(value);
                }
                case DISPLAY -> throw new IllegalArgumentException("Un campo informativo no admite respuestas");
                default -> answer.setValueText(value);
            }
        } catch (NumberFormatException | java.time.format.DateTimeParseException | JsonProcessingException ex) {
            throw new IllegalArgumentException("El valor de " + concept.getBaseName() + " no coincide con su tipo de dato");
        }
    }

    private String readTypedValue(ConsultationFieldAnswer answer) {
        if (answer.getValueText() != null) return answer.getValueText();
        if (answer.getValueNumber() != null) return answer.getValueNumber().stripTrailingZeros().toPlainString();
        if (answer.getValueBoolean() != null) return answer.getValueBoolean().toString();
        if (answer.getValueDate() != null) return answer.getValueDate().toString();
        return answer.getValueJson();
    }

    private void clearValue(ConsultationFieldAnswer answer) {
        answer.setValueText(null);
        answer.setValueNumber(null);
        answer.setValueBoolean(null);
        answer.setValueDate(null);
        answer.setValueJson(null);
    }

    private boolean isCoreField(ClinicalTemplateField field) {
        try {
            return objectMapper.readTree(field.getConcept().getValidationConfig()).hasNonNull("coreField");
        } catch (JsonProcessingException ex) {
            return false;
        }
    }

    private ClinicalTemplateResponse toResponse(ClinicalTemplate template, String species) {
        List<ClinicalTemplateResponse.Section> sections = template.getSections().stream()
                .map(section -> ClinicalTemplateResponse.Section.builder()
                        .id(section.getId())
                        .title(section.getTitle())
                        .order(section.getDisplayOrder())
                        .fields(section.getFields().stream()
                                .filter(ClinicalTemplateField::isVisible)
                                .filter(field -> appliesToSpecies(field, species))
                                .map(this::toFieldResponse)
                                .toList())
                        .build())
                .filter(section -> species == null || !section.getFields().isEmpty())
                .toList();
        return ClinicalTemplateResponse.builder()
                .id(template.getId())
                .templateKey(template.getTemplateKey())
                .name(template.getName())
                .version(template.getVersion())
                .status(template.getStatus())
                .publishedAt(template.getPublishedAt())
                .description(template.getDescription())
                .defaultTemplate(template.isDefaultTemplate())
                .active(template.isActive())
                .species(splitSpecies(template.getApplicableSpecies()))
                .serviceIds(template.getServices().stream().map(ServiciosVeterinarios::getId).toList())
                .serviceNames(template.getServices().stream().map(ServiciosVeterinarios::getNombre).toList())
                .sections(sections)
                .build();
    }

    private ClinicalTemplateResponse.Field toFieldResponse(ClinicalTemplateField field) {
        ClinicalConcept concept = field.getConcept();
        return ClinicalTemplateResponse.Field.builder()
                .id(field.getId())
                .conceptId(concept.getId())
                .conceptCode(concept.getCode())
                .label(field.getVisibleLabel())
                .helpText(field.getHelpText())
                .valueType(concept.getValueType())
                .unit(concept.getUnit())
                .required(field.isRequired())
                .visible(field.isVisible())
                .reportable(concept.isReportable())
                .order(field.getDisplayOrder())
                .species(splitSpecies(field.getApplicableSpecies()))
                .validationConfig(concept.getValidationConfig())
                .visibilityRules(field.getVisibilityRules())
                .build();
    }

    private ClinicalTemplateSummaryResponse toSummary(ClinicalTemplate template) {
        return ClinicalTemplateSummaryResponse.builder()
                .id(template.getId())
                .templateKey(template.getTemplateKey())
                .name(template.getName())
                .version(template.getVersion())
                .status(template.getStatus())
                .publishedAt(template.getPublishedAt())
                .description(template.getDescription())
                .defaultTemplate(template.isDefaultTemplate())
                .active(template.isActive())
                .species(splitSpecies(template.getApplicableSpecies()))
                .serviceIds(template.getServices().stream().map(ServiciosVeterinarios::getId).toList())
                .serviceNames(template.getServices().stream().map(ServiciosVeterinarios::getNombre).toList())
                .build();
    }

    private ClinicalConceptResponse toConceptResponse(ClinicalConcept concept) {
        return ClinicalConceptResponse.builder()
                .id(concept.getId())
                .code(concept.getCode())
                .name(concept.getBaseName())
                .description(concept.getDescription())
                .valueType(concept.getValueType())
                .unit(concept.getUnit())
                .origin(concept.getOrigin())
                .reportable(concept.isReportable())
                .active(concept.isActive())
                .validationConfig(concept.getValidationConfig())
                .build();
    }

    private void copySections(ClinicalTemplate source, ClinicalTemplate target) {
        for (ClinicalTemplateSection sourceSection : source.getSections()) {
            ClinicalTemplateSection section = new ClinicalTemplateSection();
            section.setTemplate(target);
            section.setTitle(sourceSection.getTitle());
            section.setDisplayOrder(sourceSection.getDisplayOrder());
            for (ClinicalTemplateField sourceField : sourceSection.getFields()) {
                ClinicalTemplateField field = new ClinicalTemplateField();
                field.setSection(section);
                field.setConcept(sourceField.getConcept());
                field.setVisibleLabel(sourceField.getVisibleLabel());
                field.setHelpText(sourceField.getHelpText());
                field.setRequired(sourceField.isRequired());
                field.setVisible(sourceField.isVisible());
                field.setDisplayOrder(sourceField.getDisplayOrder());
                field.setApplicableSpecies(sourceField.getApplicableSpecies());
                field.setVisibilityRules(sourceField.getVisibilityRules());
                section.getFields().add(field);
            }
            target.getSections().add(section);
        }
    }

    private void copyTemplateMetadata(ClinicalTemplate source, ClinicalTemplate target) {
        target.setDescription(source.getDescription());
        target.setApplicableSpecies(source.getApplicableSpecies());
        target.setDefaultTemplate(source.isDefaultTemplate());
        target.setActive(source.isActive());
        target.getServices().addAll(source.getServices());
    }

    private void addDefaultSection(ClinicalTemplate template, String title, int order, List<DefaultField> defaults) {
        ClinicalTemplateSection section = new ClinicalTemplateSection();
        section.setTemplate(template);
        section.setTitle(title);
        section.setDisplayOrder(order);
        for (DefaultField item : defaults) {
            ClinicalConcept concept = conceptRepository.findByCompanyIsNullAndCode(item.code())
                    .orElseThrow(() -> new IllegalStateException("No existe el concepto clínico base " + item.code()));
            ClinicalTemplateField field = new ClinicalTemplateField();
            field.setSection(section);
            field.setConcept(concept);
            field.setVisibleLabel(item.label());
            field.setRequired(item.required());
            field.setVisible(true);
            field.setDisplayOrder(item.order());
            section.getFields().add(field);
        }
        template.getSections().add(section);
    }

    private DefaultField defaultField(String code, String label, boolean required, int order) {
        return new DefaultField(code, label, required, order);
    }

    private record DefaultField(String code, String label, boolean required, int order) {}

    private void validateUniqueOrders(ClinicalTemplateDraftRequest request) {
        Set<Integer> sectionOrders = new HashSet<>();
        for (ClinicalTemplateDraftRequest.Section section : request.getSections()) {
            if (section.getOrder() < 0 || !sectionOrders.add(section.getOrder())) {
                throw new IllegalArgumentException("Las secciones deben tener órdenes únicos y no negativos");
            }
            Set<Integer> fieldOrders = new HashSet<>();
            for (ClinicalTemplateDraftRequest.Field field : section.getFields()) {
                if (field.getOrder() < 0 || !fieldOrders.add(field.getOrder())) {
                    throw new IllegalArgumentException("Los campos deben tener órdenes únicos y no negativos por sección");
                }
            }
        }
    }

    private void requireDraft(ClinicalTemplate template) {
        if (template.getStatus() != ClinicalTemplateStatus.DRAFT) {
            throw new IllegalArgumentException("Una plantilla publicada o archivada no puede modificarse");
        }
    }

    private Integer requiredCompanyId() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) throw new IllegalArgumentException("Debe seleccionar una empresa");
        return companyId;
    }

    private String buildLocalCode(String requested, String name) {
        String suffix = requested == null || requested.isBlank() ? slug(name) : requested.trim().toLowerCase(Locale.ROOT);
        return "local.company_" + requiredCompanyId() + "." + suffix;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.length() < 3) throw new IllegalArgumentException("El nombre no permite generar un código válido");
        return normalized.substring(0, Math.min(normalized.length(), 80));
    }

    private void validateJsonObject(String raw, String message) {
        try {
            JsonNode node = objectMapper.readTree(defaultJson(raw));
            if (!node.isObject()) throw new IllegalArgumentException(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private String defaultJson(String raw) {
        return raw == null || raw.isBlank() ? "{}" : raw.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String joinSpecies(List<String> species) {
        if (species == null || species.isEmpty()) return null;
        return species.stream().map(this::normalizeSpecies).filter(Objects::nonNull).distinct().sorted()
                .collect(Collectors.joining(","));
    }

    private List<String> splitSpecies(String species) {
        if (species == null || species.isBlank()) return List.of();
        return Arrays.stream(species.split(",")).filter(value -> !value.isBlank()).toList();
    }

    private String normalizeSpecies(String species) {
        return species == null || species.isBlank() ? null : species.trim().toUpperCase(Locale.ROOT);
    }

    private ClinicalTemplate resolvePublishedTemplate(Integer companyId, String species, Long serviceId) {
        List<ClinicalTemplate> candidates = templateRepository
                .findByCompanyIdAndStatusOrderByDefaultTemplateDescNameAsc(companyId, ClinicalTemplateStatus.PUBLISHED)
                .stream()
                .filter(ClinicalTemplate::isActive)
                .filter(template -> appliesToSpecies(template, species))
                .toList();

        if (serviceId != null) {
            Optional<ClinicalTemplate> exact = candidates.stream()
                    .filter(template -> template.getServices().stream().anyMatch(service -> service.getId().equals(serviceId)))
                    .findFirst();
            if (exact.isPresent()) return exact.get();
        }

        return candidates.stream()
                .filter(template -> template.getServices().isEmpty())
                .findFirst()
                .orElseGet(() -> candidates.stream().findFirst().orElseGet(() -> publishedEntity(companyId)));
    }

    private boolean appliesToSpecies(ClinicalTemplate template, String species) {
        if (species == null || template.getApplicableSpecies() == null || template.getApplicableSpecies().isBlank()) return true;
        return splitSpecies(template.getApplicableSpecies()).contains(species);
    }

    private Set<ServiciosVeterinarios> resolveServices(Integer companyId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        Set<Long> requested = new LinkedHashSet<>(ids);
        List<ServiciosVeterinarios> services = serviceRepository.findAllById(requested).stream()
                .filter(service -> service.getCompany() != null && companyId.equals(service.getCompany().getId()))
                .toList();
        if (services.size() != requested.size()) {
            throw new IllegalArgumentException("Uno o más servicios no pertenecen a la empresa");
        }
        return new LinkedHashSet<>(services);
    }

    private String uniqueTemplateKey(Integer companyId, String name) {
        String base = slug(name).toUpperCase(Locale.ROOT);
        String key = base;
        int suffix = 2;
        while (templateRepository.countByCompanyIdAndTemplateKey(companyId, key) > 0) {
            key = base.substring(0, Math.min(base.length(), 70)) + "_" + suffix++;
        }
        return key;
    }

    private boolean appliesToSpecies(ClinicalTemplateField field, String species) {
        if (species == null || field.getApplicableSpecies() == null || field.getApplicableSpecies().isBlank()) return true;
        return splitSpecies(field.getApplicableSpecies()).contains(species);
    }
}
