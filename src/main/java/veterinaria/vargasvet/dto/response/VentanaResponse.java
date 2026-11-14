package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.domain.enums.ViewAudience;

import java.util.List;

public record VentanaResponse(
        Integer id,
        String codigo,
        String nombre,
        String grupo,
        Integer orden,
        boolean activo,
        String icono,
        MenuPresentation presentacionDefault,
        List<VistaDeVentana> vistas) {

    public record VistaDeVentana(
            Integer id,
            String codigo,
            String nombre,
            String ruta,
            String grupo,
            Integer orden,
            Integer ordenGrupo,
            String icono,
            boolean activo,
            boolean visibleMenu,
            ViewAudience audience) {

        static VistaDeVentana de(Vista vista) {
            return new VistaDeVentana(vista.getId(), vista.getCodigo(), vista.getNombre(), vista.getRuta(),
                    vista.getGrupo(), vista.getOrden(), vista.getOrdenGrupo(), vista.getIcono(), vista.isActivo(),
                    vista.isVisibleMenu(), vista.getAudience());
        }
    }

    public static VentanaResponse de(Ventana ventana) {
        return new VentanaResponse(ventana.getId(), ventana.getCodigo(), ventana.getNombre(), ventana.getGrupo(),
                ventana.getOrden(), ventana.isActivo(), ventana.getIcono(), ventana.getPresentacionDefault(),
                ventana.getVistas() == null ? null : ventana.getVistas().stream().map(VistaDeVentana::de).toList());
    }
}
