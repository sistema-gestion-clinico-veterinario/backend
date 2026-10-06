package veterinaria.vargasvet.exception;

public class GoogleAccountClosedException extends RuntimeException {
    private final Integer usuarioId;
    private final Integer companyId;
    private final String email;
    private final String slug;

    public GoogleAccountClosedException() {
        this(null, null, null, null);
    }

    public GoogleAccountClosedException(Integer usuarioId, Integer companyId, String email, String slug) {
        super("Cerraste tu cuenta en esta veterinaria");
        this.usuarioId = usuarioId;
        this.companyId = companyId;
        this.email = email;
        this.slug = slug;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public String getEmail() {
        return email;
    }

    public String getSlug() {
        return slug;
    }
}
