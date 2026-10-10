package veterinaria.vargasvet.util;

import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

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
        return componer(c, version, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    public static String componer(CamposAvisoPrivacidad c, int version, AudienciaAvisoPrivacidad audiencia) {
        return audiencia == AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS
                ? componerParaTrabajadores(c, version)
                : componerParaPropietarios(c, version);
    }

    private static String componerParaPropietarios(CamposAvisoPrivacidad c, int version) {
        StringBuilder t = new StringBuilder();
        encabezado(t, version, "PROPIETARIOS Y PERSONAS AUTORIZADAS");

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
        t.append("Recordatorios opcionales: solo le enviaremos por correo recordatorios de las vacunas y desparasitaciones de su mascota si usted los autoriza. ")
                .append("Puede retirar esa autorización en cualquier momento, sin que eso afecte la atención.\n");

        t.append("\n4. A quién se comunican\n");
        vinetas(t, c.getDestinatarios());
        t.append("Transferencias de datos: ").append(c.getTransferencias()).append("\n");

        t.append("\n5. Consecuencias de proporcionar sus datos o de negarse\n");
        t.append("Si no proporciona los datos obligatorios no podremos registrarle ni atender a su mascota. ")
                .append("No autorizar o dejar de recibir los recordatorios por correo no tiene ninguna consecuencia sobre la atención.\n");

        t.append("\n6. Cuánto tiempo se conservan\n");
        t.append(c.getPlazoConservacion()).append("\n");

        t.append("\n7. Sus derechos y cómo ejercerlos\n");
        t.append("Usted puede ejercer sus derechos de acceso, actualización, inclusión, rectificación, supresión, ")
                .append("oposición y a impedir el suministro de sus datos, escribiendo a ").append(c.getCorreoDerechos())
                .append(" o pidiéndolo en la clínica, sin costo. ")
                .append("Puede retirar su autorización para recibir recordatorios por correo en cualquier momento, desde su perfil en el portal o pidiéndolo en la clínica. ")
                .append("Si no se atiende su solicitud, puede acudir a la Autoridad Nacional de Protección de Datos Personales.\n");
        return t.toString().strip();
    }

    private static String componerParaTrabajadores(CamposAvisoPrivacidad c, int version) {
        StringBuilder t = new StringBuilder();
        encabezado(t, version, "TRABAJADORES Y USUARIOS INTERNOS");

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
        t.append("Datos obligatorios necesarios para gestionar su vínculo laboral o profesional y habilitar su acceso al sistema:\n");
        vinetas(t, c.getDatosObligatorios());
        if (!c.getDatosFacultativos().isEmpty()) {
            t.append("Datos facultativos, que puede no proporcionar:\n");
            vinetas(t, c.getDatosFacultativos());
        }

        t.append("\n3. Para qué se usan\n");
        vinetas(t, c.getFinalidades());

        t.append("\n4. A quién se comunican\n");
        vinetas(t, c.getDestinatarios());
        t.append("Transferencias de datos: ").append(c.getTransferencias()).append("\n");

        t.append("\n5. Consecuencias de proporcionar sus datos o de negarse\n");
        t.append("Si no proporciona los datos obligatorios no podremos gestionar su vínculo con la clínica ni habilitar las funciones del sistema que correspondan a su cargo.\n");

        t.append("\n6. Cuánto tiempo se conservan\n");
        t.append(c.getPlazoConservacion()).append("\n");

        t.append("\n7. Sus derechos y cómo ejercerlos\n");
        t.append("Usted puede ejercer sus derechos de acceso, actualización, inclusión, rectificación, supresión, ")
                .append("oposición y a impedir el suministro de sus datos, escribiendo a ").append(c.getCorreoDerechos())
                .append(" o solicitándolo a la clínica, sin costo. Si no se atiende su solicitud, puede acudir a la Autoridad Nacional de Protección de Datos Personales.\n");
        return t.toString().strip();
    }

    private static void encabezado(StringBuilder t, int version, String audiencia) {
        t.append("AVISO DE PRIVACIDAD - ").append(audiencia).append(" - Versión ").append(version).append("\n\n");
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
