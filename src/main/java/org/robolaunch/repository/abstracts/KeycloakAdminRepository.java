package org.robolaunch.repository.abstracts;

import java.io.IOException;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.model.account.Organization;
import org.robolaunch.model.account.User;

public interface KeycloakAdminRepository {
    void createRealm(Organization organization) throws ApplicationException;

    void updateFederation(Organization organization);

    void setManagementRoles(User user, Organization organization);

    void forgotPassword(String username);

    void syncFederationMainRealm();

    void syncFederationInCurrentRealm(Organization organization);

    void syncIPAGroupsInCurrentRealm(Organization organization);

    void createClient(Organization organization);

    void addGroupMapper(Organization organization);

    void createGatekeeperClient(Organization organization) throws InternalError, IOException;

    void createOAuthProxyClient(Organization organization);

    void createClientScope(Organization organization) throws InternalError, IOException;

    void deleteRealm(Organization organization) throws InternalError, IOException;

    Boolean isPasswordUpdated(User user);

    String getClientSecret(String organizationName, String clientId);
}
