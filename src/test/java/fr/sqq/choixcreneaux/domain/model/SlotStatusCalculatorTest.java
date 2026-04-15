package fr.sqq.choixcreneaux.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SlotStatusCalculatorTest {

    @Test
    void count_at_or_above_max_is_FULL() {
        assertThat(SlotStatusCalculator.compute(4, 5, 5, false)).isEqualTo(SlotStatus.FULL);
        assertThat(SlotStatusCalculator.compute(4, 5, 6, false)).isEqualTo(SlotStatus.FULL);
        assertThat(SlotStatusCalculator.compute(4, 5, 5, true)).isEqualTo(SlotStatus.FULL);
    }

    @Test
    void count_below_min_is_NEEDS_PEOPLE() {
        assertThat(SlotStatusCalculator.compute(4, 5, 0, false)).isEqualTo(SlotStatus.NEEDS_PEOPLE);
        assertThat(SlotStatusCalculator.compute(4, 5, 3, true)).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }

    @Test
    void at_minimum_but_some_other_slot_under_is_LOCKED() {
        assertThat(SlotStatusCalculator.compute(4, 5, 4, true)).isEqualTo(SlotStatus.LOCKED);
    }

    @Test
    void at_minimum_with_no_other_slot_under_is_OPEN() {
        assertThat(SlotStatusCalculator.compute(4, 5, 4, false)).isEqualTo(SlotStatus.OPEN);
    }
}
