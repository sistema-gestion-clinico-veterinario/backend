package veterinaria.vargasvet.exception;

import java.time.LocalDateTime;

public class AccountClosedException extends RuntimeException {

    private final Integer usuarioId;
    private final Integer companyId;
    private final LocalDateTime reactivableHasta;

    public AccountClosedException(Integer usuarioId, Integer companyId, LocalDateTime reactivableHasta) {
        super("Tu cuenta está cerrada");
        this.usuarioId = usuarioId;
        this.companyId = companyId;
        this.reactivableHasta = reactivableHasta;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public LocalDateTime getReactivableHasta() {
        return reactivableHasta;
    }
}
