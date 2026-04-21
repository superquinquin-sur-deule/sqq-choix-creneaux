package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.application.query.GetSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GetSlotsQueryHandlerTest {
    private SlotRepository slotRepo;
    private GetSlotsQuery.Handler handler;

    private static Slot slot(String id, int min, int max, int registrations, SlotStatus status) {
        UUID slotId = UUID.fromString(id);
        List<SlotRegistration> regs = new ArrayList<>();
        for (int i = 0; i < registrations; i++) {
            regs.add(new SlotRegistration(UUID.randomUUID(), slotId, UUID.randomUUID(), Instant.now()));
        }
        return Slot.rehydrate(slotId, Week.A, DayOfWeek.MONDAY,
                LocalTime.of(8, 15), LocalTime.of(11, 0), min, max, null, 0, regs, status);
    }

    @BeforeEach
    void setUp() {
        slotRepo = Mockito.mock(SlotRepository.class);
        handler = new GetSlotsQuery.Handler(slotRepo);
    }

    @Test
    void slot_under_minimum_has_NEEDS_PEOPLE_status() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5, 2, SlotStatus.NEEDS_PEOPLE);
        when(slotRepo.findAll()).thenReturn(List.of(s1));
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
        assertThat(result.getFirst().registrationCount()).isEqualTo(2);
    }

    @Test
    void open_slot_becomes_LOCKED_when_another_slot_is_under_minimum() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5, 4, SlotStatus.OPEN);
        var s2 = slot("00000000-0000-0000-0000-000000000002", 4, 5, 2, SlotStatus.NEEDS_PEOPLE);
        when(slotRepo.findAll()).thenReturn(List.of(s1, s2));
        var result = handler.handle(new GetSlotsQuery());
        var r1 = result.stream().filter(r -> r.slot().id().equals(s1.id())).findFirst().orElseThrow();
        var r2 = result.stream().filter(r -> r.slot().id().equals(s2.id())).findFirst().orElseThrow();
        assertThat(r1.status()).isEqualTo(SlotStatus.LOCKED);
        assertThat(r2.status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }

    @Test
    void all_slots_OPEN_when_all_minimums_reached() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5, 4, SlotStatus.OPEN);
        var s2 = slot("00000000-0000-0000-0000-000000000002", 4, 5, 4, SlotStatus.OPEN);
        when(slotRepo.findAll()).thenReturn(List.of(s1, s2));
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result).allMatch(r -> r.status() == SlotStatus.OPEN);
    }

    @Test
    void slot_at_max_capacity_is_FULL() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5, 5, SlotStatus.FULL);
        when(slotRepo.findAll()).thenReturn(List.of(s1));
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.FULL);
    }

    @Test
    void slot_with_no_registrations_has_count_zero() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5, 0, SlotStatus.NEEDS_PEOPLE);
        when(slotRepo.findAll()).thenReturn(List.of(s1));
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result.getFirst().registrationCount()).isEqualTo(0);
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }
}
