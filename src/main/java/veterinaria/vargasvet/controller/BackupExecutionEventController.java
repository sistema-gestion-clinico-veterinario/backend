package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.request.BackupExecutionEventRequest;
import veterinaria.vargasvet.service.BackupExecutionService;

@RestController
@RequestMapping("/internal/backups/events")
@RequiredArgsConstructor
public class BackupExecutionEventController {

    private final BackupExecutionService service;

    @PostMapping
    public ResponseEntity<Void> record(@Valid @RequestBody BackupExecutionEventRequest request) {
        service.record(request);
        return ResponseEntity.accepted().build();
    }
}
