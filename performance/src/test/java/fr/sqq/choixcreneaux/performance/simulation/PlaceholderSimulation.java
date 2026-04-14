package fr.sqq.choixcreneaux.performance.simulation;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PlaceholderSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json");

    ScenarioBuilder healthCheck = scenario("Health Check")
            .exec(http("GET /api/health").get("/api/health").check(status().is(200)));

    {
        setUp(healthCheck.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
    }
}
