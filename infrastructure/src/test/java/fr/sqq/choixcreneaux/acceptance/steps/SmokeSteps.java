package fr.sqq.choixcreneaux.acceptance.steps;

import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Quand;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class SmokeSteps {

    private Response response;

    @Quand("j'appelle le endpoint de santé")
    public void jAppelleLeEndpointDeSante() {
        response = given()
                .when()
                .get("/api/health");
    }

    @Alors("je reçois le statut {string}")
    public void jeRecoisLeStatut(String expectedStatus) {
        response.then()
                .statusCode(200)
                .body("status", equalTo(expectedStatus));
    }
}
