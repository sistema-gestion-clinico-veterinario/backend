package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "company_exceptions")
public class CompanyException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "exception_date", nullable = false)
    private LocalDate date;

    @Column(name = "description")
    private String description;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = false; // Por defecto asumimos que la excepción es un cierre (feriado)
}
