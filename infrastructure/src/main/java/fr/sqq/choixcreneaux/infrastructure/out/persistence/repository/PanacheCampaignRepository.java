package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.CampaignRepository;
import fr.sqq.choixcreneaux.domain.model.Campaign;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.CampaignEntity;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class PanacheCampaignRepository implements CampaignRepository {
    @Inject EntityMapper mapper;

    @Override
    public Optional<Campaign> findActive() {
        return CampaignEntity.<CampaignEntity>find("status", "OPEN")
                .firstResultOptional().map(mapper::toDomain);
    }
}
