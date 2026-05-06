package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public record SyncPushCommand(Consumer<LogLine> logSink, Set<Week> weeks)
        implements Command<SyncPushCommand.Stats> {

    public SyncPushCommand() {
        this(line -> {}, EnumSet.allOf(Week.class));
    }

    public SyncPushCommand(Consumer<LogLine> logSink) {
        this(logSink, EnumSet.allOf(Week.class));
    }

    public record Stats(int created, int moved, int unchanged, int failed) {
        public int total() { return created + moved + unchanged + failed; }
    }

    public record LogLine(String level, String message, int processed, int total) {}

    @ApplicationScoped
    public static class Handler implements CommandHandler<SyncPushCommand, Stats> {
        private final SlotRegistrationFinder registrationRepo;
        private final SlotTemplateFinder slotRepo;
        private final CooperatorRepository cooperatorRepo;
        private final OdooSyncPort odoo;

        @Inject
        public Handler(SlotRegistrationFinder registrationRepo, SlotTemplateFinder slotRepo,
                       CooperatorRepository cooperatorRepo, OdooSyncPort odoo) {
            this.registrationRepo = registrationRepo;
            this.slotRepo = slotRepo;
            this.cooperatorRepo = cooperatorRepo;
            this.odoo = odoo;
        }

        @Override
        @Transactional
        public Stats handle(SyncPushCommand command) {
            Consumer<LogLine> logSink = command.logSink() != null ? command.logSink() : line -> {};
            Set<Week> weekFilter = command.weeks() == null || command.weeks().isEmpty()
                    ? EnumSet.allOf(Week.class)
                    : EnumSet.copyOf(command.weeks());
            Log.infof("SyncPushCommand: pushing registrations to Odoo (weeks=%s)", weekFilter);
            var allRegistrations = registrationRepo.findAll();
            var registrations = allRegistrations.stream()
                    .filter(reg -> {
                        var slot = slotRepo.findById(reg.slotTemplateId()).orElse(null);
                        return slot != null && weekFilter.contains(slot.week());
                    })
                    .toList();
            int total = registrations.size();
            int processed = 0;
            int created = 0, moved = 0, unchanged = 0, failed = 0;
            String weeksLabel = weekFilter.size() == Week.values().length
                    ? "toutes semaines"
                    : "semaines " + weekFilter.stream().map(Enum::name).sorted().toList();
            logSink.accept(new LogLine("info",
                    "Démarrage de la synchronisation (" + weeksLabel + ") : "
                            + total + " inscription(s) à pousser.",
                    0, total));
            for (var reg : registrations) {
                processed++;
                var slot = slotRepo.findById(reg.slotTemplateId()).orElse(null);
                var coop = cooperatorRepo.findById(reg.cooperatorId()).orElse(null);
                String coopLabel = coop != null
                        ? (coop.firstName() + " " + coop.lastName()).trim()
                        : reg.cooperatorId().toString();
                if (slot == null || coop == null || slot.odooTemplateId() == null || coop.odooPartnerId() == null) {
                    failed++;
                    String msg = "IDs Odoo manquants pour " + coopLabel + " — ignoré.";
                    Log.warnf("Skipping registration %s: missing Odoo IDs", reg.id());
                    logSink.accept(new LogLine("warn", msg, processed, total));
                    continue;
                }
                try {
                    PushOutcome outcome = odoo.pushRegistration(coop.odooPartnerId(), slot.odooTemplateId());
                    String msg = switch (outcome) {
                        case CREATED -> coopLabel + " : inscription créée dans Odoo.";
                        case MOVED -> coopLabel + " : inscription déplacée vers le nouveau créneau.";
                        case UNCHANGED -> coopLabel + " : déjà à jour, rien à faire.";
                    };
                    switch (outcome) {
                        case CREATED -> created++;
                        case MOVED -> moved++;
                        case UNCHANGED -> unchanged++;
                    }
                    logSink.accept(new LogLine("info", msg, processed, total));
                } catch (Exception e) {
                    failed++;
                    Log.errorf("Failed to push registration %s: %s", reg.id(), e.getMessage());
                    logSink.accept(new LogLine("error",
                            coopLabel + " : échec — " + e.getMessage(),
                            processed, total));
                }
            }
            logSink.accept(new LogLine("info",
                    "Terminé : " + created + " créées, " + moved + " déplacées, "
                            + unchanged + " inchangées, " + failed + " échec(s).",
                    processed, total));
            return new Stats(created, moved, unchanged, failed);
        }
    }
}
