package veterinaria.vargasvet.selenium.support;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class EvidenceManager {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Path SCREENSHOT_DIR = Path.of("target", "evidence", "screenshots");
    private static final Path VIDEO_DIR = Path.of("target", "evidence", "videos");

    private Process recorder;
    private Path videoPath;

    public void startVideo(String testName) {
        if (!E2eConfig.VIDEO) {
            return;
        }
        try {
            Files.createDirectories(VIDEO_DIR);
            videoPath = VIDEO_DIR.resolve(fileName(testName) + ".mp4");
            recorder = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-f", "gdigrab",
                    "-framerate", "15",
                    "-i", "desktop",
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-pix_fmt", "yuv420p",
                    videoPath.toAbsolutePath().toString()
            ).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo iniciar FFmpeg para grabar " + testName, e);
        }
    }

    public Path screenshot(WebDriver driver, String testName, String suffix) {
        if (!(driver instanceof TakesScreenshot camera)) {
            return null;
        }
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            Path destination = SCREENSHOT_DIR.resolve(fileName(testName + "-" + suffix) + ".png");
            Files.copy(camera.getScreenshotAs(OutputType.FILE).toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la captura de " + testName, e);
        }
    }

    public void stopVideo() {
        if (recorder == null) {
            return;
        }
        try {
            if (recorder.isAlive()) {
                recorder.getOutputStream().write("q\n".getBytes(StandardCharsets.UTF_8));
                recorder.getOutputStream().flush();
                recorder.getOutputStream().close();
            }
            if (!recorder.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                recorder.destroy();
            }
            if (recorder.isAlive() && !recorder.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                recorder.destroyForcibly();
            }
        } catch (IOException e) {
            recorder.destroy();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recorder.destroyForcibly();
        } finally {
            recorder = null;
        }
    }

    public Path getVideoPath() {
        return videoPath;
    }

    private String fileName(String value) {
        String safe = value.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return safe + "-" + LocalDateTime.now().format(STAMP);
    }
}
