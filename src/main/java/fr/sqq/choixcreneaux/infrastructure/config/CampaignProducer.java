package fr.sqq.choixcreneaux.infrastructure.config;

import fr.sqq.choixcreneaux.domain.model.Campaign;
import fr.sqq.choixcreneaux.domain.model.CampaignStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;

@ApplicationScoped
public class CampaignProducer {

    @ConfigProperty(name = "app.campaign.open", defaultValue = "true")
    boolean open;

    @ConfigProperty(name = "app.campaign.store-opening")
    LocalDate storeOpening;

    @ConfigProperty(name = "app.campaign.week-a-reference")
    LocalDate weekAReference;

    @Produces
    @ApplicationScoped
    public Campaign campaign() {
        return new Campaign(
            open ? CampaignStatus.OPEN : CampaignStatus.CLOSED,
            storeOpening,
            weekAReference
        );
    }
}
