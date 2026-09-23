package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaContacto;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository;

import java.util.Optional;

/** Resuelve y actualiza el telefono/direccion de una persona PARA UNA empresa puntual -
 * cada relacion (usuario, empresa) tiene su propio contacto, igual que ya pasa con la
 * contraseña (ver UsuarioEmpresaCredencial). Servicio compartido en vez de repetir esta
 * logica en cada Service que hoy lee/escribe Usuario.telefono/direccion. */
@Service
@RequiredArgsConstructor
public class UsuarioContactoService {

    private final UsuarioEmpresaContactoRepository contactoRepository;
    private final CompanyRepository companyRepository;

    public Optional<UsuarioEmpresaContacto> resolve(Integer usuarioId, Integer companyId) {
        return companyId == null
                ? contactoRepository.findByUsuarioIdAndCompanyIsNull(usuarioId)
                : contactoRepository.findByUsuarioIdAndCompanyId(usuarioId, companyId);
    }

    public String telefono(Integer usuarioId, Integer companyId) {
        return resolve(usuarioId, companyId).map(UsuarioEmpresaContacto::getTelefono).orElse(null);
    }

    public String direccion(Integer usuarioId, Integer companyId) {
        return resolve(usuarioId, companyId).map(UsuarioEmpresaContacto::getDireccion).orElse(null);
    }

    /** Usada al registrar a la persona en ESA empresa por primera vez - siempre crea una
     * fila nueva (se asume que todavia no existe, ver existsByUsuarioIdAndCompanyId en el
     * llamador antes de decidir si crear credencial+contacto o reutilizar). */
    @Transactional
    public void crear(Usuario usuario, Company company, String telefono, String direccion) {
        UsuarioEmpresaContacto contacto = new UsuarioEmpresaContacto();
        contacto.setUsuario(usuario);
        contacto.setCompany(company);
        contacto.setTelefono(telefono);
        contacto.setDireccion(direccion);
        contacto.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
        contactoRepository.save(contacto);
    }

    /** Actualiza el contacto de ESA empresa puntual - si por alguna razon todavia no
     * existe la fila (ej. datos de antes de esta migracion que no quedaron cubiertos por
     * el backfill), la crea en vez de fallar. Solo toca los campos no-null del llamador,
     * igual que hacian los Service al escribir directo sobre Usuario. */
    @Transactional
    public void actualizar(Usuario usuario, Company company, String telefono, String direccion) {
        if (telefono == null && direccion == null) {
            return;
        }
        Integer companyId = company != null ? company.getId() : null;
        UsuarioEmpresaContacto contacto = resolve(usuario.getId(), companyId).orElseGet(() -> {
            UsuarioEmpresaContacto nuevo = new UsuarioEmpresaContacto();
            nuevo.setUsuario(usuario);
            nuevo.setCompany(company);
            nuevo.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
            return nuevo;
        });
        if (telefono != null) contacto.setTelefono(telefono);
        if (direccion != null) contacto.setDireccion(direccion);
        contacto.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        contactoRepository.save(contacto);
    }

    /** Igual que actualizar(Usuario, Company, ...) pero para llamadores que solo tienen el
     * id de la empresa a mano (ej. SecurityUtils.getCurrentCompanyId()), sin haber cargado
     * la entidad Company - evita que cada caller tenga que buscarla primero solo para esto. */
    @Transactional
    public void actualizar(Usuario usuario, Integer companyId, String telefono, String direccion) {
        Company company = companyId != null ? companyRepository.getReferenceById(companyId) : null;
        actualizar(usuario, company, telefono, direccion);
    }
}
