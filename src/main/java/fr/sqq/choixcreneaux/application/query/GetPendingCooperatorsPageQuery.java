package fr.sqq.choixcreneaux.application.query;

import fr.sqq.mediator.Query;

public record GetPendingCooperatorsPageQuery(int page, int size) implements Query<PendingCooperatorsPage> {}
