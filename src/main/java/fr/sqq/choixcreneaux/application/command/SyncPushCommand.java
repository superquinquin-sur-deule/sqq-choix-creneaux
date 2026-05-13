package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
        public Stats handle(SyncPushCommand command) {
            Consumer<LogLine> logSink = command.logSink() != null ? command.logSink() : line -> {};
            Set<Week> weekFilter = command.weeks() == null || command.weeks().isEmpty()
                    ? EnumSet.allOf(Week.class)
                    : EnumSet.copyOf(command.weeks());
            Log.infof("SyncPushCommand: pushing registrations to Odoo (weeks=%s)", weekFilter);
            List<Job> jobs = QuarkusTransaction.requiringNew().call(() -> prepareJobs(weekFilter));
            int total = jobs.size();
            int processed = 0;
            int created = 0, moved = 0, unchanged = 0, failed = 0;
            String weeksLabel = weekFilter.size() == Week.values().length
                    ? "toutes semaines"
                    : "semaines " + weekFilter.stream().map(Enum::name).sorted().toList();
            logSink.accept(new LogLine("info",
                    "Démarrage de la synchronisation (" + weeksLabel + ") : "
                            + total + " inscription(s) à pousser.",
                    0, total));
            for (Job job : jobs) {
                processed++;
                if (job.missingIds()) {
                    failed++;
                    String msg = "IDs Odoo manquants pour " + job.coopLabel() + " — ignoré.";
                    Log.warnf("Skipping registration %s: missing Odoo IDs", job.registrationId());
                    logSink.accept(new LogLine("warn", msg, processed, total));
                    continue;
                }
                try {
                    PushOutcome outcome = odoo.pushRegistration(job.odooPartnerId(), job.odooTemplateId());
                    String msg = switch (outcome) {
                        case CREATED -> job.coopLabel() + " : inscription créée dans Odoo.";
                        case MOVED -> job.coopLabel() + " : inscription déplacée vers le nouveau créneau.";
                        case UNCHANGED -> job.coopLabel() + " : déjà à jour, rien à faire.";
                    };
                    switch (outcome) {
                        case CREATED -> created++;
                        case MOVED -> moved++;
                        case UNCHANGED -> unchanged++;
                    }
                    logSink.accept(new LogLine("info", msg, processed, total));
                } catch (Exception e) {
                    failed++;
                    Log.errorf("Failed to push registration %s: %s", job.registrationId(), e.getMessage());
                    logSink.accept(new LogLine("error",
                            job.coopLabel() + " : échec — " + e.getMessage(),
                            processed, total));
                }
            }
            logSink.accept(new LogLine("info",
                    "Terminé : " + created + " créées, " + moved + " déplacées, "
                            + unchanged + " inchangées, " + failed + " échec(s).",
                    processed, total));
            return new Stats(created, moved, unchanged, failed);
        }

        private List<Job> prepareJobs(Set<Week> weekFilter) {
            var registrations = registrationRepo.findAll();
            List<Job> jobs = new ArrayList<>(registrations.size());
            for (var reg : registrations) {
                var slot = slotRepo.findById(reg.slotTemplateId()).orElse(null);
                if (slot == null || !weekFilter.contains(slot.week())) continue;
                var coop = cooperatorRepo.findById(reg.cooperatorId()).orElse(null);
                String coopLabel = coop != null
                        ? (coop.firstName() + " " + coop.lastName()).trim()
                        : reg.cooperatorId().toString();
                boolean missing = coop == null
                        || slot.odooTemplateId() == null
                        || coop.odooPartnerId() == null;
                jobs.add(new Job(
                        reg.id(),
                        coopLabel,
                        missing ? 0L : coop.odooPartnerId(),
                        missing ? 0L : slot.odooTemplateId(),
                        missing));
            }
            return jobs;
        }

        private record Job(java.util.UUID registrationId, String coopLabel,
                           long odooPartnerId, long odooTemplateId, boolean missingIds) {}
    }
}
