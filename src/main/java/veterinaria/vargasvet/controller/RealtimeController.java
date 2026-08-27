package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.service.RealtimeTicketService;

@RestController
@RequestMapping("/realtime")
@RequiredArgsConstructor
public class RealtimeController {
    private final RealtimeTicketService ticketService;

    @PostMapping("/ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> issueTicket(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket de conexión generado",
                new TicketResponse(ticketService.issue(authentication))));
    }

    public record TicketResponse(String ticket) {}
}
