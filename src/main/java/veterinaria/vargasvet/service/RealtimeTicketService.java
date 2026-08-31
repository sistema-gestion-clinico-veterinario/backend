package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.domain.entity.RealtimeTicket;
import veterinaria.vargasvet.repository.RealtimeTicketRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeTicketService {
    private final TokenProvider tokenProvider;
    private final RealtimeTicketRepository realtimeTicketRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;

    @Transactional
    public String issue(Authentication authentication) {
        UsuarioPrincipal principal = authentication.getPrincipal() instanceof UsuarioPrincipal value ? value : null;
        Integer companyId = principal != null ? principal.getCompanyId() : null;
        Integer userId = principal != null ? principal.getId() : null;
        if (userId == null) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "La sesión no contiene una identidad válida");
        }
        TokenProvider.IssuedRealtimeTicket issued = tokenProvider.createRealtimeTicket(
                userId, authentication.getName(),
                authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList(), companyId,
                principal.getActiveRoleId(), principal.getActiveRoleScope(),
                principal.getActiveRolePurpose(), principal.getPermissionVersion());
        realtimeTicketRepository.save(RealtimeTicket.builder()
                .jti(issued.jti())
                .usuarioId(userId)
                .issuedAt(issued.issuedAt())
                .expiresAt(issued.expiresAt())
                .build());
        return issued.token();
    }

    @Transactional
    public Optional<Authentication> consume(String ticket) {
        try {
            TokenProvider.RealtimeTicketDetails details = tokenProvider.getRealtimeTicketDetails(ticket);
            UsuarioPrincipal principal = details.authentication().getPrincipal() instanceof UsuarioPrincipal value
                    ? value : null;
            if (principal == null || principal.getActiveRoleId() == null) {
                return Optional.empty();
            }
            boolean roleIsCurrent = usuarioPorRolRepository
                    .findActiveAssignmentByUsuarioIdAndRoleId(principal.getId(), principal.getActiveRoleId())
                    .filter(assignment -> assignment.getRol().getPermissionVersion() == principal.getPermissionVersion())
                    .isPresent();
            if (!roleIsCurrent) {
                return Optional.empty();
            }
            int consumed = realtimeTicketRepository.consumeOnce(
                    details.jti(), veterinaria.vargasvet.util.AppClock.instantNow());
            if (consumed != 1) {
                log.warn("Ticket WebSocket reutilizado, expirado o inexistente (jti={}, usuario={})",
                        details.jti(), details.authentication().getName());
            }
            return consumed == 1 ? Optional.of(details.authentication()) : Optional.empty();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    @Scheduled(fixedDelayString = "${app.realtime-ticket.cleanup-ms:300000}")
    @Transactional
    public void deleteExpiredTickets() {
        realtimeTicketRepository.deleteByExpiresAtBefore(veterinaria.vargasvet.util.AppClock.instantNow());
    }
}
