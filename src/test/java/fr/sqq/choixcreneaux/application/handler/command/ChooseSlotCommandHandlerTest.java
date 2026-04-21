package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.ChooseSlotCommand;
import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.exception.*;
import fr.sqq.choixcreneaux.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChooseSlotCommandHandlerTest {
    private SlotRepository slotRepo;
    private CooperatorRepository cooperatorRepo;
    private SlotRegistrationRepository registrationRepo;
    private Campaign campaign;
    private EmailSender emailSender;
    private EmailLogRepository emailLogRepo;
    private ChooseSlotCommandHandler handler;

    private static final UUID SLOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SLOT_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID COOP_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String BARCODE_BASE = "12345";
    private static final Cooperator COOP = new Cooperator(COOP_ID, "test@example.com", "Jean", "Dupont", null, BARCODE_BASE);

    private Slot slot(int registrations) {
        List<SlotRegistration> regs = new ArrayList<>();
        for (int i = 0; i < registrations; i++) {
            regs.add(new SlotRegistration(UUID.randomUUID(), SLOT_ID, UUID.randomUUID(), Instant.now()));
        }
        return Slot.rehydrate(SLOT_ID, Week.A, DayOfWeek.MONDAY, LocalTime.of(15, 45), LocalTime.of(18, 30),
                4, 5, null, 0, regs);
    }

    private Slot otherSlot(int registrations) {
        List<SlotRegistration> regs = new ArrayList<>();
        for (int i = 0; i < registrations; i++) {
            regs.add(new SlotRegistration(UUID.randomUUID(), SLOT_ID_2, UUID.randomUUID(), Instant.now()));
        }
        return Slot.rehydrate(SLOT_ID_2, Week.A, DayOfWeek.TUESDAY, LocalTime.of(15, 45), LocalTime.of(18, 30),
                4, 5, null, 0, regs);
    }

    @BeforeEach
    void setUp() {
        slotRepo = Mockito.mock(SlotRepository.class);
        cooperatorRepo = Mockito.mock(CooperatorRepository.class);
        registrationRepo = Mockito.mock(SlotRegistrationRepository.class);
        campaign = new Campaign(CampaignStatus.OPEN, LocalDate.of(2026, 5, 18), LocalDate.of(2015, 12, 28));
        emailSender = Mockito.mock(EmailSender.class);
        emailLogRepo = Mockito.mock(EmailLogRepository.class);
        handler = new ChooseSlotCommandHandler(slotRepo, cooperatorRepo, registrationRepo, campaign, emailSender, emailLogRepo);

        when(cooperatorRepo.findByBarcodeBase(BARCODE_BASE)).thenReturn(Optional.of(COOP));
        when(registrationRepo.findByCooperatorId(COOP_ID)).thenReturn(Optional.empty());
    }

    @Test
    void rejects_when_cooperator_already_registered() {
        when(registrationRepo.findByCooperatorId(COOP_ID))
                .thenReturn(Optional.of(new SlotRegistration(UUID.randomUUID(), SLOT_ID, COOP_ID, Instant.now())));
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, BARCODE_BASE)))
                .isInstanceOf(AlreadyRegisteredException.class);
    }

    @Test
    void rejects_when_slot_is_full() {
        when(slotRepo.findAll()).thenReturn(List.of(slot(5), otherSlot(4)));
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, BARCODE_BASE)))
                .isInstanceOf(SlotFullException.class);
        verify(slotRepo, never()).save(any());
    }

    @Test
    void rejects_when_slot_is_locked() {
        // target slot at min (4), other slot under min (2) → locked
        when(slotRepo.findAll()).thenReturn(List.of(slot(4), otherSlot(2)));
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, BARCODE_BASE)))
                .isInstanceOf(SlotLockedException.class);
        verify(slotRepo, never()).save(any());
    }

    @Test
    void succeeds_when_slot_needs_people() {
        // both under min → not locked
        when(slotRepo.findAll()).thenReturn(List.of(slot(2), otherSlot(2)));
        handler.handle(new ChooseSlotCommand(SLOT_ID, BARCODE_BASE));
        ArgumentCaptor<Slot> saved = ArgumentCaptor.forClass(Slot.class);
        verify(slotRepo).save(saved.capture());
        assertThat(saved.getValue().hasCooperator(COOP_ID)).isTrue();
        assertThat(saved.getValue().registrationCount()).isEqualTo(3);
    }

    @Test
    void rejects_when_campaign_not_open() {
        var closedCampaign = new Campaign(CampaignStatus.CLOSED, LocalDate.of(2026, 5, 18), LocalDate.of(2015, 12, 28));
        handler = new ChooseSlotCommandHandler(slotRepo, cooperatorRepo, registrationRepo, closedCampaign, emailSender, emailLogRepo);
        when(slotRepo.findAll()).thenReturn(List.of(slot(2), otherSlot(2)));
        assertThatThrownBy(() -> handler.handle(new ChooseSlotCommand(SLOT_ID, BARCODE_BASE)))
                .isInstanceOf(CampaignNotOpenException.class);
    }
}
