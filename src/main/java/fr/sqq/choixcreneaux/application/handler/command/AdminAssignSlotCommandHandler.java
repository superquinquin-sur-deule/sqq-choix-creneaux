package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.AdminAssignSlotCommand;
import fr.sqq.choixcreneaux.application.command.AdminAssignSlotResult;
import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.exception.SlotFullException;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AdminAssignSlotCommandHandler implements CommandHandler<AdminAssignSlotCommand, AdminAssignSlotResult> {
    private final SlotTemplateRepository slotRepo;
    private final CooperatorRepository cooperatorRepo;
    private final SlotRegistrationRepository registrationRepo;
    private final EmailSender emailSender;
    private final EmailLogRepository emailLogRepo;

    @Inject
    public AdminAssignSlotCommandHandler(SlotTemplateRepository slotRepo, CooperatorRepository cooperatorRepo,
                                          SlotRegistrationRepository registrationRepo,
                                          EmailSender emailSender, EmailLogRepository emailLogRepo) {
        this.slotRepo = slotRepo;
        this.cooperatorRepo = cooperatorRepo;
        this.registrationRepo = registrationRepo;
        this.emailSender = emailSender;
        this.emailLogRepo = emailLogRepo;
    }

    @Override
    @Transactional
    public AdminAssignSlotResult handle(AdminAssignSlotCommand command) {
        var cooperator = cooperatorRepo.findById(command.cooperatorId())
                .orElseThrow(() -> new RuntimeException("Cooperator not found"));
        var slot = slotRepo.findById(command.slotTemplateId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        var existing = registrationRepo.findByCooperatorId(cooperator.id());
        boolean moved = false;
        if (existing.isPresent()) {
            if (existing.get().slotTemplateId().equals(command.slotTemplateId())) {
                return new AdminAssignSlotResult(false);
            }
            Log.infof("Admin move: cooperator %s from slot %s to slot %s",
                    cooperator.id(), existing.get().slotTemplateId(), command.slotTemplateId());
            registrationRepo.deleteByCooperatorId(cooperator.id());
            moved = true;
        }

        int freshCount = registrationRepo.countBySlotTemplateId(command.slotTemplateId());
        if (freshCount >= slot.maxCapacity()) throw new SlotFullException();

        registrationRepo.save(command.slotTemplateId(), cooperator.id());

        try {
            emailSender.sendConfirmation(cooperator, slot, slot.week().name());
            emailLogRepo.log(cooperator.id(), EmailType.CONFIRMATION);
        } catch (Exception e) {
            Log.warn("Failed to send confirmation email", e);
        }
        return new AdminAssignSlotResult(moved);
    }
}
