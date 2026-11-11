package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.dto.request.VistaReorderDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VistaReordenarConsultasTest {

    @Mock private VistaRepository vistaRepository;
    @Mock private VentanaRepository ventanaRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private VistaService service;

    @Test
    void reordenarVeinteVistasLasBuscaConUnaSolaConsulta() {
        List<Vista> vistas = new ArrayList<>();
        List<VistaReorderDTO> cambios = new ArrayList<>();
        for (int id = 1; id <= 20; id++) {
            Vista vista = new Vista();
            vista.setId(id);
            vista.setCodigo("VISTA_" + id);
            vista.setOrden(id);
            vistas.add(vista);
            VistaReorderDTO cambio = new VistaReorderDTO();
            cambio.setId(id);
            cambio.setOrden(100 - id);
            cambios.add(cambio);
        }
        when(vistaRepository.findAllById(any())).thenReturn(vistas);

        service.reordenar(cambios);

        verify(vistaRepository, times(1)).findAllById(any());
        verify(vistaRepository, never()).findById(anyInt());
        assertThat(vistas.get(0).getOrden()).isEqualTo(99);
        assertThat(vistas.get(19).getOrden()).isEqualTo(80);
    }

    @Test
    void siUnaVistaNoExisteInformaCualFalta() {
        Vista existente = new Vista();
        existente.setId(1);
        existente.setCodigo("VISTA_1");
        when(vistaRepository.findAllById(any())).thenReturn(List.of(existente));
        VistaReorderDTO uno = new VistaReorderDTO();
        uno.setId(1);
        uno.setOrden(10);
        VistaReorderDTO fantasma = new VistaReorderDTO();
        fantasma.setId(99);
        fantasma.setOrden(20);

        assertThatThrownBy(() -> service.reordenar(List.of(uno, fantasma)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
