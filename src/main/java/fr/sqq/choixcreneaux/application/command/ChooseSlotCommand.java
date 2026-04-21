package fr.sqq.choixcreneaux.application.command;
import fr.sqq.mediator.Command;
import java.util.UUID;
public record ChooseSlotCommand(UUID slotTemplateId, String barcodeBase) implements Command<Void> {}
