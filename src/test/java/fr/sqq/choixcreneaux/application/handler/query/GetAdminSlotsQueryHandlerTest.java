package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.application.query.GetAdminSlotsQuery;
import fr.sqq.choixcreneaux.application.query.RegistrantSummary;
import fr.sqq.choixcreneaux.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GetAdminSlotsQueryHandlerTest {

    private SlotRepository slotRepo;
    private CooperatorRepository cooperatorRepo;
    private GetAdminSlotsQuery.Handler handler;

    private static Slot slotWithCoops(String slotId, int min, int max, List<UUID> coopIds, SlotStatus status) {
        UUID id = UUID.fromString(slotId);
        List<SlotRegistration> regs = new ArrayList<>();
        for (UUID coopId : coopIds) {
            regs.add(new SlotRegistration(UUID.randomUUID(), id, coopId, Instant.parse("2026-01-01T00:00:00Z")));
        }
        return Slot.rehydrate(id, Week.A, DayOfWeek.MONDAY,
                LocalTime.of(8, 15), LocalTime.of(11, 0), min, max, null, 0, regs, status);
    }

    private static Cooperator coop(String id, String firstName, String lastName) {
        return new Cooperator(UUID.fromString(id), firstName.toLowerCase() + "@ex.fr",
                firstName, lastName, null, null);
    }

    @BeforeEach
    void setUp() {
        slotRepo = Mockito.mock(SlotRepository.class);
        cooperatorRepo = Mockito.mock(CooperatorRepository.class);
        handler = new GetAdminSlotsQuery.Handler(slotRepo, cooperatorRepo);
    }

    @Test
    void slot_with_no_registrations_has_empty_registrants_list() {
        var s = slotWithCoops("00000000-0000-0000-0000-000000000001", 4, 5, List.of(), SlotStatus.NEEDS_PEOPLE);
        when(slotRepo.findAll()).thenReturn(List.of(s));
        when(cooperatorRepo.findAllById(ArgumentMatchers.anyCollection())).thenReturn(List.of());

        var result = handler.handle(new GetAdminSlotsQuery());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().registrationCount()).isEqualTo(0);
        assertThat(result.getFirst().registrants()).isEmpty();
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }

    @Test
    void slot_with_registrations_includes_firstname_and_lastname_initial() {
        var c1Id = UUID.fromString("00000000-0000-0000-0000-000000000011");
        var c2Id = UUID.fromString("00000000-0000-0000-0000-000000000012");
        var c1 = coop("00000000-0000-0000-0000-000000000011", "Marie", "Dupont");
        var c2 = coop("00000000-0000-0000-0000-000000000012", "Jean", "Martin");
        var s = slotWithCoops("00000000-0000-0000-0000-000000000001", 1, 5, List.of(c1Id, c2Id), SlotStatus.OPEN);
        when(slotRepo.findAll()).thenReturn(List.of(s));
        when(cooperatorRepo.findAllById(ArgumentMatchers.anyCollection())).thenReturn(List.of(c1, c2));

        var result = handler.handle(new GetAdminSlotsQuery());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().registrationCount()).isEqualTo(2);
        assertThat(result.getFirst().registrants())
                .extracting(RegistrantSummary::firstName, RegistrantSummary::lastNameInitial)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Marie", "D."),
                        org.assertj.core.groups.Tuple.tuple("Jean", "M."));
    }

    @Test
    void cooperator_with_empty_last_name_produces_empty_initial() {
        var cId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        var c = coop("00000000-0000-0000-0000-000000000011", "Alex", "");
        var s = slotWithCoops("00000000-0000-0000-0000-000000000001", 1, 5, List.of(cId), SlotStatus.OPEN);
        when(slotRepo.findAll()).thenReturn(List.of(s));
        when(cooperatorRepo.findAllById(ArgumentMatchers.anyCollection())).thenReturn(List.of(c));

        var result = handler.handle(new GetAdminSlotsQuery());

        assertThat(result.getFirst().registrants()).hasSize(1);
        assertThat(result.getFirst().registrants().getFirst().lastNameInitial()).isEqualTo("");
    }

    @Test
    void open_slot_decorated_as_LOCKED_when_another_is_under_minimum() {
        List<UUID> s1Coops = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<UUID> s2Coops = List.of(UUID.randomUUID(), UUID.randomUUID());
        var s1 = slotWithCoops("00000000-0000-0000-0000-000000000001", 4, 5, s1Coops, SlotStatus.OPEN);
        var s2 = slotWithCoops("00000000-0000-0000-0000-000000000002", 4, 5, s2Coops, SlotStatus.NEEDS_PEOPLE);
        when(slotRepo.findAll()).thenReturn(List.of(s1, s2));
        when(cooperatorRepo.findAllById(ArgumentMatchers.anyCollection())).thenReturn(List.of());

        var result = handler.handle(new GetAdminSlotsQuery());

        var r1 = result.stream().filter(r -> r.slot().id().equals(s1.id())).findFirst().orElseThrow();
        var r2 = result.stream().filter(r -> r.slot().id().equals(s2.id())).findFirst().orElseThrow();
        assertThat(r1.status()).isEqualTo(SlotStatus.LOCKED);
        assertThat(r2.status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }
}
