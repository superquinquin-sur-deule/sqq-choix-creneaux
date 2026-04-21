package fr.sqq.choixcreneaux.application.command;
import fr.sqq.mediator.Command;
public record SyncCooperatorsCommand() implements Command<SyncCooperatorsCommand.Result> {
    public record Result(int cooperatorsImported) {}
}
