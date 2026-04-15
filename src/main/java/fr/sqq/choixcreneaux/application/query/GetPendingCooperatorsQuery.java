package fr.sqq.choixcreneaux.application.query;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.mediator.Query;
import java.util.List;
public record GetPendingCooperatorsQuery() implements Query<List<Cooperator>> {}
