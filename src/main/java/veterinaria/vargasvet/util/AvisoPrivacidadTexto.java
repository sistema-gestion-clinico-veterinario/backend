package veterinaria.vargasvet.util;

import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class AvisoPrivacidadTexto {

    private AvisoPrivacidadTexto() {
    }

    public static CamposAvisoPrivacidad limpiar(CamposAvisoPrivacidad campos) {
        return new CamposAvisoPrivacidad(
                linea(campos.getRazonSocial()),
                linea(campos.getRuc()),
                linea(campos.getDomicilio()),
                linea(campos.getCorreoDerechos()),
                opcional(campos.getRegistroBancoDatos()),
                opcional(campos.getEncargadoTratamiento()),
                lista(campos.getFinalidades()),
                lista(campos.getDatosObligatorios()),
                lista(campos.getDatosFacultativos()),
                lista(campos.getDestinatarios()),
                linea(campos.getTransferencias()),
                linea(campos.getPlazoConservacion()));
    }

    public static String componer(CamposAvisoPrivacidad c, int version) {
        StringBuilder t = new StringBuilder();
        t.append("AVISO DE PRIVACIDAD - Versión ").append(version).append("\n\n");

        t.append("1. Quién trata sus datos\n");
        t.append("El titular del banco de datos personales es ").append(c.getRazonSocial())
                .append(", con RUC ").append(c.getRuc()).append(" y domicilio en ").append(c.getDomicilio()).append(".\n");
        if (c.getRegistroBancoDatos() != null) {
            t.append("Banco de datos inscrito con el código ").append(c.getRegistroBancoDatos()).append(".\n");
        }
        if (c.getEncargadoTratamiento() != null) {
            t.append("Encargado del tratamiento: ").append(c.getEncargadoTratamiento()).append(".\n");
        }

        t.append("\n2. Qué datos se registran\n");
        t.append("Datos obligatorios, sin los cuales no podemos registrarle ni atender a su mascota:\n");
        vinetas(t, c.getDatosObligatorios());
        if (!c.getDatosFacultativos().isEmpty()) {
            t.append("Datos facultativos, que puede no proporcionar:\n");
            vinetas(t, c.getDatosFacultativos());
        }

        t.append("\n3. Para qué se usan\n");
        vinetas(t, c.getFinalidades());
        t.append("Recordatorios del servicio: le enviaremos por correo recordatorios de las vacunas y desparasitaciones de su mascota. ")
                .append("Puede pedir en cualquier momento que no se los enviemos, sin que eso afecte la atención.\n");

        t.append("\n4. A quién se comunican\n");
        vinetas(t, c.getDestinatarios());
        t.append("Transferencias de datos: ").append(c.getTransferencias()).append("\n");

        t.append("\n5. Consecuencias de proporcionar sus datos o de negarse\n");
        t.append("Si no proporciona los datos obligatorios no podremos registrarle ni atender a su mascota. ")
                .append("Pedir no recibir los recordatorios por correo no tiene ninguna consecuencia sobre la atención.\n");

        t.append("\n6. Cuánto tiempo se conservan\n");
        t.append(c.getPlazoConservacion()).append("\n");

        t.append("\n7. Sus derechos y cómo ejercerlos\n");
        t.append("Usted puede ejercer sus derechos de acceso, actualización, inclusión, rectificación, supresión, ")
                .append("oposición y a impedir el suministro de sus datos, escribiendo a ").append(c.getCorreoDerechos())
                .append(" o pidiéndolo en la clínica, sin costo. ")
                .append("Puede dejar de recibir los recordatorios por correo en cualquier momento, desde su perfil en el portal o pidiéndolo en la clínica. ")
                .append("Si no se atiende su solicitud, puede acudir a la Autoridad Nacional de Protección de Datos Personales.\n");
        return t.toString().strip();
    }

    private static void vinetas(StringBuilder t, List<String> items) {
        for (String item : items) {
            t.append("- ").append(item).append("\n");
        }
    }

    private static String linea(String valor) {
        return valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
    }

    private static String opcional(String valor) {
        String limpio = linea(valor);
        return limpio.isEmpty() ? null : limpio;
    }

    private static List<String> lista(List<String> valores) {
        if (valores == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> limpios = new LinkedHashSet<>();
        for (String valor : valores) {
            String limpio = linea(valor);
            if (!limpio.isEmpty()) {
                limpios.add(limpio);
            }
        }
        return new ArrayList<>(limpios);
    }
}
