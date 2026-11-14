package veterinaria.vargasvet.testutil;

import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.service.CompanyComplianceService;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.OwnerContactPolicy;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.service.impl.RecordatorioPreventivoServiceImpl;
import veterinaria.vargasvet.service.recordatorios.ComponedorDelCorreoPreventivo;
import veterinaria.vargasvet.service.recordatorios.PlanificadorDeRecordatoriosPreventivos;
import veterinaria.vargasvet.service.recordatorios.RegistroDeRecordatoriosEnviados;

public final class RecordatoriosTestFixture {
    private RecordatoriosTestFixture() { }

    public static RecordatorioPreventivoServiceImpl servicio(ControlPreventivoRepository controlRepository,
                                                              RecordatorioPreventivoRepository recordatorioRepository,
                                                              ApoderadoRepository apoderadoRepository,
                                                              EmailService emailService,
                                                              OwnerContactPolicy ownerContactPolicy,
                                                              PetOwnershipService petOwnershipService,
                                                              ConsentimientoDatosService consentimientoDatosService,
                                                              CompanyComplianceService complianceService) {
        return new RecordatorioPreventivoServiceImpl(
                new PlanificadorDeRecordatoriosPreventivos(controlRepository, recordatorioRepository, ownerContactPolicy,
                        petOwnershipService, consentimientoDatosService, complianceService),
                new ComponedorDelCorreoPreventivo(emailService),
                new RegistroDeRecordatoriosEnviados(recordatorioRepository, apoderadoRepository, controlRepository),
                emailService);
    }
}
