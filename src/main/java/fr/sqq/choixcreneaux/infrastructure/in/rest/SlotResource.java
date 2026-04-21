package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.query.GetSlotsQuery;
import fr.sqq.choixcreneaux.application.query.SlotWithFillInfo;
import fr.sqq.choixcreneaux.domain.model.Campaign;
import fr.sqq.mediator.Mediator;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Path("/api/slots")
@Produces(MediaType.APPLICATION_JSON)
public class SlotResource {

    @Inject
    Mediator mediator;

    @Inject
    Campaign campaign;

    @GET
    public SlotsPageResponse getSlots() {
        List<SlotWithFillInfo> slots = mediator.send(new GetSlotsQuery());
        CampaignInfo campaignInfo = new CampaignInfo(campaign.storeOpening().toString(), campaign.weekAReference().toString());
        return new SlotsPageResponse(slots.stream().map(SlotResponse::from).toList(), campaignInfo);
    }

    public record SlotsPageResponse(List<SlotResponse> slots, CampaignInfo campaign) {}

    public record CampaignInfo(String storeOpening, String weekAReference) {}

    public record SlotResponse(UUID id, String week, DayOfWeek dayOfWeek, String startTime, String endTime,
                               int minCapacity, int maxCapacity, int registrationCount, String status) {
        static SlotResponse from(SlotWithFillInfo info) {
            return new SlotResponse(info.slot().id(), info.slot().week().name(), info.slot().dayOfWeek(),
                    info.slot().startTime().toString(), info.slot().endTime().toString(),
                    info.slot().minCapacity(), info.slot().maxCapacity(), info.registrationCount(), info.status().name());
        }
    }
}
