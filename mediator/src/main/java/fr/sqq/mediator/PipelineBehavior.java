package fr.sqq.mediator;

public interface PipelineBehavior {
    <R> R handle(Command<R> command, Next<R> next);

    default int order() {
        return 0;
    }

    @FunctionalInterface
    interface Next<R> {
        R invoke();
    }
}
