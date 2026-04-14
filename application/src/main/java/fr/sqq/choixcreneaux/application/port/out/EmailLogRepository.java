package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import java.util.UUID;
public interface EmailLogRepository {
    void log(UUID cooperatorId, EmailType type);
}
