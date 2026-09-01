package veterinaria.vargasvet.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Collection;
import java.util.Collections;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

@Data
public class UsuarioPrincipal implements UserDetails {
    private Integer id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Integer companyId;
    private Integer activeRoleId;
    private RoleScope activeRoleScope;
    private RolePurpose activeRolePurpose;
    private long permissionVersion;
    private long credentialsVersion;

    public UsuarioPrincipal(Integer id, String email, String password, Collection<? extends GrantedAuthority> authorities, Integer companyId) {
        this(id, email, password, authorities, companyId, null, null, null, 0L);
    }

    public UsuarioPrincipal(Integer id, String email, String password,
                            Collection<? extends GrantedAuthority> authorities, Integer companyId,
                            Integer activeRoleId, RoleScope activeRoleScope,
                            RolePurpose activeRolePurpose, long permissionVersion) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.companyId = companyId;
        this.activeRoleId = activeRoleId;
        this.activeRoleScope = activeRoleScope;
        this.activeRolePurpose = activeRolePurpose;
        this.permissionVersion = permissionVersion;
    }

    public static UsuarioPrincipal create(Usuario usuario) {
        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        if (usuario.getUsuariosPorRol() != null) {
            for (veterinaria.vargasvet.domain.entity.UsuarioPorRol upr : usuario.getUsuariosPorRol()) {
                authorities.add(new SimpleGrantedAuthority(upr.getRol().getName()));
            }
        }

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
