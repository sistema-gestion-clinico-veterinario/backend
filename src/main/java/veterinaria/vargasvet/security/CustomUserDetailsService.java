package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        if (usuario.getApoderado() != null && usuario.getEmpleado() == null
                && usuario.getUsuariosPorRol().isEmpty()) {
            throw new UsernameNotFoundException("Los apoderados sin rol no tienen acceso al sistema");
        }

        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException("El usuario está inactivo");
        }

        boolean esSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> "ROLE_SUPER_ADMIN".equals(upr.getRol().getName()));
        if (!esSuperAdmin && usuario.getCompany() != null && !usuario.getCompany().isActivo()) {
            throw new UsernameNotFoundException("La empresa está desactivada. Contacta al administrador del sistema.");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (UsuarioPorRol upr : usuario.getUsuariosPorRol()) {
            authorities.add(new SimpleGrantedAuthority(upr.getRol().getName()));
        }

        return new User(usuario.getEmail(), usuario.getPassword(), new ArrayList<>(authorities));
    }
}
