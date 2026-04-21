package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.port.out.EmailLogRepository;
import fr.sqq.choixcreneaux.application.port.out.EmailSender;
import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.choixcreneaux.infrastructure.in.rest.fixtures.TestFixtures;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile(SlotRegistrationCampaignClosedTest.CampaignClosedProfile.class)
class SlotRegistrationCampaignClosedTest {

    private static final String BARCODE = "12345";

    @Inject TestFixtures fixtures;
    @InjectMock EmailSender emailSender;
    @InjectMock EmailLogRepository emailLogRepo;

    @BeforeEach
    void reset() {
        fixtures.cleanAll();
    }

    @Test
    @TestSecurity(user = BARCODE)
    void rejects_when_campaign_not_open() {
        UUID slotId = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 1, 5, 0);
        fixtures.seedCooperator(BARCODE, "Jean", "Dupont");

        given().when().post("/api/slots/{slotId}/register", slotId)
                .then().statusCode(403);
    }

    public static class CampaignClosedProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("app.campaign.open", "false");
        }
    }
}
