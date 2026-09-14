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
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.domain.enums.RolePurpose;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ApoderadoRepository apoderadoRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (apoderadoRepository.existsByUserId(usuario.getId()) && !empleadoRepository.existsByUserId(usuario.getId())
                && usuario.getUsuariosPorRol().isEmpty()) {
            throw new UsernameNotFoundException("Los apoderados sin rol no tienen acceso al sistema");
        }

        if (!usuario.isActivo()) {
            throw new UsernameNotFoundException("El usuario está inactivo");
        }

        boolean esSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> upr.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN);
        if (!esSuperAdmin && usuario.getCompany() != null && !usuario.getCompany().isActivo()) {
            throw new UsernameNotFoundException("La empresa está desactivada. Contacta al administrador del sistema.");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (UsuarioPorRol upr : usuario.getUsuariosPorRol()) {
            authorities.add(new SimpleGrantedAuthority(upr.getRol().getName()));
        }

        return new User(usuario.getUsername(), usuario.getPassword(), new ArrayList<>(authorities));
    }
}
