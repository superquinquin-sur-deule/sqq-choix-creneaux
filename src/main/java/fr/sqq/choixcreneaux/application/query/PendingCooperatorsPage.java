package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.domain.model.Cooperator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PendingCooperatorsPage(
        List<Cooperator> items,
        long total,
        Map<UUID, Instant> lastReminderByCooperatorId,
        Map<UUID, CooperatorSlotSummary> slotByCooperatorId) {
    public PendingCooperatorsPage(List<Cooperator> items, long total) {
        this(items, total, Map.of(), Map.of());
    }
    public PendingCooperatorsPage(List<Cooperator> items, long total, Map<UUID, Instant> lastReminderByCooperatorId) {
        this(items, total, lastReminderByCooperatorId, Map.of());
    }
}
