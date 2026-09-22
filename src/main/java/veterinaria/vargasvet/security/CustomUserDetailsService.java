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
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.domain.enums.RolePurpose;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** No queda conectado a ningún AuthenticationProvider real (login() valida
 * manualmente contra UsuarioEmpresaCredencial) - verificado que nada más en el
 * código inyecta esta clase. Se mantiene compilando por si algo externo a este
 * módulo llegara a necesitar el contrato estándar de Spring Security, pero no es
 * parte del flujo de autenticación real y una sola contraseña "por defecto" aquí
 * no tiene sentido post-migración a credenciales por empresa. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioEmpresaCredencialRepository credencialRepository;

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

        String password = credencialRepository.findAllByUsuarioId(usuario.getId()).stream()
                .findFirst()
                .map(veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial::getPassword)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario no tiene credencial configurada"));
        return new User(usuario.getUsername(), password, new ArrayList<>(authorities));
    }
}
