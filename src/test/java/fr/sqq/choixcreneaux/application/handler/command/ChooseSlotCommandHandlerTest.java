package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.ChooseSlotCommand;
import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.application.handler.query.GetSlotsQueryHandler;
import fr.sqq.choixcreneaux.domain.exception.*;
import fr.sqq.choixcreneaux.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChooseSlotCommandHandlerTest {
    private SlotTemplateRepository slotRepo;
    private CooperatorRepository cooperatorRepo;
    private SlotRegistrationRepository registrationRepo;
    private CampaignRepository campaignRepo;
    private EmailSender emailSender;
    private EmailLogRepository emailLogRepo;
    private ChooseSlotCommandHandler handler;

    private static final UUID SLOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SLOT_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID COOP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String SUBJECT = "keycloak-sub-123";
    private static final SlotTemplate SLOT = new SlotTemplate(SLOT_ID, Week.A, DayOfWeek.MONDAY, LocalTime.of(15, 45), LocalTime.of(18, 30), 4, 5, null);
    private static final SlotTemplate SLOT_2 = new SlotTemplate(SLOT_ID_2, Week.A, DayOfWeek.TUESDAY, LocalTime.of(15, 45), LocalTime.of(18, 30), 4, 5, null);
    private static final Cooperator COOP = new Cooperator(COOP_ID, "test@example.com", "Jean", "Dupont", null, SUBJECT);

    @BeforeEach
    void setUp() {
        slotRepo = Mockito.mock(SlotTemplateRepository.class);
        cooperatorRepo = Mockito.mock(CooperatorRepository.class);
        registrationRepo = Mockito.mock(SlotRegistrationRepository.class);
        campaignRepo = Mockito.mock(CampaignRepository.class);
        emailSender = Mockito.mock(EmailSender.class);
        emailLogRepo = Mockito.mock(EmailLogRepository.class);
        var slotsHandler = new GetSlotsQueryHandler(slotRepo);
        handler = new ChooseSlotCommandHandler(slotRepo, cooperatorRepo, registrationRepo, campaignRepo, emailSender, emailLogRepo, slotsHandler);

        when(campaignRepo.findActive()).thenReturn(Optional.of(new Campaign(UUID.randomUUID(), CampaignStatus.OPEN, Instant.now(), null, null, null)));
        when(cooperatorRepo.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.of(COOP));
        when(slotRepo.findById(SLOT_ID)).thenReturn(Optional.of(SLOT));
        when(slotRepo.findAll()).thenReturn(List.of(SLOT, SLOT_2));
        when(registrationRepo.findByCooperatorId(COOP_ID)).thenReturn(Optional.empty());
    }

    @Test
    void rejects_when_cooperator_already_registered() {
        when(registrationRepo.findByCooperatorId(COOP_ID)).thenReturn(Optional.of(new SlotRegistration(UUID.randomUUID(), SLOT_ID, COOP_ID, Instant.now())));
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, SUBJECT))).isInstanceOf(AlreadyRegisteredException.class);
    }

    @Test
    void rejects_when_slot_is_full() {
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of(SLOT_ID, 5, SLOT_ID_2, 0));
        when(registrationRepo.countBySlotTemplateId(SLOT_ID)).thenReturn(5);
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, SUBJECT))).isInstanceOf(SlotFullException.class);
    }

    @Test
    void rejects_when_slot_is_locked() {
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of(SLOT_ID, 4, SLOT_ID_2, 2));
        when(registrationRepo.countBySlotTemplateId(SLOT_ID)).thenReturn(4);
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, SUBJECT))).isInstanceOf(SlotLockedException.class);
    }

    @Test
    void succeeds_when_slot_needs_people() {
        when(slotRepo.countRegistrationsPerSlot()).thenReturn(Map.of(SLOT_ID, 2, SLOT_ID_2, 2));
        when(registrationRepo.countBySlotTemplateId(SLOT_ID)).thenReturn(2);
        when(registrationRepo.save(SLOT_ID, COOP_ID)).thenReturn(new SlotRegistration(UUID.randomUUID(), SLOT_ID, COOP_ID, Instant.now()));
        handler.handle(new ChooseSlotCommand(SLOT_ID, SUBJECT));
        verify(registrationRepo).save(SLOT_ID, COOP_ID);
    }

    @Test
    void rejects_when_campaign_not_open() {
        when(campaignRepo.findActive()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, SUBJECT))).isInstanceOf(CampaignNotOpenException.class);
    }
}
