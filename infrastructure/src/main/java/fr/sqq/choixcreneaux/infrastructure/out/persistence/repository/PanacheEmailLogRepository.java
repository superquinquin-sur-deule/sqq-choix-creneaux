package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.EmailLogRepository;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.EmailLogEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
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
}
