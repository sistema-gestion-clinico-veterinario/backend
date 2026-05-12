package veterinaria.vargasvet.modules.clinical.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.modules.clinical.domain.entity.ArchivoClinico;
import veterinaria.vargasvet.modules.clinical.domain.entity.Consulta;
import veterinaria.vargasvet.modules.clinical.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.modules.clinical.domain.enums.TipoArchivo;
import veterinaria.vargasvet.modules.clinical.dto.ArchivoClinicoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.modules.clinical.repository.ArchivoClinicoRepository;
import veterinaria.vargasvet.modules.clinical.repository.ConsultaRepository;
import veterinaria.vargasvet.modules.users.security.SecurityUtils;
import veterinaria.vargasvet.modules.shared.service.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArchivoClinicoServiceImpl implements ArchivoClinicoService {

    private static final long MAX_TAMANIO_BYTES = 20L * 1024 * 1024;

    private static final Set<String> EXTENSIONES_RADIOGRAFIA = Set.of(".dcm", ".jpg", ".jpeg");
    private static final Set<String> EXTENSIONES_LABORATORIO = Set.of(".jpg", ".jpeg", ".png", ".pdf");

    private static final Set<String> MIMES_RADIOGRAFIA = Set.of(
            "application/dicom", "application/octet-stream", "image/jpeg");
    private static final Set<String> MIMES_LABORATORIO = Set.of(
            "image/jpeg", "image/png", "application/pdf");

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

        if (consulta.getEstado() == EstadoConsulta.CERRADA) {
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

        if (extension.equals(".dcm")) {
            validarCabeceraDicom(file);
        }

        String filename = storageService.store(file);

        ArchivoClinico archivo = new ArchivoClinico();
        archivo.setConsulta(consulta);
        archivo.setNombre(file.getOriginalFilename());
        archivo.setTipo(tipo);
        archivo.setTipoMime(file.getContentType());
        archivo.setTamanioBytes(file.getSize());
        archivo.setUrl(filename);
        archivo.setDescripcion(descripcion);
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
        } else {
            throw new IllegalArgumentException("Tipo de examen no válido. Use RADIOGRAFIA o LABORATORIO");
        }
    }

    private void validarCabeceraDicom(MultipartFile file) {
        try {
            byte[] cabecera = new byte[132];
            int leidos = file.getInputStream().read(cabecera, 0, 132);
            if (leidos < 132) {
                throw new IllegalArgumentException("El archivo .dcm no tiene una cabecera DICOM válida");
            }
            if (cabecera[128] != 'D' || cabecera[129] != 'I' || cabecera[130] != 'C' || cabecera[131] != 'M') {
                throw new IllegalArgumentException("El archivo .dcm no contiene una cabecera DICOM válida");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo para validación DICOM", e);
        }
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
        storageService.delete(archivo.getUrl());
        archivoClinicoRepository.delete(archivo);
    }

    @Override
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
