package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.ChooseSlotCommand;
import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.exception.*;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.choixcreneaux.application.handler.query.GetSlotsQueryHandler;
import fr.sqq.choixcreneaux.application.query.GetSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ChooseSlotCommandHandler implements CommandHandler<ChooseSlotCommand, Void> {
    private final SlotTemplateRepository slotRepo;
    private final CooperatorRepository cooperatorRepo;
    private final SlotRegistrationRepository registrationRepo;
    private final CampaignRepository campaignRepo;
    private final EmailSender emailSender;
    private final EmailLogRepository emailLogRepo;
    private final GetSlotsQueryHandler slotsHandler;

    @Inject
    public ChooseSlotCommandHandler(SlotTemplateRepository slotRepo, CooperatorRepository cooperatorRepo,
                                     SlotRegistrationRepository registrationRepo, CampaignRepository campaignRepo,
                                     EmailSender emailSender, EmailLogRepository emailLogRepo,
                                     GetSlotsQueryHandler slotsHandler) {
        this.slotRepo = slotRepo;
        this.cooperatorRepo = cooperatorRepo;
        this.registrationRepo = registrationRepo;
        this.campaignRepo = campaignRepo;
        this.emailSender = emailSender;
        this.emailLogRepo = emailLogRepo;
        this.slotsHandler = slotsHandler;
    }

    @Override
    @Transactional
    public Void handle(ChooseSlotCommand command) {
        campaignRepo.findActive().orElseThrow(CampaignNotOpenException::new);
        var cooperator = cooperatorRepo.findByKeycloakSubject(command.keycloakSubject())
                .orElseThrow(() -> new RuntimeException("Cooperator not found"));
        if (registrationRepo.findByCooperatorId(cooperator.id()).isPresent()) {
            throw new AlreadyRegisteredException();
        }
        var slot = slotRepo.findById(command.slotTemplateId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        var allSlots = slotsHandler.handle(new GetSlotsQuery());
        var targetSlot = allSlots.stream()
                .filter(s -> s.slot().id().equals(command.slotTemplateId()))
                .findFirst().orElseThrow();
        if (targetSlot.status() == SlotStatus.FULL) throw new SlotFullException();
        if (targetSlot.status() == SlotStatus.LOCKED) throw new SlotLockedException();

        int freshCount = registrationRepo.countBySlotTemplateId(command.slotTemplateId());
        if (freshCount >= slot.maxCapacity()) throw new SlotFullException();

        registrationRepo.save(command.slotTemplateId(), cooperator.id());

        try {
            emailSender.sendConfirmation(cooperator, slot, slot.week().name());
            emailLogRepo.log(cooperator.id(), EmailType.CONFIRMATION);
        } catch (Exception e) {
            Log.warn("Failed to send confirmation email", e);
        }
        return null;
    }
}
