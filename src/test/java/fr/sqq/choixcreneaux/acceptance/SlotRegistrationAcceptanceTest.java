package fr.sqq.choixcreneaux.acceptance;

import fr.sqq.choixcreneaux.application.port.out.EmailLogRepository;
import fr.sqq.choixcreneaux.application.port.out.EmailSender;
import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.choixcreneaux.acceptance.fixtures.TestFixtures;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class SlotRegistrationAcceptanceTest {

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
    void cooperator_registers_on_a_slot_under_minimum() {
        UUID slotId = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 2);
        fixtures.seedCooperator(BARCODE, "Jean", "Dupont");

        given().when().post("/api/slots/{slotId}/register", slotId)
                .then().statusCode(204);

        given().when().get("/api/slots")
                .then().statusCode(200)
                .body("slots.find { it.id == '%s' }.registrationCount".formatted(slotId), is(3));
    }

    @Test
    @TestSecurity(user = BARCODE)
    void registering_succeeds_even_when_other_slots_are_under_minimum_if_target_also_is() {
        UUID targetSlot = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 2);
        fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, 4, 5, 1);
        fixtures.seedCooperator(BARCODE, "Jean", "Dupont");

        given().when().post("/api/slots/{slotId}/register", targetSlot)
                .then().statusCode(204);

        given().when().get("/api/slots")
                .then()
                .body("slots.find { it.id == '%s' }.registrationCount".formatted(targetSlot), is(3));
    }

    @Test
    @TestSecurity(user = BARCODE)
    void rejects_when_slot_is_full() {
        UUID slotId = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 5);
        fixtures.seedCooperator(BARCODE, "Jean", "Dupont");

        given().when().post("/api/slots/{slotId}/register", slotId)
                .then().statusCode(409)
                .body("detail", containsString("complet"));

        given().when().get("/api/slots")
                .then().body("slots.find { it.id == '%s' }.registrationCount".formatted(slotId), is(5));
    }

    @Test
    @TestSecurity(user = BARCODE)
    void rejects_when_slot_is_locked() {
        UUID targetAtMin = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 4);
        fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, 4, 5, 1);
        fixtures.seedCooperator(BARCODE, "Jean", "Dupont");

        given().when().post("/api/slots/{slotId}/register", targetAtMin)
                .then().statusCode(423);

        given().when().get("/api/slots")
                .then().body("slots.find { it.id == '%s' }.registrationCount".formatted(targetAtMin), is(4));
    }

    @Test
    @TestSecurity(user = BARCODE)
    void rejects_when_cooperator_already_registered() {
        UUID firstSlot = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 1, 5, 0);
        UUID secondSlot = fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, 1, 5, 0);
        fixtures.seedCooperator(BARCODE, "Jean", "Dupont");

        given().when().post("/api/slots/{slotId}/register", firstSlot)
                .then().statusCode(204);

        given().when().post("/api/slots/{slotId}/register", secondSlot)
                .then().statusCode(409);

        given().when().get("/api/slots")
                .then()
                .body("slots.find { it.id == '%s' }.registrationCount".formatted(firstSlot), is(1))
                .body("slots.find { it.id == '%s' }.registrationCount".formatted(secondSlot), is(0));
    }
}
