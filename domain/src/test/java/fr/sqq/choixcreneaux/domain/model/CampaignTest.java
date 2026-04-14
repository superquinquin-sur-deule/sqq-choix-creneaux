package fr.sqq.choixcreneaux.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class CampaignTest {
    private static final LocalDate WEEK_A_REF = LocalDate.of(2015, 12, 28);
    private static final LocalDate STORE_OPENING = LocalDate.of(2026, 5, 18);

    @Test
    void firstOccurrenceAfterOpening_returns_correct_monday_for_each_week() {
        var campaign = new Campaign(null, CampaignStatus.OPEN, null, null, STORE_OPENING, WEEK_A_REF);
        assertThat(campaign.firstMondayAfterOpening(Week.C)).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(campaign.firstMondayAfterOpening(Week.D)).isEqualTo(LocalDate.of(2026, 5, 25));
        assertThat(campaign.firstMondayAfterOpening(Week.A)).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(campaign.firstMondayAfterOpening(Week.B)).isEqualTo(LocalDate.of(2026, 6, 8));
    }
}
