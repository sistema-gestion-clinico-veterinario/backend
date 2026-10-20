package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.SexoMascota;
import veterinaria.vargasvet.dto.request.AltaClienteRequest;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.request.PrimeraMascotaRequest;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientOnboardingServiceTest {
    @Mock ApoderadoService apoderadoService;
    @Mock MascotaService mascotaService;
    @InjectMocks ClientOnboardingService service;

    @Test
    void registraResponsableYMascotaComoUnaSolaOperacion() {
        ApoderadoRequest clienteRequest = new ApoderadoRequest();
        PrimeraMascotaRequest mascotaInicial = new PrimeraMascotaRequest();
        mascotaInicial.setNombreCompleto("Luna");
        mascotaInicial.setEspecie(EspecieMascota.GATO);
        mascotaInicial.setRazaId(12L);
        mascotaInicial.setSexo(SexoMascota.HEMBRA);
        mascotaInicial.setFechaNacimiento(LocalDate.now().minusYears(2));

        AltaClienteRequest request = new AltaClienteRequest();
        request.setCliente(clienteRequest);
        request.setMascota(mascotaInicial);

        UserProfileDTO cliente = new UserProfileDTO();
        cliente.setApoderadoId(41);
        MascotaResponse mascota = new MascotaResponse();
        mascota.setId(77L);
        mascota.setNombreCompleto("Luna");

        when(apoderadoService.registerApoderado(clienteRequest)).thenReturn(cliente);
        when(mascotaService.registerMascota(any(MascotaRequest.class))).thenReturn(mascota);

        var response = service.registrar(request);

        ArgumentCaptor<MascotaRequest> captor = ArgumentCaptor.forClass(MascotaRequest.class);
        verify(mascotaService).registerMascota(captor.capture());
        assertThat(captor.getValue().getApoderadoId()).isEqualTo(41L);
        assertThat(captor.getValue().getNombreCompleto()).isEqualTo("Luna");
        assertThat(response.cliente()).isSameAs(cliente);
        assertThat(response.mascota()).isSameAs(mascota);
    }
}
