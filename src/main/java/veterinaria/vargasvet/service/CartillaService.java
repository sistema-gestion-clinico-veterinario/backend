package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.request.CartillaAplicacionEditRequest;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.dto.response.CartillaAplicacionResponse;
import veterinaria.vargasvet.dto.response.MascotaCartillaResponse;
import veterinaria.vargasvet.dto.response.RecordatorioWhatsAppResponse;

import java.util.List;

public interface CartillaService {

    CartillaAplicacionResponse registrarVacunacion(CartillaAplicacionRequest request);

    CartillaAplicacionResponse registrarDesparasitacion(CartillaAplicacionRequest request);

    Page<MascotaCartillaResponse> listarMascotasConCartilla(Integer companyId, String nombre, EspecieMascota especie, Boolean activo, Pageable pageable);

    CartillaAplicacionResponse editarVacunacion(Long id, CartillaAplicacionEditRequest request);

    CartillaAplicacionResponse editarDesparasitacion(Long id, CartillaAplicacionEditRequest request);

    void cambiarEstadoVacunacion(Long id, boolean activo);

    void cambiarEstadoDesparasitacion(Long id, boolean activo);

    List<RecordatorioWhatsAppResponse> listarRecordatoriosPreventivosWhatsApp(Integer companyId);
}
