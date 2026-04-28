package fr.sqq.choixcreneaux.application.service;

import fr.sqq.choixcreneaux.application.command.SendReminderCommand;
import fr.sqq.mediator.Mediator;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

/**
 * Background executor for the "remind all pending cooperators" action.
 * Runs on a virtual thread so the HTTP request returns immediately while
 * email sending (which makes synchronous HTTP calls to Brevo) proceeds in
 * the background.
 */
@ApplicationScoped
public class ReminderBulkJob {

    private final Mediator mediator;

    @Inject
    public ReminderBulkJob(Mediator mediator) {
        this.mediator = mediator;
    }

    /** Schedule the bulk reminder on a virtual thread; returns immediately. */
    public void schedule() {
        schedule(false);
    }

    /**
     * Schedule the bulk reminder, optionally restricted to cooperators who have
     * never received a reminder before.
     */
    public void schedule(boolean onlyNeverReminded) {
        // Resolve via CDI so the virtual thread invokes execute() through the
        // proxy — otherwise @ActivateRequestContext would be bypassed.
        ReminderBulkJob self = CDI.current().select(ReminderBulkJob.class).get();
        Thread.ofVirtual().name("reminder-bulk-").start(() -> self.execute(onlyNeverReminded));
    }

    /**
     * Actual work. Called via the CDI proxy so {@code @ActivateRequestContext}
     * starts a fresh request scope (Hibernate session) for the thread.
     */
    @ActivateRequestContext
    public void execute(boolean onlyNeverReminded) {
        try {
            int sent = mediator.send(new SendReminderCommand(null, true, onlyNeverReminded));
            Log.infof("Bulk reminder job finished: %d reminder(s) sent (onlyNeverReminded=%s)", sent, onlyNeverReminded);
        } catch (Exception e) {
            Log.errorf(e, "Bulk reminder job failed");
        }
    }
}
