package veterinaria.vargasvet.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class ClinicalPage extends BasePage {
    public ClinicalPage(WebDriver driver) {
        super(driver);
    }

    public ClinicalPage waitUntilOpen() {
        visible(By.xpath("//h1[contains(.,'Atención Clínica')]"));
        return this;
    }

    public void fillRequiredData(String anamnesis) {
        selectText(By.cssSelector("select[formcontrolname='tipoConsulta']"), "Control rutina");
        type(By.cssSelector("[formcontrolname='pesoEnConsulta']"), "12.8");
        clickText("Datos clínicos");
        type(By.cssSelector("[formcontrolname='anamnesis']"), anamnesis);
        type(By.cssSelector("[formcontrolname='examenFisico']"), "Evaluación clínica E2E sin hallazgos críticos");
        type(By.cssSelector("[formcontrolname='observaciones']"), "Seguimiento Selenium E2E");
    }

    public String currentVersion() {
        return driver.findElement(By.cssSelector("input[formcontrolname='version']")).getAttribute("value");
    }

    public void waitForVersionChange(String previousVersion) {
        wait.until(current -> !previousVersion.equals(currentVersion()));
    }

    public void retryFailedSave() {
        visible(By.xpath("//*[contains(normalize-space(.),'Error al guardar')]"));
        clickText("Reintentar");
    }

    public void uploadRadiography(String absolutePath) {
        clickText("Exámenes");
        all(By.cssSelector("input[type='file']")).getFirst().sendKeys(absolutePath);
        selectText(By.cssSelector("select:not([formcontrolname])"), "Radiografía");
        type(By.cssSelector("input[placeholder*='Hemograma completo']"), "Radiografía Selenium E2E");
        clickText("Subir archivo");
    }

    public void closeConsultation() {
        clickText("Cerrar consulta");
        clickText("Sí, cerrar");
        wait.until(current -> current.getCurrentUrl().contains("/citas/agenda")
                || current.findElement(By.tagName("body")).getText().contains("consulta fue cerrada"));
    }
}
