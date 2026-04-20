package fr.sqq.choixcreneaux.application.query;

import fr.sqq.mediator.Query;

public record SearchCooperatorsQuery(String q, int page, int size) implements Query<PendingCooperatorsPage> {}
