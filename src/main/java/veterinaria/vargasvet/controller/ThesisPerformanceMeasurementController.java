package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.ThesisPerformanceMeasurementResponse;
import veterinaria.vargasvet.service.ThesisPerformanceMeasurementService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/thesis/performance-measurements")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ThesisPerformanceMeasurementController {

    private final ThesisPerformanceMeasurementService measurementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThesisPerformanceMeasurementResponse>>> findOwnSession(
            @RequestParam UUID sessionId) {
        List<ThesisPerformanceMeasurementResponse> measurements = measurementService.findOwnSession(sessionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Mediciones de rendimiento recuperadas", measurements));
    }
}
