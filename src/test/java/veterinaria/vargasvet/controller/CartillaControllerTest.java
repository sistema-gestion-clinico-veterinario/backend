package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.CartillaService;
import veterinaria.vargasvet.service.ControlPreventivoService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartillaControllerTest {

    private final CartillaService cartillaService = mock(CartillaService.class);
    private final ControlPreventivoService controlPreventivoService = mock(ControlPreventivoService.class);
    private final CartillaController controller = new CartillaController(cartillaService, controlPreventivoService);

    @Test
    void superAdminConsultaLaEmpresaSeleccionada() {
        when(cartillaService.listarMascotasConCartilla(eq(25), isNull(), isNull(), eq(true), any(Pageable.class)))
                .thenReturn(Page.empty());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            controller.listarMascotasConCartilla(25, null, null, 0, 10);
        }

        verify(cartillaService).listarMascotasConCartilla(eq(25), isNull(), isNull(), eq(true), any(Pageable.class));
    }

    @Test
    void usuarioDeEmpresaNoPuedeForzarOtraEmpresa() {
        when(cartillaService.listarMascotasConCartilla(eq(7), isNull(), isNull(), eq(true), any(Pageable.class)))
                .thenReturn(Page.empty());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(7);

            controller.listarMascotasConCartilla(99, null, null, 0, 10);
        }

        verify(cartillaService).listarMascotasConCartilla(eq(7), isNull(), isNull(), eq(true), any(Pageable.class));
        verify(cartillaService, never()).listarMascotasConCartilla(eq(99), any(), any(), any(), any());
    }
}
