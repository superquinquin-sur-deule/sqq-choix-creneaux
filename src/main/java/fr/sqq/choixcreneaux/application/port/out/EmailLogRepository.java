package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
public interface EmailLogRepository {
    void log(UUID cooperatorId, EmailType type);
    Map<UUID, Instant> findLastSentByCooperators(Collection<UUID> cooperatorIds, EmailType type);
}
