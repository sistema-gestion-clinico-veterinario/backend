package veterinaria.vargasvet.service.recordatorios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.RecordatorioPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoRecordatorio;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario.Aviso;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RegistroDeRecordatoriosEnviados {

    private final RecordatorioPreventivoRepository recordatorioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final ControlPreventivoRepository controlRepository;

    @Transactional
    public void registrar(AvisosParaPropietario propietario) {
        Apoderado apoderado = apoderadoRepository.getReferenceById(propietario.apoderadoId());
        LocalDateTime enviadoEn = AppClock.now();
        for (Aviso aviso : propietario.avisos()) {
            RecordatorioPreventivo registro = new RecordatorioPreventivo();
            registro.setApoderado(apoderado);
            registro.setControlPreventivo(controlRepository.getReferenceById(aviso.controlId()));
            registro.setTipoAviso(aviso.tipoAviso());
            registro.setFechaProgramada(aviso.fechaRecomendada());
            registro.setFechaEnvio(enviadoEn);
            registro.setEstado(EstadoRecordatorio.ENVIADO);
            registro.setCreatedBy("SYSTEM");
            registro.setUpdatedBy("SYSTEM");
            recordatorioRepository.save(registro);
        }
    }
}
