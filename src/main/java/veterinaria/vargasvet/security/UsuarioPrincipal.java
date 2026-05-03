package veterinaria.vargasvet.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Collection;
import java.util.Collections;

@Data
public class UsuarioPrincipal implements UserDetails {
    private Integer id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Integer companyId;

    public UsuarioPrincipal(Integer id, String email, String password, Collection<? extends GrantedAuthority> authorities, Integer companyId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.companyId = companyId;
    }

    public static UsuarioPrincipal create(Usuario usuario) {
        java.util.List<GrantedAuthority> authorities = usuario.getRoles() != null
                ? usuario.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .collect(java.util.stream.Collectors.toList())
                : Collections.emptyList();

        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;

        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPassword(),
                authorities,
                companyId
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
