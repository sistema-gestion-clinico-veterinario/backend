package veterinaria.vargasvet.selenium.support;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static WebDriver create() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--lang=es-PE",
                "--window-size=1440,1000",
                "--disable-notifications",
                "--disable-search-engine-choice-screen",
                "--no-default-browser-check"
        );
        if (E2eConfig.HEADLESS) {
            options.addArguments("--headless=new");
        }
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(20));
        if (!E2eConfig.HEADLESS) {
            driver.manage().window().maximize();
        }
        return driver;
    }
}
