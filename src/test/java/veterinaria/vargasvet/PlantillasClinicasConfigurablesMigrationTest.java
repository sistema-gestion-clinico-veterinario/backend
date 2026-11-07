package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillasClinicasConfigurablesMigrationTest {

    @Test
    void laMigracionVersionaLaFichaYConservaLasConsultasHistoricas() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V130__Plantillas_Clinicas_Configurables.sql"));

        assertThat(sql)
                .contains("CREATE TABLE clinical_concept")
                .contains("CREATE TABLE clinical_template")
                .contains("CREATE TABLE clinical_template_section")
                .contains("CREATE TABLE clinical_template_field")
                .contains("CREATE TABLE consultation_field_answer")
                .contains("CONSTRAINT uk_clinical_template_version UNIQUE (company_id, template_key, version)")
                .contains("WHERE status = 'PUBLISHED'")
                .contains("ADD COLUMN IF NOT EXISTS clinical_template_id")
                .contains("CONSTRAINT ck_consultation_field_answer_single_value")
                .contains("UPDATE consulta consultation")
                .contains("AND consultation.clinical_template_id IS NULL");
    }
}
