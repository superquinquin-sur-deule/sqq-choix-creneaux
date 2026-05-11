package fr.sqq.choixcreneaux.domain.model;

import fr.sqq.choixcreneaux.domain.exception.AlreadyRegisteredException;
import fr.sqq.choixcreneaux.domain.exception.CampaignNotOpenException;
import fr.sqq.choixcreneaux.domain.exception.SlotFullException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotTest {

    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final Cooperator COOP = new Cooperator(UUID.randomUUID(), "a@b.com", "A", "B", null, "1", null);
    private static final Campaign OPEN = new Campaign(CampaignStatus.OPEN, LocalDate.of(2026, 5, 18), LocalDate.of(2015, 12, 28));
    private static final Campaign CLOSED = new Campaign(CampaignStatus.CLOSED, LocalDate.of(2026, 5, 18), LocalDate.of(2015, 12, 28));

    private Slot slotWith(int registrations, int min, int max) {
        List<SlotRegistration> regs = new ArrayList<>();
        for (int i = 0; i < registrations; i++) {
            regs.add(new SlotRegistration(UUID.randomUUID(), SLOT_ID, UUID.randomUUID(), Instant.now()));
        }
        return Slot.rehydrate(SLOT_ID, Week.A, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0),
                min, max, null, 0, regs, SlotStatus.NEEDS_PEOPLE);
    }

    @Test
    void register_adds_cooperator() {
        var slot = slotWith(0, 1, 3);
        slot.register(COOP, OPEN);
        assertThat(slot.hasCooperator(COOP.id())).isTrue();
        assertThat(slot.registrationCount()).isEqualTo(1);
    }

    @Test
    void register_rejects_when_full() {
        var slot = slotWith(3, 1, 3);
        assertThatThrownBy(() -> slot.register(COOP, OPEN))
                .isInstanceOf(SlotFullException.class);
    }

    @Test
    void register_rejects_when_campaign_closed() {
        var slot = slotWith(0, 1, 3);
        assertThatThrownBy(() -> slot.register(COOP, CLOSED))
                .isInstanceOf(CampaignNotOpenException.class);
    }

    @Test
    void register_rejects_duplicate() {
        var slot = slotWith(0, 1, 3);
        slot.register(COOP, OPEN);
        assertThatThrownBy(() -> slot.register(COOP, OPEN))
                .isInstanceOf(AlreadyRegisteredException.class);
    }

    @Test
    void register_refreshes_status_to_OPEN_when_min_reached() {
        var slot = slotWith(1, 2, 5);
        slot.register(COOP, OPEN);
        assertThat(slot.status()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    void register_refreshes_status_to_FULL_when_max_reached() {
        var slot = slotWith(2, 1, 3);
        slot.register(COOP, OPEN);
        assertThat(slot.status()).isEqualTo(SlotStatus.FULL);
    }

    @Test
    void unregister_removes_cooperator() {
        var slot = slotWith(0, 1, 3);
        slot.register(COOP, OPEN);
        assertThat(slot.unregister(COOP.id())).isTrue();
        assertThat(slot.hasCooperator(COOP.id())).isFalse();
    }

    @Test
    void admin_assign_bypasses_campaign() {
        var slot = slotWith(2, 2, 5);
        slot.adminAssign(COOP);
        assertThat(slot.hasCooperator(COOP.id())).isTrue();
    }

    @Test
    void admin_assign_still_rejects_full() {
        var slot = slotWith(3, 1, 3);
        assertThatThrownBy(() -> slot.adminAssign(COOP)).isInstanceOf(SlotFullException.class);
    }

    @Test
    void admin_assign_is_idempotent() {
        var slot = slotWith(0, 1, 3);
        slot.adminAssign(COOP);
        slot.adminAssign(COOP);
        assertThat(slot.registrationCount()).isEqualTo(1);
    }
}
