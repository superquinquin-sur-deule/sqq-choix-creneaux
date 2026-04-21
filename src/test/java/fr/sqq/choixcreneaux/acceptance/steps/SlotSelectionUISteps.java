package fr.sqq.choixcreneaux.acceptance.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Etantdonné;
import io.cucumber.java.fr.Quand;

public class SlotSelectionUISteps {

    private static final int TEST_PORT = 8081;
    private static final String SLOT_ID = "00000000-0000-0000-0000-000000000001";

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @Before
    public void setup() {
        playwright = Playwright.create();
        boolean headless = !"false".equals(System.getProperty("quarkus.playwright.headless"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        context = browser.newContext();
        page = context.newPage();
    }

    @After
    public void tearDown() {
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Etantdonné("des créneaux sont disponibles")
    public void queDesCreneauxSontDisponibles() {
        page.route("**/api/me", route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("""
                        {
                          "barcodeBase": "12345",
                          "email": "coop@example.com",
                          "firstName": "Jean",
                          "lastName": "Dupont",
                          "roles": []
                        }
                        """)));

        page.route("**/api/me/registration", route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("{\"registeredSlotId\": null}")));

        page.route("**/api/slots", route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("""
                        {
                          "slots": [
                            {
                              "id": "%s",
                              "week": "A",
                              "dayOfWeek": "TUESDAY",
                              "startTime": "10:00",
                              "endTime": "12:00",
                              "minCapacity": 2,
                              "maxCapacity": 4,
                              "registrationCount": 0,
                              "status": "NEEDS_PEOPLE"
                            }
                          ],
                          "campaign": {"storeOpening": "2026-05-18", "weekAReference": "2015-12-28"}
                        }
                        """.formatted(SLOT_ID))));

        page.route("**/api/slots/*/register", route -> {
            // After a successful registration the app refetches /api/me/registration.
            // Re-register the mock so the subsequent fetch sees the persisted slot id
            // (Playwright matches routes LIFO, so this override wins).
            page.route("**/api/me/registration", r -> r.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"registeredSlotId\": \"%s\"}".formatted(SLOT_ID))));
            route.fulfill(new Route.FulfillOptions().setStatus(204));
        });
    }

    @Quand("je me rends sur la page de choix de créneau")
    public void jeMeRendsSurLaPageDeChoix() {
        page.navigate("http://localhost:" + TEST_PORT + "/choisir");
    }

    @Et("je sélectionne le créneau du mardi 10h00")
    public void jeSelectionneLeCreneauDuMardi10h() {
        page.locator("button:has-text('10h – 12h')").first().click();
    }

    @Et("je continue vers la confirmation")
    public void jeContinueVersLaConfirmation() {
        page.locator("a:has-text('Continuer vers la confirmation')").click();
    }

    @Et("je confirme mon choix")
    public void jeConfirmeMonChoix() {
        page.locator("button:has-text('Confirmer mon choix')").click();
    }

    @Alors("je vois la page {string}")
    public void jeVoisLaPage(String expectedHeading) {
        PlaywrightAssertions.assertThat(page.locator("h1")).hasText(expectedHeading);
    }
}
