package fr.sqq.choixcreneaux.infrastructure.in.audit;

import fr.sqq.mediator.Command;
import fr.sqq.mediator.PipelineBehavior;
import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuditPipelineBehavior implements PipelineBehavior {

    private static final Logger LOG = Logger.getLogger(AuditPipelineBehavior.class);

    @Inject
    Instance<SecurityIdentity> securityIdentityInstance;

    @Inject
    @IdToken
    Instance<JsonWebToken> idToken;

    @Override
    public <R> R handle(Command<R> command, Next<R> next) {
        String adminEmail = resolveAdminEmail();
        if (adminEmail == null) {
            return next.invoke();
        }

        String commandName = command.getClass().getSimpleName();
        LOG.infof("AUDIT | admin=%s | commande=%s | payload=%s", adminEmail, commandName, command);
        try {
            R result = next.invoke();
            LOG.infof("AUDIT | admin=%s | commande=%s | resultat=succes", adminEmail, commandName);
            return result;
        } catch (Exception e) {
            LOG.errorf("AUDIT | admin=%s | commande=%s | resultat=echec | erreur=%s",
                    adminEmail, commandName, e.getMessage());
            throw e;
        }
    }

    private String resolveAdminEmail() {
        if (!securityIdentityInstance.isResolvable()) {
            return null;
        }

        SecurityIdentity identity = securityIdentityInstance.get();
        if (identity.isAnonymous()) {
            return null;
        }

        if (idToken.isResolvable()) {
            JsonWebToken token = idToken.get();
            Object email = token.getClaim("email");
            if (email != null) {
                return email.toString();
            }
        }

        return identity.getPrincipal().getName();
    }
}
