package pt.estga.shared.audit;

import org.hibernate.envers.RevisionListener;
import pt.estga.shared.models.AuditActor;
import pt.estga.shared.utils.SecurityUtils;

public class AppRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        SecurityUtils.currentPrincipal().ifPresent(principal -> {
            ((AuditRevisionEntity) revisionEntity).setActor(
                    new AuditActor(
                            principal.getId(),
                            principal.getIdentifier()
                    )
            );
        });
    }
}
