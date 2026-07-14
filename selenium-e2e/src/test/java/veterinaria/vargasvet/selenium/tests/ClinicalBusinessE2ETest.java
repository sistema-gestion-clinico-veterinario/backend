package veterinaria.vargasvet.selenium.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;
import veterinaria.vargasvet.selenium.pages.AgendaPage;
import veterinaria.vargasvet.selenium.pages.ClinicalPage;
import veterinaria.vargasvet.selenium.pages.HistoryPage;
import veterinaria.vargasvet.selenium.pages.LoginPage;
import veterinaria.vargasvet.selenium.support.BaseSeleniumTest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClinicalBusinessE2ETest extends BaseSeleniumTest {

    @Test
    @Order(1)
    @DisplayName("CASO 04 - Solo el veterinario asignado inicia y completa la atención")
    void caso04_inicio_controlado_de_atencion() {
        resetClinicalAppointment("clinicalAppointmentId");
        loginVet(driver);
        ClinicalPage clinical = startAppointment(driver, "E2E CONSULTA");

        assertTrue(URI.create(driver.getCurrentUrl()).getPath().matches(".*/historias-clinicas/consulta/\\d+$"));
        String initialVersion = clinical.currentVersion();
        clinical.fillRequiredData("Paciente estable durante la atención E2E");
        clinical.waitForVersionChange(initialVersion);
        clinical.closeConsultation();
        assertTrue(driver.getCurrentUrl().contains("/citas/agenda")
                || clinical.containsText("consulta fue cerrada"));
    }

    @Test
    @Order(2)
    @DisplayName("CASO 27 - La segunda edición concurrente no sobrescribe la consulta")
    void caso27_control_de_version_en_consulta() {
        resetClinicalAppointment("concurrentClinicalAppointmentId");
        loginVet(driver);
        ClinicalPage first = startAppointment(driver, "E2E CONSULTA CONCURRENTE");
        String consultationUrl = driver.getCurrentUrl();

        WebDriver secondDriver = secondaryDriver();
        boolean firstConsultationStillOpen = true;
        try {
            loginVet(secondDriver);
            secondDriver.get(consultationUrl);
            ClinicalPage second = new ClinicalPage(secondDriver).waitUntilOpen();

            String initialVersion = first.currentVersion();
            first.fillRequiredData("Edición confirmada desde la primera sesión");
            first.waitForVersionChange(initialVersion);

            second.fillRequiredData("Edición obsoleta desde la segunda sesión");
            second.retryFailedSave();
            assertTrue(second.containsText("modificada por otro usuario"),
                    "La segunda sesión debe recibir un conflicto de versión");

            first.closeConsultation();
            firstConsultationStillOpen = false;
        } finally {
            if (firstConsultationStillOpen) {
                try {
                    first.closeConsultation();
                } catch (RuntimeException ignored) {
                    // La limpieza E2E no debe ocultar el fallo original del control de versión.
                }
            }
            secondDriver.quit();
        }
    }

    @Test
    @Order(3)
    @DisplayName("CASO 40 - Historia, radiografía y asistencia IA forman un flujo clínico")
    void caso40_flujo_clinico_con_radiografia_e_ia() throws IOException {
        resetClinicalAppointment("iaClinicalAppointmentId");
        loginVet(driver);
        ClinicalPage clinical = startAppointment(driver, "E2E CONSULTA IA");
        String initialVersion = clinical.currentVersion();
        clinical.fillRequiredData("Paciente evaluado con apoyo de imagen clínica");
        clinical.waitForVersionChange(initialVersion);

        Path image = createRadiography();
        clinical.uploadRadiography(image.toAbsolutePath().toString());
        assertTrue(clinical.containsText("radiografia-selenium.png"));
        clinical.closeConsultation();

        HistoryPage history = new HistoryPage(driver).openForPet(fixtures.text("thirdPetName"));
        history.requestAiAnalysis();
        assertTrue(history.containsText("HC + Radiografía"));
        assertTrue(history.containsText("sin hallazgos oseos agudos") || history.containsText("sin hallazgos óseos agudos"));
    }

    private ClinicalPage startAppointment(WebDriver target, String reason) {
        AgendaPage agenda = new AgendaPage(target).open(fixtures.text("clinicalAppointmentDate"));
        agenda.openActions(reason);
        agenda.chooseAction("Iniciar consulta");
        return new ClinicalPage(target).waitUntilOpen();
    }

    private void loginVet(WebDriver target) {
        new LoginPage(target).open().login(fixtures.text("vetEmail"), fixtures.text("vetPassword"));
    }

    private void resetClinicalAppointment(String fixtureKey) {
        new veterinaria.vargasvet.selenium.support.FixtureClient()
                .post("/setup/e2e/scenario/reset-clinical-appointment?fixtureKey=" + fixtureKey);
    }

    private Path createRadiography() throws IOException {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        Path directory = Path.of("target", "evidence", "inputs");
        Files.createDirectories(directory);
        Path file = directory.resolve("radiografia-selenium.png");
        Files.write(file, png);
        return file;
    }
}
