package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.domain.model.Cooperator;
import java.util.List;

public record PendingCooperatorsPage(List<Cooperator> items, long total) {}
