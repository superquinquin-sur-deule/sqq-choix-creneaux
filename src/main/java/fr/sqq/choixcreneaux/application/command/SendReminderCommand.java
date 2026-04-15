package fr.sqq.choixcreneaux.application.command;
import fr.sqq.mediator.Command;
import java.util.List;
import java.util.UUID;
public record SendReminderCommand(List<UUID> cooperatorIds, boolean all) implements Command<Integer> {}
