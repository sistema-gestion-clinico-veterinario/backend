package veterinaria.vargasvet.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public final class LoginPage extends BasePage {
    private static final By EMAIL = By.id("email");
    private static final By PASSWORD = By.id("password");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        go("/login");
        visible(EMAIL);
        return this;
    }

    public void login(String email, String password) {
        type(EMAIL, email);
        type(PASSWORD, password);
        click(By.xpath("//button[contains(normalize-space(.),'Ingresar')]"));
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
    }

    public void loginExpectingError(String email, String password, String message) {
        type(EMAIL, email);
        type(PASSWORD, password);
        click(By.xpath("//button[contains(normalize-space(.),'Ingresar')]"));
        if (!containsText(message)) {
            throw new AssertionError("No apareció el rechazo esperado: " + message);
        }
    }
}
