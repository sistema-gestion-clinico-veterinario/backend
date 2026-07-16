package veterinaria.vargasvet.selenium.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.WebDriver;

public abstract class BaseSeleniumTest {
    protected WebDriver driver;
    protected Fixtures fixtures;
    protected final EvidenceManager evidence = new EvidenceManager();

    @BeforeEach
    void setUpSelenium(TestInfo testInfo) {
        fixtures = new FixtureClient().load();
        evidence.startVideo(testInfo.getDisplayName());
        driver = DriverFactory.create();
    }

    @AfterEach
    void tearDownSelenium(TestInfo testInfo) {
        try {
            if (driver != null) {
                evidence.screenshot(driver, testInfo.getDisplayName(), "final");
            }
        } finally {
            if (driver != null) {
                driver.quit();
            }
            evidence.stopVideo();
        }
    }

    protected WebDriver secondaryDriver() {
        return DriverFactory.create();
    }
}
