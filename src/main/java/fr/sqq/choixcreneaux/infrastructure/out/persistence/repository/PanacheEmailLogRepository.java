package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.EmailLogRepository;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.EmailLogEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class PanacheEmailLogRepository implements EmailLogRepository {
    @Override
    public void log(UUID cooperatorId, EmailType type) {
        var entity = new EmailLogEntity();
        entity.id = UUID.randomUUID();
        entity.cooperatorId = cooperatorId;
        entity.type = type.name();
        entity.sentAt = Instant.now();
        entity.persist();
    }

    @Override
    public Map<UUID, Instant> findLastSentByCooperators(Collection<UUID> cooperatorIds, EmailType type) {
        if (cooperatorIds.isEmpty()) return Map.of();
        var rows = EmailLogEntity.<EmailLogEntity>list(
                "type = ?1 and cooperatorId in ?2", type.name(), cooperatorIds);
        Map<UUID, Instant> latest = new HashMap<>();
        for (var row : rows) {
            latest.merge(row.cooperatorId, row.sentAt,
                    (existing, candidate) -> candidate.isAfter(existing) ? candidate : existing);
        }
        return latest;
    }
}
