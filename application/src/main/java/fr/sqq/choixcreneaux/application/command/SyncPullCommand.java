package fr.sqq.choixcreneaux.application.command;
import fr.sqq.mediator.Command;
public record SyncPullCommand() implements Command<SyncPullCommand.Result> {
    public record Result(int slotsImported, int cooperatorsImported) {}
}
