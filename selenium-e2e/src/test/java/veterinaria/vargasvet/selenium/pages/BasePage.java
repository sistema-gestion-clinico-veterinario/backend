package veterinaria.vargasvet.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import veterinaria.vargasvet.selenium.support.E2eConfig;

import java.time.Duration;
import java.util.List;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    public void go(String path) {
        driver.get(E2eConfig.BASE_URL + path);
        pause();
    }

    public WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void click(By locator) {
        clickable(locator).click();
        pause();
    }

    public void clickText(String text) {
        click(By.xpath("//*[self::button or self::a or @role='button'][contains(normalize-space(.)," + literal(text) + ")]"));
    }

    public void type(By locator, String value) {
        WebElement element = visible(locator);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(value);
        pause();
    }

    public void setInputValue(By locator, String value) {
        WebElement element = visible(locator);
        ((JavascriptExecutor) driver).executeScript("""
                const input = arguments[0];
                const value = arguments[1];
                const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                setter.call(input, value);
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                """, element, value);
        pause();
    }

    public void selectText(By locator, String text) {
        new Select(visible(locator)).selectByVisibleText(text);
        pause();
    }

    public void selectContaining(By locator, String text) {
        Select select = new Select(visible(locator));
        WebElement option = select.getOptions().stream()
                .filter(item -> item.getText().contains(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe la opción que contiene: " + text));
        select.selectByValue(option.getAttribute("value"));
        pause();
    }

    public boolean containsText(String text) {
        try {
            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text));
            return true;
        } catch (TimeoutException ignored) {
            return false;
        }
    }

    public boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    public List<WebElement> all(By locator) {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    public WebElement rowContaining(String text) {
        return visible(By.xpath("//*[self::tr or @role='row'][contains(normalize-space(.)," + literal(text) + ")]"));
    }

    public WebElement rowWithExactText(String text) {
        return visible(By.xpath("//*[self::tr or @role='row'][.//*[normalize-space()=" + literal(text) + "]]"));
    }

    protected void pause() {
        if (E2eConfig.SLOW_MO <= 0) {
            return;
        }
        try {
            Thread.sleep(E2eConfig.SLOW_MO);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected static String literal(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }
}
