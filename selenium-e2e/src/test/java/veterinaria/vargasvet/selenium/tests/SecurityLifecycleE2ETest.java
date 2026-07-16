package veterinaria.vargasvet.selenium.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import veterinaria.vargasvet.selenium.pages.AdministrationPage;
import veterinaria.vargasvet.selenium.pages.AgendaPage;
import veterinaria.vargasvet.selenium.pages.LoginPage;
import veterinaria.vargasvet.selenium.support.BaseSeleniumTest;
import veterinaria.vargasvet.selenium.support.E2eConfig;
import veterinaria.vargasvet.selenium.support.FixtureClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(3)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityLifecycleE2ETest extends BaseSeleniumTest {

    @Test
    @Order(1)
    @DisplayName("CASO 13 - Una empresa inactiva bloquea usuarios y puede reactivarse")
    void caso13_desactivacion_de_empresa() {
        loginAdmin(driver);
        AdministrationPage admin = new AdministrationPage(driver);
        admin.toggleCompany(fixtures.text("toggleCompanyName"), false);
        try {
            WebDriver affected = secondaryDriver();
            try {
                new LoginPage(affected).open().loginExpectingError(
                        fixtures.text("toggleCompanyAdminEmail"),
                        fixtures.text("toggleCompanyAdminPassword"),
                        "empresa"
                );
            } finally {
                affected.quit();
            }
        } finally {
            admin.toggleCompany(fixtures.text("toggleCompanyName"), true);
        }

        WebDriver reactivated = secondaryDriver();
        try {
            new LoginPage(reactivated).open().login(
                    fixtures.text("toggleCompanyAdminEmail"), fixtures.text("toggleCompanyAdminPassword"));
            assertFalse(reactivated.getCurrentUrl().contains("/login"));
        } finally {
            reactivated.quit();
        }
    }

    @Test
    @Order(2)
    @DisplayName("CASO 14 - Un rol inactivo deja de autorizar al usuario asignado")
    void caso14_desactivacion_de_rol() {
        loginAdmin(driver);
        AdministrationPage admin = new AdministrationPage(driver);
        admin.toggleRole(fixtures.text("restrictedRoleName"), false);
        try {
            WebDriver restricted = secondaryDriver();
            try {
                new LoginPage(restricted).open().loginExpectingError(
                        fixtures.text("restrictedUserEmail"),
                        fixtures.text("restrictedUserPassword"),
                        "rol activo"
                );
            } finally {
                restricted.quit();
            }
        } finally {
            admin.toggleRole(fixtures.text("restrictedRoleName"), true);
        }
    }

    @Test
    @Order(3)
    @DisplayName("CASO 21 - Un empleado inactivo desaparece de disponibilidad")
    void caso21_desactivacion_de_empleado() {
        loginAdmin(driver);
        AdministrationPage admin = new AdministrationPage(driver);
        admin.toggleEmployee(fixtures.text("toggleEmployeeName"), false);
        try {
            AgendaPage agenda = new AgendaPage(driver).open(fixtures.text("concurrencyDate"));
            agenda.openNew();
            agenda.selectOwner("Ana Pruebas");
            agenda.selectPet(fixtures.text("petName"));
            agenda.selectService(fixtures.text("serviceName"));
            agenda.click(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'Seleccionar empleado') or contains(.,'Seleccionar veterinario')]"));
            assertFalse(agenda.isPresent(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'" + fixtures.text("toggleEmployeeName") + "')]")));
        } finally {
            admin.toggleEmployee(fixtures.text("toggleEmployeeName"), true);
        }
    }

    @Test
    @Order(4)
    @DisplayName("CASO 28 - Un servicio desactivado no admite nuevas citas y conserva historial")
    void caso28_desactivacion_de_servicio() {
        loginAdmin(driver);
        AdministrationPage admin = new AdministrationPage(driver);
        admin.toggleService(fixtures.text("toggleServiceName"), false);
        try {
            AgendaPage agenda = new AgendaPage(driver).open(fixtures.text("concurrencyDate"));
            agenda.openNew();
            agenda.selectOwner("Ana Pruebas");
            agenda.selectPet(fixtures.text("secondPetName"));
            agenda.click(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'Seleccionar servicio')]"));
            assertFalse(agenda.isPresent(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'" + fixtures.text("toggleServiceName") + "')]")));

            agenda.open(fixtures.text("historicalServiceDate"));
            assertTrue(agenda.rowHas("E2E SERVICIO HISTORICO", "E2E SERVICIO HISTORICO"));
        } finally {
            admin.toggleService(fixtures.text("toggleServiceName"), true);
        }
    }

    @Test
    @Order(5)
    @DisplayName("CASO 24 - El apoderado no accede al historial de otra persona")
    void caso24_aislamiento_de_datos_del_apoderado() {
        LoginPage portal = new LoginPage(driver);
        portal.open().login(fixtures.text("ownerEmail"), fixtures.text("ownerPassword"));
        driver.get(E2eConfig.BASE_URL + "/mi-historial/" + fixtures.number("petId"));
        assertTrue(portal.containsText(fixtures.text("petName")));

        driver.get(E2eConfig.BASE_URL + "/mi-historial/" + fixtures.number("foreignPetId"));
        assertTrue(portal.containsText(fixtures.text("petName")),
                "Ante un identificador ajeno, el portal debe conservar solamente las mascotas autorizadas");
        String body = driver.findElement(By.tagName("body")).getText();
        assertFalse(body.contains(fixtures.text("foreignPetName")), "No debe exponer datos de la mascota ajena");
    }

    @Test
    @Order(6)
    @DisplayName("CASO 31 - El empleado activa su cuenta y luego inicia sesión")
    void caso31_activacion_de_cuenta() {
        new FixtureClient().post("/setup/e2e/scenario/reset-activation");
        String newPassword = "Activated!456";
        driver.get(E2eConfig.BASE_URL + "/auth/verify/selenium-e2e-activation-token");
        var page = new LoginPage(driver);
        page.type(By.cssSelector("input[formcontrolname='password']"), newPassword);
        page.type(By.cssSelector("input[formcontrolname='confirmPassword']"), newPassword);
        page.click(By.xpath("//button[@type='submit']"));
        assertTrue(page.containsText("Cuenta activada"));

        page.open().login(fixtures.text("activationEmail"), newPassword);
        assertFalse(driver.getCurrentUrl().contains("/login"));
    }

    private void loginAdmin(WebDriver target) {
        new LoginPage(target).open().login(fixtures.text("adminEmail"), fixtures.text("adminPassword"));
    }
}
