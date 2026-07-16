package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new LinkedHashMap<>();

        try {
            Integer databaseCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (!Integer.valueOf(1).equals(databaseCheck)) {
                return unavailable(response);
            }

            response.put("status", "UP");
            response.put("database", "UP");
            return ResponseEntity.ok(response);
        } catch (DataAccessException exception) {
            return unavailable(response);
        }
    }

    private ResponseEntity<Map<String, String>> unavailable(Map<String, String> response) {
        response.put("status", "DOWN");
        response.put("database", "DOWN");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
