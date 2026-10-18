package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.request.BackupExecutionEventRequest;
import veterinaria.vargasvet.security.BackupReportTokenVerifier;
import veterinaria.vargasvet.service.BackupExecutionService;

@RestController
@RequestMapping("/internal/backups/events")
@RequiredArgsConstructor
public class BackupExecutionEventController {

    private final BackupExecutionService service;
    private final BackupReportTokenVerifier tokenVerifier;

    @PostMapping
    public ResponseEntity<Void> record(
            @RequestHeader(value = "X-Backup-Report-Token", required = false) String token,
            @Valid @RequestBody BackupExecutionEventRequest request) {
        if (!tokenVerifier.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.record(request);
        return ResponseEntity.accepted().build();
    }
}
