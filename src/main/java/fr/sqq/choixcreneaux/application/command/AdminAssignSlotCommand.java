package fr.sqq.choixcreneaux.application.command;

import fr.sqq.mediator.Command;
import java.util.UUID;

// TODO: les commande et les handlers devraient être dans le même fichier (pareil pour les queries)
public record AdminAssignSlotCommand(UUID slotTemplateId, UUID cooperatorId) implements Command<AdminAssignSlotResult> {}

