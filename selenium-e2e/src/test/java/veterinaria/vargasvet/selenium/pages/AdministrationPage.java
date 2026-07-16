package veterinaria.vargasvet.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class AdministrationPage extends BasePage {
    public AdministrationPage(WebDriver driver) {
        super(driver);
    }

    public void toggleCompany(String companyName, boolean activate) {
        go("/company");
        visible(By.xpath("//h1[contains(.,'Administración de empresas')]"));
        WebElement row = rowWithExactText(companyName);
        row.findElement(By.cssSelector("button[aria-label='Abrir acciones']")).click();
        pause();
        String action = activate ? "Activar" : "Desactivar";
        click(By.xpath("//*[@role='menuitem']//*[self::a or self::button][contains(normalize-space(.)," + literal(action) + ")]"
                + " | //*[self::a or self::button][@role='menuitem' and contains(normalize-space(.)," + literal(action) + ")]"));
        visible(By.xpath("//h3[contains(.," + literal(activate ? "Activar empresa" : "Desactivar empresa") + ")]"));
        click(By.xpath("//div[contains(@class,'fixed')]//button[normalize-space()=" + literal(activate ? "Activar" : "Desactivar") + "]"));
        wait.until(driver -> rowWithExactText(companyName).getText().contains(activate ? "Activo" : "Inactivo"));
    }

    public void toggleRole(String roleName, boolean activate) {
        go("/roles");
        visible(By.xpath("//h1[contains(.,'Gestión de Roles')]"));
        String label = roleName.replace("ROLE_", "").replace('_', ' ');
        click(By.xpath("//button[contains(normalize-space(.)," + literal(label) + ")]"));
        String current = activate ? "Inactivo" : "Activo";
        click(By.xpath("//button[contains(normalize-space(.)," + literal(current) + ")]"));
        click(By.xpath("//div[contains(@class,'fixed')]//button[contains(normalize-space(.),'Confirmar')]"));
        wait.until(driver -> containsText(activate ? "Activo" : "Inactivo"));
    }

    public void toggleEmployee(String employeeName, boolean activate) {
        go("/empleados");
        visible(By.xpath("//h1[contains(.,'Gestión de Personal')]"));
        WebElement row = rowContaining(employeeName);
        String expectedCurrent = activate ? "Inactivo" : "Activo";
        WebElement status = row.findElement(By.xpath(".//button[contains(normalize-space(.)," + literal(expectedCurrent) + ")]"));
        status.click();
        pause();
        clickText("Confirmar");
        wait.until(driver -> rowContaining(employeeName).getText().contains(activate ? "Activo" : "Inactivo"));
    }

    public void toggleService(String serviceName, boolean activate) {
        go("/complementario");
        visible(By.xpath("//h1[contains(.,'Configuración Complementaria')]"));
        click(By.xpath("//button[contains(normalize-space(.),'Servicios')]"));
        WebElement row = rowContaining(serviceName);
        String current = activate ? "Inactivo" : "Activo";
        row.findElement(By.xpath(".//button[contains(normalize-space(.)," + literal(current) + ")]"))
                .click();
        pause();
        click(By.xpath("//div[contains(@class,'fixed')]//button[contains(normalize-space(.),'Confirmar')]"));
        wait.until(driver -> rowContaining(serviceName).getText().contains(activate ? "Activo" : "Inactivo"));
    }
}
