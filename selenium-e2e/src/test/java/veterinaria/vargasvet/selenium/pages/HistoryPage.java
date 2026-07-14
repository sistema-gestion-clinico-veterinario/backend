package veterinaria.vargasvet.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public final class HistoryPage extends BasePage {
    public HistoryPage(WebDriver driver) {
        super(driver);
    }

    public HistoryPage openByRecordNumber(String recordNumber) {
        go("/historias-clinicas/mascota/" + recordNumber);
        visible(By.xpath("//h1[contains(.,'Historia Clínica')]"));
        return this;
    }

    public HistoryPage openForPet(String petName) {
        go("/historias-clinicas");
        visible(By.xpath("//h1[contains(.,'Historias Clínicas')]"));
        var row = rowContaining(petName);
        row.findElement(By.tagName("button")).click();
        visible(By.xpath("//h1[contains(.,'Historia Clínica')]"));
        return this;
    }

    public void requestAiAnalysis() {
        clickText("Asistente IA");
        clickText("Analizar con IA");
    }
}
