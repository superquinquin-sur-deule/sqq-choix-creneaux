package fr.sqq.choixcreneaux.acceptance.steps;

import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonné;
import io.cucumber.java.fr.Quand;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

public class SlotSelectionSteps {
    private Response response;

    @Quand("j'appelle GET {string}")
    public void jAppelleGet(String path) {
        response = given().when().get(path);
    }

    @Quand("j'appelle POST {string}")
    public void jAppellePost(String path) {
        response = given().contentType("application/json").when().post(path);
    }

    @Alors("je reçois un code HTTP {int}")
    public void jeRecoisUnCodeHttp(int code) {
        response.then().statusCode(code);
    }

    @Etantdonné("que je suis déjà inscrit à un créneau")
    public void queJeSuisDejaInscritAUnCreneau() {
        // Pre-condition: a registration already exists for the test slot
        // This state is assumed to be set up by the test environment seed data
    }

    @Alors("la réponse contient une liste de créneaux")
    public void laReponseContientUneListeDeCreneaux() {
        response.then().body("slots.size()", greaterThan(0));
    }
}
