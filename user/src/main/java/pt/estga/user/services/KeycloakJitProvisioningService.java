package pt.estga.user.services;

import pt.estga.user.dtos.KeycloakIdentitySnapshot;
import pt.estga.user.entities.User;

public interface KeycloakJitProvisioningService {

    User resolveOrProvision(KeycloakIdentitySnapshot snapshot);
}
