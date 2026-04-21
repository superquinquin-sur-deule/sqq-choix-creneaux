package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.choixcreneaux.application.query.GetAdminSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GetAdminSlotsQueryHandlerTest {

    private SlotTemplateRepository slotRepo;
    private SlotRegistrationRepository registrationRepo;
    private CooperatorRepository cooperatorRepo;
    private GetAdminSlotsQuery.Handler handler;

    private static SlotTemplate slot(String id, int min, int max) {
        return new SlotTemplate(UUID.fromString(id), Week.A, DayOfWeek.MONDAY,
                LocalTime.of(8, 15), LocalTime.of(11, 0), min, max, null);
    }

    private static Cooperator coop(String id, String firstName, String lastName) {
        return new Cooperator(UUID.fromString(id), firstName.toLowerCase() + "@ex.fr",
                firstName, lastName, null, null);
    }

    private static SlotRegistration reg(String slotId, String coopId) {
        return new SlotRegistration(UUID.randomUUID(), UUID.fromString(slotId),
                UUID.fromString(coopId), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @BeforeEach
    void setUp() {
        slotRepo = Mockito.mock(SlotTemplateRepository.class);
        registrationRepo = Mockito.mock(SlotRegistrationRepository.class);
        cooperatorRepo = Mockito.mock(CooperatorRepository.class);
        handler = new GetAdminSlotsQuery.Handler(slotRepo, registrationRepo, cooperatorRepo);
    }

    @Test
    void slot_with_no_registrations_has_empty_registrants_list() {
        var s = slot("00000000-0000-0000-0000-000000000001", 4, 5);
        when(slotRepo.findAll()).thenReturn(List.of(s));
        when(registrationRepo.findAll()).thenReturn(List.of());
        when(cooperatorRepo.findAllById(ArgumentMatchers.anyCollection())).thenReturn(List.of());

        var result = handler.handle(new GetAdminSlotsQuery());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().registrationCount()).isEqualTo(0);
        assertThat(result.getFirst().registrants()).isEmpty();
        assertThat(result.getFirst().status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }

    @Test
    void slot_with_registrations_includes_firstname_and_lastname_initial() {
        var s = slot("00000000-0000-0000-0000-000000000001", 1, 5);
        var c1 = coop("00000000-0000-0000-0000-000000000011", "Marie", "Dupont");
        var c2 = coop("00000000-0000-0000-0000-000000000012", "Jean", "Martin");
        when(slotRepo.findAll()).thenReturn(List.of(s));
        when(registrationRepo.findAll()).thenReturn(List.of(
                reg("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000011"),
                reg("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000012")));
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
        var s = slot("00000000-0000-0000-0000-000000000001", 1, 5);
        var c = coop("00000000-0000-0000-0000-000000000011", "Alex", "");
        when(slotRepo.findAll()).thenReturn(List.of(s));
        when(registrationRepo.findAll()).thenReturn(List.of(
                reg("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000011")));
        when(cooperatorRepo.findAllById(ArgumentMatchers.anyCollection())).thenReturn(List.of(c));

        var result = handler.handle(new GetAdminSlotsQuery());

        assertThat(result.getFirst().registrants()).hasSize(1);
        assertThat(result.getFirst().registrants().getFirst().lastNameInitial()).isEqualTo("");
    }

    @Test
    void status_uses_shared_calculator_logic() {
        var s1 = slot("00000000-0000-0000-0000-000000000001", 4, 5);
        var s2 = slot("00000000-0000-0000-0000-000000000002", 4, 5);
        when(slotRepo.findAll()).thenReturn(List.of(s1, s2));
        // s1 full at min (4), s2 is under min (2) → s1 LOCKED, s2 NEEDS_PEOPLE
        var regs = new ArrayList<SlotRegistration>();
        for (int i = 0; i < 4; i++) {
            regs.add(new SlotRegistration(UUID.randomUUID(), s1.id(), UUID.randomUUID(), Instant.now()));
        }
        for (int i = 0; i < 2; i++) {
            regs.add(new SlotRegistration(UUID.randomUUID(), s2.id(), UUID.randomUUID(), Instant.now()));
        }
        when(registrationRepo.findAll()).thenReturn(regs);
        when(cooperatorRepo.findAllById(ArgumentMatchers.anyCollection())).thenReturn(List.of());

        var result = handler.handle(new GetAdminSlotsQuery());

        var r1 = result.stream().filter(r -> r.slot().id().equals(s1.id())).findFirst().orElseThrow();
        var r2 = result.stream().filter(r -> r.slot().id().equals(s2.id())).findFirst().orElseThrow();
        assertThat(r1.status()).isEqualTo(SlotStatus.LOCKED);
        assertThat(r2.status()).isEqualTo(SlotStatus.NEEDS_PEOPLE);
    }
}
