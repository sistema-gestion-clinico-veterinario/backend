package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.ProtectedPersonalData;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class SensitiveDataResponseAdvice implements ResponseBodyAdvice<Object> {

    private final AccesoValidator accesoValidator;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        Object data = body instanceof ApiResponse<?> api ? api.getData() : body;
        if (data != null) {
            visit(data, new HashMap<>());
        }
        return body;
    }

    private void visit(Object data, Map<String, Boolean> decisions) {
        if (data instanceof ProtectedPersonalData protectedData) {
            boolean full = decisions.computeIfAbsent(protectedData.authorizingView(), this::canViewFull);
            if (!full) {
                protectedData.maskSensitiveData();
            }
        } else if (data instanceof Iterable<?> items) {
            items.forEach(item -> {
                if (item instanceof ProtectedPersonalData) {
                    visit(item, decisions);
                }
            });
        }
    }

    private boolean canViewFull(String view) {
        return accesoValidator.can(view, "ESCRIBIR") || accesoValidator.can(view, "MODIFICAR");
    }
}
