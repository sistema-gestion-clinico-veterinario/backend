package veterinaria.vargasvet.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThesisPerformanceOperationTest {

    @Test
    void resolvesOnlyCriticalClinicalOperations() {
        assertThat(ThesisPerformanceOperation.resolve("GET", "/medical-records"))
                .contains(ThesisPerformanceOperation.SEARCH_MEDICAL_RECORDS);
        assertThat(ThesisPerformanceOperation.resolve("GET", "/medical-records/{id}"))
                .contains(ThesisPerformanceOperation.RETRIEVE_MEDICAL_RECORD);
        assertThat(ThesisPerformanceOperation.resolve("PATCH", "/appointments/{id}/start"))
                .contains(ThesisPerformanceOperation.START_CLINICAL_ATTENTION);
        assertThat(ThesisPerformanceOperation.resolve("PUT", "/consultations/{id}"))
                .contains(ThesisPerformanceOperation.SAVE_CLINICAL_ATTENTION);
        assertThat(ThesisPerformanceOperation.resolve("PATCH", "/consultations/{id}/close"))
                .contains(ThesisPerformanceOperation.SAVE_CLINICAL_ATTENTION);

        assertThat(ThesisPerformanceOperation.resolve("GET", "/appointments")).isEmpty();
        assertThat(ThesisPerformanceOperation.resolve("POST", "/medical-records")).isEmpty();
    }
}
