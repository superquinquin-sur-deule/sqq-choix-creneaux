package fr.sqq.choixcreneaux.application.command;

import fr.sqq.mediator.Command;
import java.util.UUID;

public record AdminAssignSlotCommand(UUID slotTemplateId, UUID cooperatorId) implements Command<AdminAssignSlotResult> {}
