package veterinaria.vargasvet.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class PortalAppointmentsPage extends BasePage {
    public PortalAppointmentsPage(WebDriver driver) {
        super(driver);
    }

    public PortalAppointmentsPage open() {
        go("/apoderado/mis-citas");
        visible(By.xpath("//h1[contains(.,'Mis Citas')]"));
        return this;
    }

    public void openNew() {
        clickText("Nueva Cita");
    }

    public void create(String pet, String service, String employee, String date, String time, String reason) {
        selectContaining(By.cssSelector("select[formcontrolname='mascotaId']"), pet);
        selectContaining(By.cssSelector("select[formcontrolname='servicioId']"), service);
        selectContaining(By.cssSelector("select[formcontrolname='veterinarioId']"), employee);
        setInputValue(By.cssSelector("input[formcontrolname='fechaCita']"), date);
        if (isPresent(By.xpath("//button[normalize-space()=" + literal(time) + "]"))) {
            click(By.xpath("//button[normalize-space()=" + literal(time) + "]"));
        } else {
            setInputValue(By.cssSelector("input[formcontrolname='horaCita']"), time);
        }
        type(By.cssSelector("input[formcontrolname='motivoCita']"), reason);
        click(By.cssSelector("form button[type='submit']"));
        clickText("Sí, confirmar");
    }
}
