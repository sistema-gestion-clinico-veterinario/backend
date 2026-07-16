package veterinaria.vargasvet.selenium.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;
import veterinaria.vargasvet.selenium.pages.AgendaPage;
import veterinaria.vargasvet.selenium.pages.LoginPage;
import veterinaria.vargasvet.selenium.pages.PortalAppointmentsPage;
import veterinaria.vargasvet.selenium.support.BaseSeleniumTest;
import veterinaria.vargasvet.selenium.support.FixtureClient;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(1)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppointmentBusinessE2ETest extends BaseSeleniumTest {

    @Test
    @Order(1)
    @DisplayName("CASO 01 - Solo una sesión reserva el mismo horario concurrente")
    void caso01_reserva_concurrente_del_mismo_horario() throws Exception {
        WebDriver second = secondaryDriver();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            AgendaPage firstPage = prepareAdminAppointment(driver, fixtures.text("concurrencyDate"), "14:00", "E2E CONCURRENCIA");
            AgendaPage secondPage = prepareAdminAppointment(second, fixtures.text("concurrencyDate"), "14:00", "E2E CONCURRENCIA");

            Callable<AgendaPage.SubmissionOutcome> firstSubmit = firstPage::submitAndAwaitOutcome;
            Callable<AgendaPage.SubmissionOutcome> secondSubmit = secondPage::submitAndAwaitOutcome;
            List<Future<AgendaPage.SubmissionOutcome>> results = executor.invokeAll(List.of(firstSubmit, secondSubmit));
            List<AgendaPage.SubmissionOutcome> outcomes = results.stream().map(this::get).toList();
            long successful = outcomes.stream().filter(AgendaPage.SubmissionOutcome.SUCCESS::equals).count();
            long rejected = outcomes.stream().filter(AgendaPage.SubmissionOutcome.CONFLICT::equals).count();

            assertEquals(1, successful, "Exactamente una sesión debe obtener la reserva");
            assertEquals(1, rejected, "La otra sesión debe recibir el conflicto de horario");
        } finally {
            executor.shutdownNow();
            second.quit();
        }
    }

    @Test
    @Order(2)
    @DisplayName("CASO 02 - La disponibilidad respeta clínica, turno, duración y ocupación")
    void caso02_disponibilidad_integral() {
        loginAdmin(driver);
        AgendaPage agenda = new AgendaPage(driver).open(fixtures.text("appointmentDate"));
        agenda.openNew();
        agenda.selectOwner("Ana Pruebas");
        agenda.selectPet(fixtures.text("petName"));
        agenda.selectService(fixtures.text("serviceName"));
        agenda.selectEmployee(fixtures.text("vetName"));
        agenda.setDate(fixtures.text("appointmentDate"));

        List<String> slots = agenda.availableSlots();
        assertFalse(slots.isEmpty(), "Debe existir al menos un horario disponible");
        assertFalse(slots.contains("09:00"), "09:00 ya está ocupado");
        assertFalse(slots.contains("10:00"), "10:00 ya está ocupado");
        assertFalse(slots.contains("12:00"), "12:00 ya está ocupado");
        assertTrue(slots.stream().allMatch(this::insideClinicHours), "Todos los horarios deben estar dentro de 08:00-18:00");
    }

    @Test
    @Order(3)
    @DisplayName("CASO 03 - El portal aplica límites diarios por mascota y apoderado")
    void caso03_limites_diarios_del_apoderado() {
        new LoginPage(driver).open().login(fixtures.text("ownerEmail"), fixtures.text("ownerPassword"));
        PortalAppointmentsPage portal = new PortalAppointmentsPage(driver).open();
        String date = fixtures.text("quotaDate");

        createPortalAppointment(portal, fixtures.text("petName"), date, "09:00", "CUOTA UNO");
        createPortalAppointment(portal, fixtures.text("petName"), date, "10:00", "CUOTA DOS");
        portal.openNew();
        portal.create(fixtures.text("petName"), fixtures.text("serviceName"), fixtures.text("vetName"), date, "11:00", "CUOTA TRES MISMA MASCOTA");
        assertTrue(portal.containsText("más de 2 citas"), "La tercera cita de la misma mascota debe rechazarse");

        driver.navigate().refresh();
        portal.openNew();
        portal.create(fixtures.text("secondPetName"), fixtures.text("serviceName"), fixtures.text("vetName"), date, "11:00", "CUOTA TOTAL TRES");
        assertTrue(portal.containsText("agendó") || portal.containsText("programada") || portal.containsText("registrada"));

        portal.openNew();
        portal.create(fixtures.text("thirdPetName"), fixtures.text("serviceName"), fixtures.text("vetName"), date, "12:00", "CUOTA TOTAL CUATRO");
        assertTrue(portal.containsText("más de 3 citas"), "La cuarta cita total del apoderado debe rechazarse");
    }

    @Test
    @Order(4)
    @DisplayName("CASO 16 - Reprogramar conserva datos y respeta disponibilidad")
    void caso16_reprogramacion_con_reglas() {
        loginAdmin(driver);
        AgendaPage agenda = new AgendaPage(driver).open(fixtures.text("appointmentDate"));
        agenda.openActions("E2E REPROGRAMAR");
        agenda.chooseAction("Reprogramar");
        agenda.setDate(fixtures.text("rescheduleTargetDate"));
        agenda.selectSlot("14:00");
        agenda.submit();
        agenda.clickText("Sí, reprogramar");

        agenda.open(fixtures.text("rescheduleTargetDate"));
        assertTrue(agenda.rowHas("E2E REPROGRAMAR", "Reprogramada"));
    }

    @Test
    @Order(5)
    @DisplayName("CASO 18 - Una emergencia autorizada se agenda con la clínica cerrada")
    void caso18_emergencia_fuera_del_horario() {
        FixtureClient client = new FixtureClient();
        client.post("/setup/e2e/scenario/clinic-closed-today?closed=true");
        try {
            loginAdmin(driver);
            AgendaPage agenda = new AgendaPage(driver).open(java.time.LocalDate.now().toString());
            agenda.openNew();
            agenda.selectOwner("Ana Pruebas");
            agenda.selectPet(fixtures.text("petName"));
            agenda.selectService(fixtures.text("serviceName"));
            agenda.selectEmployee(fixtures.text("vetName"));
            agenda.setDate(java.time.LocalDate.now().toString());
            if (agenda.containsText("Agendar como Emergencia")) {
                agenda.clickText("Agendar como Emergencia");
            } else {
                agenda.setEmergency(true);
            }
            agenda.setReason("EMERGENCIA SELENIUM");
            agenda.submit();
            assertTrue(agenda.containsText("programada") || agenda.containsText("Emergencia Médica"));
        } finally {
            client.post("/setup/e2e/scenario/clinic-closed-today?closed=false");
        }
    }

    private AgendaPage prepareAdminAppointment(WebDriver target, String date, String slot, String reason) {
        loginAdmin(target);
        AgendaPage agenda = new AgendaPage(target).open(date);
        agenda.openNew();
        agenda.selectOwner("Ana Pruebas");
        agenda.selectPet(fixtures.text("petName"));
        agenda.selectService(fixtures.text("serviceName"));
        agenda.selectEmployee(fixtures.text("vetName"));
        agenda.setDate(date);
        agenda.selectSlot(slot);
        agenda.setReason(reason);
        return agenda;
    }

    private void createPortalAppointment(PortalAppointmentsPage portal, String pet, String date, String time, String reason) {
        portal.openNew();
        portal.create(pet, fixtures.text("serviceName"), fixtures.text("vetName"), date, time, reason);
        assertTrue(portal.containsText("agendó") || portal.containsText("programada") || portal.containsText("registrada"));
    }

    private <T> T get(Future<T> result) {
        try {
            return result.get();
        } catch (Exception e) {
            throw new AssertionError("Falló una sesión concurrente", e);
        }
    }

    private boolean insideClinicHours(String text) {
        LocalTime time = LocalTime.parse(text);
        return !time.isBefore(LocalTime.of(8, 0)) && time.plusMinutes(30).compareTo(LocalTime.of(18, 0)) <= 0;
    }

    private void loginAdmin(WebDriver target) {
        new LoginPage(target).open().login(fixtures.text("adminEmail"), fixtures.text("adminPassword"));
    }
}
