package veterinaria.vargasvet.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public final class AgendaPage extends BasePage {
    private static final By FORM = By.id("citaSidebarForm");

    public AgendaPage(WebDriver driver) {
        super(driver);
    }

    public AgendaPage open(String date) {
        go("/citas/agenda");
        visible(By.xpath("//h1[contains(.,'Agenda de Citas')]"));
        if (date != null) {
            setInputValue(By.cssSelector("main input[type='date']"), date);
        }
        return this;
    }

    public void openNew() {
        clickText("Nueva Cita");
        visible(FORM);
    }

    public void selectOwner(String name) {
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'Seleccionar dueño') or contains(.,'Seleccionar propietario')]"));
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(normalize-space(.)," + literal(name) + ")]"));
    }

    public void selectPet(String name) {
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'Seleccionar mascota')]"));
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(normalize-space(.)," + literal(name) + ")]"));
    }

    public void selectService(String name) {
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'Seleccionar servicio')]"));
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(normalize-space(.)," + literal(name) + ")]"));
    }

    public void selectEmployee(String name) {
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(.,'Seleccionar empleado') or contains(.,'Seleccionar veterinario')]"));
        click(By.xpath("//*[@id='citaSidebarForm']//button[contains(normalize-space(.)," + literal(name) + ")]"));
    }

    public void setDate(String date) {
        setInputValue(By.cssSelector("#citaSidebarForm [formcontrolname='fechaCita']"), date);
    }

    public List<String> availableSlots() {
        return driver.findElements(By.cssSelector("#citaSidebarForm button"))
                .stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> text.matches("\\d{2}:\\d{2}"))
                .distinct()
                .toList();
    }

    public void selectSlot(String time) {
        click(By.xpath("//*[@id='citaSidebarForm']//button[normalize-space()=" + literal(time) + "]"));
    }

    public void setEmergency(boolean emergency) {
        WebElement checkbox = visible(By.id("chkEmergenciaSB"));
        if (checkbox.isSelected() != emergency) {
            checkbox.click();
            pause();
        }
    }

    public void setReason(String reason) {
        type(By.cssSelector("#citaSidebarForm [formcontrolname='motivoCita']"), reason);
    }

    public void submit() {
        click(By.cssSelector("button[type='submit'][form='citaSidebarForm']"));
    }

    public SubmissionOutcome submitAndAwaitOutcome() {
        submit();
        return wait.until(current -> {
            String body = current.findElement(By.tagName("body")).getText();
            if (body.contains("Cita programada")) {
                return SubmissionOutcome.SUCCESS;
            }
            String normalized = body.toLowerCase();
            if (normalized.contains("ya tiene")
                    || normalized.contains("conflicto de horario")
                    || normalized.contains("ese horario")) {
                return SubmissionOutcome.CONFLICT;
            }
            return null;
        });
    }

    public void openActions(String reason) {
        WebElement row = rowWithExactText(reason);
        row.findElement(By.cssSelector("button")).click();
        pause();
    }

    public void chooseAction(String action) {
        click(By.xpath("//*[@role='menuitem' and contains(normalize-space(.)," + literal(action) + ")]"));
    }

    public boolean rowHas(String reason, String expected) {
        return rowWithExactText(reason).getText().contains(expected);
    }

    public enum SubmissionOutcome {
        SUCCESS,
        CONFLICT
    }
}
