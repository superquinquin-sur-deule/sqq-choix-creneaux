package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.Campaign;
import java.util.Optional;
public interface CampaignRepository {
    Optional<Campaign> findActive();
}
