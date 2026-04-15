package fr.sqq.choixcreneaux.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class WeekTest {
    private static final LocalDate WEEK_A_REF = LocalDate.of(2015, 12, 28);

    @Test
    void week_a_reference_date_is_week_a() {
        assertThat(Week.fromDate(WEEK_A_REF, WEEK_A_REF)).isEqualTo(Week.A);
    }

    @Test
    void one_week_after_reference_is_week_b() {
        assertThat(Week.fromDate(WEEK_A_REF.plusWeeks(1), WEEK_A_REF)).isEqualTo(Week.B);
    }

    @Test
    void four_weeks_after_reference_cycles_back_to_a() {
        assertThat(Week.fromDate(WEEK_A_REF.plusWeeks(4), WEEK_A_REF)).isEqualTo(Week.A);
    }

    @Test
    void april_14_2026_is_week_b() {
        assertThat(Week.fromDate(LocalDate.of(2026, 4, 14), WEEK_A_REF)).isEqualTo(Week.B);
    }

    @Test
    void may_18_2026_is_week_c() {
        assertThat(Week.fromDate(LocalDate.of(2026, 5, 18), WEEK_A_REF)).isEqualTo(Week.C);
    }
}
