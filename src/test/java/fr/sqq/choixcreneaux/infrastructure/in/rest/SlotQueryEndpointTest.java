package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.choixcreneaux.infrastructure.in.rest.fixtures.TestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class SlotQueryEndpointTest {

    @Inject TestFixtures fixtures;

    @BeforeEach
    void reset() {
        fixtures.cleanAll();
    }

    @Test
    void slot_under_minimum_is_NEEDS_PEOPLE() {
        UUID id = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 2);

        given().when().get("/api/slots")
                .then().statusCode(200)
                .body("slots.find { it.id == '%s' }.status".formatted(id), is("NEEDS_PEOPLE"))
                .body("slots.find { it.id == '%s' }.registrationCount".formatted(id), is(2));
    }

    @Test
    void open_slot_becomes_LOCKED_when_another_is_under_minimum() {
        UUID open = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 4);
        UUID underMin = fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, 4, 5, 2);

        given().when().get("/api/slots")
                .then().statusCode(200)
                .body("slots.find { it.id == '%s' }.status".formatted(open), is("LOCKED"))
                .body("slots.find { it.id == '%s' }.status".formatted(underMin), is("NEEDS_PEOPLE"));
    }

    @Test
    void all_slots_OPEN_when_all_minimums_reached() {
        UUID a = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 4);
        UUID b = fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, 4, 5, 4);

        given().when().get("/api/slots")
                .then().statusCode(200)
                .body("slots.find { it.id == '%s' }.status".formatted(a), is("OPEN"))
                .body("slots.find { it.id == '%s' }.status".formatted(b), is("OPEN"));
    }

    @Test
    void slot_at_max_capacity_is_FULL() {
        UUID id = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 5);

        given().when().get("/api/slots")
                .then().statusCode(200)
                .body("slots.find { it.id == '%s' }.status".formatted(id), is("FULL"))
                .body("slots.find { it.id == '%s' }.registrationCount".formatted(id), is(5));
    }

    @Test
    void slot_with_no_registrations_has_count_zero_and_NEEDS_PEOPLE() {
        UUID id = fixtures.seedSlot(Week.A, DayOfWeek.MONDAY, 4, 5, 0);

        given().when().get("/api/slots")
                .then().statusCode(200)
                .body("slots.find { it.id == '%s' }.status".formatted(id), is("NEEDS_PEOPLE"))
                .body("slots.find { it.id == '%s' }.registrationCount".formatted(id), is(0));
    }
}
