package fr.sqq.choixcreneaux.e2e.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.choixcreneaux.infrastructure.in.rest.fixtures.TestFixtures;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Etantdonné;
import io.cucumber.java.fr.Quand;
import jakarta.inject.Inject;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

public class SlotSelectionUISteps {

    private static final int TEST_PORT = 8081;
    private static final String BARCODE = "12345";

    @Inject
    TestFixtures fixtures;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @Before
    public void setup() {
        fixtures.cleanAll();

        playwright = Playwright.create();
        boolean headless = !"false".equals(System.getProperty("quarkus.playwright.headless"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        context = browser.newContext();
        // Every request carries an X-Test-Auth header; TestAuthFilter turns that
        // into a SecurityIdentity so the real endpoints see an authenticated user.
        context.setExtraHTTPHeaders(Map.of("X-Test-Auth", BARCODE));
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
        fixtures.seedCooperator(BARCODE, "Jean", "Dupont");
        fixtures.seedSlot(Week.A, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), 1, 4, 0);
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
