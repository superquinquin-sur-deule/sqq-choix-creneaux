package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.choixcreneaux.application.query.GetSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GetSlotsQueryHandlerTest {
    private SlotTemplateRepository slotRepo;
    private GetSlotsQueryHandler handler;

    private static SlotTemplate slot(String id, int min, int max) {
        return new SlotTemplate(UUID.fromString(id), Week.A, DayOfWeek.MONDAY,
                LocalTime.of(8, 15), LocalTime.of(11, 0), min, max, null);
    }

    @BeforeEach
    void setUp() {
        slotRepo = Mockito.mock(SlotTemplateRepository.class);
        handler = new GetSlotsQueryHandler(slotRepo);
    }

    @Test
    void slot_under_minimum_has_NEEDS_PEOPLE_status() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5);
        when(slotRepo.findAll()).thenReturn(List.of(s1));
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of(s1.id(), 2));
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
        assertThat(result.getFirst().registrationCount()).isEqualTo(2);
    }

    @Test
    void slot_at_minimum_is_LOCKED_when_another_slot_is_under_minimum() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5);
        var s2 = slot("00000000-0000-0000-0000-000000000002", 4, 5);
        when(slotRepo.findAll()).thenReturn(List.of(s1, s2));
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of(s1.id(), 4, s2.id(), 2));
        var result = handler.handle(new GetSlotsQuery());
        var s1Result = result.stream().filter(r -> r.slot().id().equals(s1.id())).findFirst().orElseThrow();
        var s2Result = result.stream().filter(r -> r.slot().id().equals(s2.id())).findFirst().orElseThrow();
        assertThat(s1Result.status()).isEqualTo(SlotStatus.LOCKED);
        assertThat(s2Result.status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }

    @Test
    void all_slots_OPEN_when_all_minimums_reached() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5);
        var s2 = slot("00000000-0000-0000-0000-000000000002", 4, 5);
        when(slotRepo.findAll()).thenReturn(List.of(s1, s2));
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of(s1.id(), 4, s2.id(), 4));
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result).allMatch(r -> r.status() == SlotStatus.OPEN);
    }

    @Test
    void slot_at_max_capacity_is_FULL() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5);
        when(slotRepo.findAll()).thenReturn(List.of(s1));
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of(s1.id(), 5));
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.FULL);
    }

    @Test
    void slot_with_no_registrations_has_count_zero() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5);
        when(slotRepo.findAll()).thenReturn(List.of(s1));
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of());
        var result = handler.handle(new GetSlotsQuery());
        assertThat(result.getFirst().registrationCount()).isEqualTo(0);
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }
}
