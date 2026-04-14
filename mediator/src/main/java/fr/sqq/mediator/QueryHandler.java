package fr.sqq.mediator;

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
