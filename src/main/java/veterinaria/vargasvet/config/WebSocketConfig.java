package veterinaria.vargasvet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.RealtimeTicketService;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeTicketService ticketService;

    @Value("${cors.allowed-origins:https://systemvetfrontend.vercel.app}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
              .setHeartbeatValue(new long[]{10000, 10000})
              .setTaskScheduler(heartbeatScheduler());
        
        config.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    public TaskScheduler heartbeatScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new);
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins);
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String ticket = accessor.getFirstNativeHeader("ticket");
                    var authentication = ticketService.consume(ticket)
                            .orElseThrow(() -> new AccessDeniedException("Ticket WebSocket inválido o expirado"));
                    accessor.setUser(authentication);
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    if (!(accessor.getUser() instanceof org.springframework.security.core.Authentication authentication)
                            || !authentication.isAuthenticated()) {
                        throw new AccessDeniedException("WebSocket no autenticado");
                    }
                    authorizeDestination(accessor.getDestination(), authentication);
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    throw new AccessDeniedException("El envío de mensajes WebSocket no está habilitado");
                }
                return message;
            }
        });
    }

    private void authorizeDestination(String destination,
                                      org.springframework.security.core.Authentication authentication) {
        if (destination == null || !destination.startsWith("/topic/")) {
            throw new AccessDeniedException("Destino WebSocket no permitido");
        }
        boolean superAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (destination.startsWith("/topic/audit-logs") && !superAdmin && !admin) {
            throw new AccessDeniedException("Sin acceso a eventos de auditoría");
        }
        Integer companyId = authentication.getPrincipal() instanceof UsuarioPrincipal principal
                ? principal.getCompanyId() : null;
        String suffix = destination.substring(destination.lastIndexOf('/') + 1);
        if (suffix.matches("\\d+") && !superAdmin
                && (companyId == null || !suffix.equals(companyId.toString()))) {
            throw new AccessDeniedException("Sin acceso a eventos de otra empresa");
        }
        if (destination.equals("/topic/audit-logs") && !superAdmin) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede suscribirse a auditoría global");
        }
    }
}
