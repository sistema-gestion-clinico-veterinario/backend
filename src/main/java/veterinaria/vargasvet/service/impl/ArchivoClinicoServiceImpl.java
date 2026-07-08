package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.domain.entity.ArchivoClinico;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.TipoArchivo;
import veterinaria.vargasvet.dto.response.ArchivoClinicoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ArchivoClinicoRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ArchivoClinicoService;
import veterinaria.vargasvet.service.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ArchivoClinicoServiceImpl implements ArchivoClinicoService {

    private static final long MAX_TAMANIO_BYTES = 20L * 1024 * 1024;

    private static final Set<String> EXTENSIONES_RADIOGRAFIA = Set.of(".dcm", ".jpg", ".jpeg");
    private static final Set<String> EXTENSIONES_LABORATORIO = Set.of(".jpg", ".jpeg", ".png", ".pdf");
    private static final Set<String> EXTENSIONES_DOCUMENTO   = Set.of(".docx", ".doc");

    private static final Set<String> MIMES_RADIOGRAFIA = Set.of(
            "application/dicom", "application/octet-stream", "image/jpeg");
    private static final Set<String> MIMES_LABORATORIO = Set.of(
            "image/jpeg", "image/png", "application/pdf");
    private static final Set<String> MIMES_DOCUMENTO = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/octet-stream");
    private static final Pattern HAS_LETTER_OR_NUMBER = Pattern.compile(".*[\\p{L}\\p{N}].*");
    private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*");
    private static final Pattern UNSAFE_TEXT = Pattern.compile(".*[{}\\[\\]<>*|\\\\^~`=@].*");
    private static final Pattern XSS_SIGNAL = Pattern.compile("(?i).*(<\\s*script|javascript:|on\\w+\\s*=|</?\\s*[a-z][^>]*>).*");

    private final ConsultaRepository consultaRepository;
    private final ArchivoClinicoRepository archivoClinicoRepository;
    private final StorageService storageService;

    @Override
    @Transactional
    public ArchivoClinicoResponse subirArchivo(Long consultaId, MultipartFile file, TipoArchivo tipo, String descripcion) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId));

        if (!SecurityUtils.isSuperAdmin()) {
            Integer companyId = SecurityUtils.getCurrentCompanyId();
            if (consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany() == null ||
                !consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("No tienes permiso para cargar archivos en esta consulta");
            }
        }

        if (consulta.getEstado() == EstadoConsulta.CERRADA && !puedeModificarArchivoCerrado()) {
            throw new IllegalArgumentException("No se pueden cargar archivos en una historia clínica cerrada");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        if (file.getSize() > MAX_TAMANIO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido de 20 MB");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        validarTipoYExtension(tipo, extension, file.getContentType());

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo", e);
        }

        if (extension.equals(".dcm")) {
            validarCabeceraDicom(fileBytes);
        }

        String descripcionNormalizada = normalizarDescripcion(descripcion);
        String filename = storageService.storeBytes(fileBytes, extension, file.getContentType(), file.getOriginalFilename());

        ArchivoClinico archivo = new ArchivoClinico();
        archivo.setConsulta(consulta);
        archivo.setNombre(file.getOriginalFilename());
        archivo.setTipo(tipo);
        archivo.setTipoMime(file.getContentType());
        archivo.setTamanioBytes(file.getSize());
        archivo.setUrl(filename);
        archivo.setDescripcion(descripcionNormalizada);
        archivo.setSubidoPor(SecurityUtils.getCurrentUserEmail());

        return toResponse(archivoClinicoRepository.save(archivo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchivoClinicoResponse> listarPorConsulta(Long consultaId) {
        if (!consultaRepository.existsById(consultaId)) {
            throw new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId);
        }
        return archivoClinicoRepository.findByConsultaId(consultaId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validarTipoYExtension(TipoArchivo tipo, String extension, String mimeType) {
        if (tipo == TipoArchivo.RADIOGRAFIA) {
            if (!EXTENSIONES_RADIOGRAFIA.contains(extension)) {
                throw new IllegalArgumentException(
                        "Extensión no permitida para radiografías. Use: .dcm, .jpg, .jpeg");
            }
            if (mimeType != null && !extension.equals(".dcm") && !MIMES_RADIOGRAFIA.contains(mimeType)) {
                throw new IllegalArgumentException("Tipo de archivo no válido para radiografías");
            }
        } else if (tipo == TipoArchivo.LABORATORIO) {
            if (!EXTENSIONES_LABORATORIO.contains(extension)) {
                throw new IllegalArgumentException(
                        "Extensión no permitida para hemogramas. Use: .jpg, .jpeg, .png, .pdf");
            }
            if (mimeType != null && !MIMES_LABORATORIO.contains(mimeType)) {
                throw new IllegalArgumentException("Tipo de archivo no válido para hemogramas");
            }
        } else if (tipo == TipoArchivo.DOCUMENTO) {
            if (!EXTENSIONES_DOCUMENTO.contains(extension)) {
                throw new IllegalArgumentException(
                        "Extensión no permitida para documentos. Use: .docx, .doc");
            }
            if (mimeType != null && !MIMES_DOCUMENTO.contains(mimeType)) {
                throw new IllegalArgumentException("Tipo de archivo no válido para documentos Word");
            }
        } else {
            throw new IllegalArgumentException("Tipo de archivo no válido.");
        }
    }

    private void validarCabeceraDicom(byte[] bytes) {
        if (bytes.length < 132) {
            throw new IllegalArgumentException("El archivo .dcm no tiene una cabecera DICOM válida");
        }
        if (bytes[128] != 'D' || bytes[129] != 'I' || bytes[130] != 'C' || bytes[131] != 'M') {
            throw new IllegalArgumentException("El archivo no es un DICOM válido (cabecera DICM ausente)");
        }
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String value = descripcion.trim().replaceAll("[\\t\\x0B\\f\\r]+", " ").replaceAll(" {2,}", " ");
        if (value.length() > 300) {
            throw new IllegalArgumentException("La descripcion no debe superar 300 caracteres");
        }
        if (!HAS_LETTER_OR_NUMBER.matcher(value).matches() || !HAS_LETTER.matcher(value).matches()) {
            throw new IllegalArgumentException("La descripcion debe contener texto real, no solo numeros o simbolos");
        }
        if (UNSAFE_TEXT.matcher(value).matches() || XSS_SIGNAL.matcher(value).matches()) {
            throw new IllegalArgumentException("La descripcion contiene caracteres no permitidos");
        }
        return value;
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoClinicoResponse obtenerPorId(Long id) {
        return toResponse(archivoClinicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado con ID: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource servirContenido(Long id) {
        ArchivoClinico archivo = archivoClinicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado con ID: " + id));
        return storageService.loadAsResource(archivo.getUrl());
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        ArchivoClinico archivo = archivoClinicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado con ID: " + id));
        if (archivo.getConsulta().getEstado() == EstadoConsulta.CERRADA && !puedeModificarArchivoCerrado()) {
            throw new IllegalArgumentException("No se pueden eliminar archivos de una historia clínica cerrada");
        }
        storageService.delete(archivo.getUrl());
        archivoClinicoRepository.delete(archivo);
    }

    private boolean puedeModificarArchivoCerrado() {
        return SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin();
    }

    public ArchivoClinicoResponse toResponse(ArchivoClinico archivo) {
        ArchivoClinicoResponse response = new ArchivoClinicoResponse();
        response.setId(archivo.getId());
        response.setNombre(archivo.getNombre());
        response.setTipo(archivo.getTipo());
        response.setTipoMime(archivo.getTipoMime());
        response.setTamanioBytes(archivo.getTamanioBytes());
        response.setUrl(archivo.getUrl());
        response.setDescripcion(archivo.getDescripcion());
        response.setSubidoPor(archivo.getSubidoPor());
        response.setFechaCarga(archivo.getCreatedAt());
        return response;
    }
}
