package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.port.out.EmailLogRepository;
import fr.sqq.choixcreneaux.application.port.out.EmailSender;
import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.choixcreneaux.infrastructure.in.rest.fixtures.TestFixtures;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class AdminSlotEndpointTest {

    private static final String ADMIN_ROLE = "Foodcoop Admin";

    @Inject TestFixtures fixtures;
    @InjectMock EmailSender emailSender;
    @InjectMock EmailLogRepository emailLogRepo;

    @BeforeEach
    void reset() {
        fixtures.cleanAll();
    }

    @Test
    @TestSecurity(user = "admin", roles = ADMIN_ROLE)
    void slot_with_no_registrations_has_empty_registrants_list() {
        UUID slotId = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 0);

        given().when().get("/api/admin/slots")
                .then().statusCode(200)
                .body("find { it.id == '%s' }.registrationCount".formatted(slotId), is(0))
                .body("find { it.id == '%s' }.registrants".formatted(slotId), empty())
                .body("find { it.id == '%s' }.status".formatted(slotId), is("NEEDS_PEOPLE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = ADMIN_ROLE)
    void slot_with_registrations_includes_firstname_and_lastname_initial() {
        UUID slotId = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 1, 5, 0);
        UUID marieId = fixtures.seedCooperator("B1", "Marie", "Dupont");
        UUID jeanId = fixtures.seedCooperator("B2", "Jean", "Martin");

        given().contentType("application/json").body(Map.of("cooperatorId", marieId))
                .when().post("/api/admin/slots/{slotId}/assign", slotId)
                .then().statusCode(200).body("moved", is(false));
        given().contentType("application/json").body(Map.of("cooperatorId", jeanId))
                .when().post("/api/admin/slots/{slotId}/assign", slotId)
                .then().statusCode(200).body("moved", is(false));

        given().when().get("/api/admin/slots")
                .then().statusCode(200)
                .body("find { it.id == '%s' }.registrationCount".formatted(slotId), is(2))
                .body("find { it.id == '%s' }.registrants.firstName".formatted(slotId),
                        containsInAnyOrder("Marie", "Jean"))
                .body("find { it.id == '%s' }.registrants.lastNameInitial".formatted(slotId),
                        containsInAnyOrder("D.", "M."));
    }

    @Test
    @TestSecurity(user = "admin", roles = ADMIN_ROLE)
    void cooperator_with_empty_last_name_produces_empty_initial() {
        UUID slotId = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 1, 5, 0);
        UUID alexId = fixtures.seedCooperator("B9", "Alex", "");

        given().contentType("application/json").body(Map.of("cooperatorId", alexId))
                .when().post("/api/admin/slots/{slotId}/assign", slotId)
                .then().statusCode(200);

        given().when().get("/api/admin/slots")
                .then()
                .body("find { it.id == '%s' }.registrants[0].firstName".formatted(slotId), is("Alex"))
                .body("find { it.id == '%s' }.registrants[0].lastNameInitial".formatted(slotId), is(""));
    }

    @Test
    @TestSecurity(user = "admin", roles = ADMIN_ROLE)
    void open_slot_decorated_as_LOCKED_when_another_is_under_minimum() {
        UUID atMin = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 4);
        UUID underMin = fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, 4, 5, 2);

        given().when().get("/api/admin/slots")
                .then().statusCode(200)
                .body("find { it.id == '%s' }.status".formatted(atMin), is("LOCKED"))
                .body("find { it.id == '%s' }.status".formatted(underMin), is("NEEDS_PEOPLE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = ADMIN_ROLE)
    void admin_assign_moves_cooperator_between_slots() {
        UUID sourceSlot = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 1, 5, 0);
        UUID targetSlot = fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, 1, 5, 0);
        UUID coopId = fixtures.seedCooperator("B42", "Alice", "Morel");

        given().contentType("application/json").body(Map.of("cooperatorId", coopId))
                .when().post("/api/admin/slots/{slotId}/assign", sourceSlot)
                .then().statusCode(200).body("moved", is(false));

        given().contentType("application/json").body(Map.of("cooperatorId", coopId))
                .when().post("/api/admin/slots/{slotId}/assign", targetSlot)
                .then().statusCode(200).body("moved", is(true));

        given().when().get("/api/admin/slots")
                .then()
                .body("find { it.id == '%s' }.registrationCount".formatted(sourceSlot), is(0))
                .body("find { it.id == '%s' }.registrationCount".formatted(targetSlot), is(1));
    }

}
