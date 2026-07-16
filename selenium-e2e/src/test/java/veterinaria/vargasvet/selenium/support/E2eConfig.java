package veterinaria.vargasvet.selenium.support;

public final class E2eConfig {
    public static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://127.0.0.1:4200");
    public static final String API_URL = System.getProperty("e2e.apiUrl", "http://127.0.0.1:8080/api/v1");
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("e2e.headless", "false"));
    public static final boolean VIDEO = Boolean.parseBoolean(System.getProperty("e2e.video", "false"));
    public static final long SLOW_MO = Long.parseLong(System.getProperty("e2e.slowMo", "150"));

    private E2eConfig() {
    }
}
