package org.robolaunch.service;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.LoginRequest;
import org.robolaunch.models.LoginResponse;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.User;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.KeycloakRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class KeycloakService {
  @Inject
  Logger log;

  @Inject
  AccountService accountService;

  @Inject
  KeycloakRepository keycloakRepository;

  @Inject
  GroupRepository groupRepository;

  @Inject
  GroupAdminRepository groupAdminRepository;

  @Inject
  UserAdminRepository userAdminRepository;

  @Inject
  UserRepository userRepository;

  @Inject
  KeycloakAdminRepository keycloakAdminRepository;

  @Inject
  OrganizationService organizationService;

  @Inject
  JsonWebToken jwt;

  @LoggerName("keycloakService")
  Logger keycloakLogger;

  public Response createClientScope(Organization organization) {
    try {
      keycloakAdminRepository.createClientScope(organization);
      keycloakLogger.info("Client scope created for organization: " + organization.getName());
      return new Response(true, "Client scope created successfully");
    } catch (Exception e) {
      keycloakLogger.error("Error creating client scope for organization: " + organization.getName());
      return new Response(false,
          "Error creating client scope for organization: " + organization.getName());
    }

  }

  public Response createGatekeeperClient(Organization organization) {
    try {
      keycloakAdminRepository.createGatekeeperClient(organization);
      keycloakLogger.info("Gatekeeper client created for organization: " + organization.getName());
      return new Response(true, "Gatekeeper client created successfully.");
    } catch (Exception e) {
      keycloakLogger.error("Error creating gatekeeper client for organization: " + organization.getName());
      return new Response(false, "Gatekeeper cannot be created.");
    }
  }

  public Response createOAuthProxyClient(Organization organization) {
    try {
      keycloakAdminRepository.createOAuthProxyClient(organization);
      keycloakLogger.info("OAuth proxy client created for organization: " + organization.getName());
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      keycloakLogger.error("Error creating oauth proxy client for organization: " + organization.getName());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public void deleteRealm(Organization organization) {
    try {
      keycloakAdminRepository.deleteRealm(new Organization(organization.getName(), organization.isEnterprise()));
      keycloakLogger.info("Realm deleted for organization: " + organization.getName());
    } catch (Exception e) {
      keycloakLogger.error("Error deleting realm for organization: " + "first-new");
    }
  }

  /* Create realm on keycloak for organization. */
  public Response createRealmForOrganization(Organization organization) throws ApplicationException {
    try {
      keycloakLogger.warn("Creating realm for organization: " + organization.getName());
      keycloakAdminRepository.createRealm(organization);
      keycloakLogger.info("Realm " + organization.getName() + " created");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  /* Create realm on keycloak for organization. */
  public Response createRealmForDefaultOrganization(Organization organization) throws ApplicationException {
    try {
      keycloakAdminRepository.createRealm(organization);
      keycloakLogger.info("Realm " + organization.getName() + " created");
      return new Response(true,
          UUID.randomUUID().toString());
    } catch (ApplicationException e) {
      keycloakLogger.error("Error happened while creating organization realm." + e.getMessage());
      return new Response(false,
          UUID.randomUUID().toString());
    }
  }

  /* Create realm on keycloak for organization. */
  public Organization deleteRealmForDefaultOrganization(Organization organization)
      throws ApplicationException, InternalError, IOException {
    try {
      keycloakAdminRepository.deleteRealm(organization);
      keycloakLogger.info("Realm " + organization.getName() + " deleted");
      return organization;
    } catch (Exception e) {
      throw new ApplicationException(e.getMessage());
    }
  }

  /* Setting keycloak for the organization. */
  public Response setKeycloakSettings(Organization organization)
      throws ApplicationException, InternalError, IOException {
    try {
      keycloakAdminRepository.updateFederation(organization);
      keycloakLogger.info("Realm " + organization.getName() + " updated");
      return new Response(true,
          UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false,
          UUID.randomUUID().toString());
    }
  }

  /* Setting keycloak for the organization. */
  public Response setKeycloakSettingsForDefaultOrganization(String defaultOrgName)
      throws ApplicationException, InternalError, IOException {
    try {
      Organization organization = new Organization();
      organization.setName(defaultOrgName);
      keycloakAdminRepository.updateFederation(organization);
      keycloakLogger.info("Realm " + organization.getName() + " updated again");
      return new Response(true,
          UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false,
          UUID.randomUUID().toString());
    }
  }

  /* Adding group mappers on keycloak */
  public Response addGroupMapper(Organization organization) throws ApplicationException, InternalError, IOException {
    try {
      keycloakAdminRepository.addGroupMapper(organization);
      keycloakAdminRepository.syncFederationMainRealm();
      keycloakLogger.info("Group Mapper added for organization: " + organization.getName() + "");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  /* Adding group mappers on keycloak */
  public Response addGroupMapperForDefaultOrganization(String defaultOrgName)
      throws ApplicationException, InternalError, IOException {
    try {
      Organization organization = new Organization();
      organization.setName(defaultOrgName);
      keycloakAdminRepository.addGroupMapper(organization);
      keycloakLogger.info("Group Mapper added for organization: " + organization.getName() + "");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  /* Running LDAP synchronozation on Keycloak. */
  public Response syncIPAGroupsInCurrentKeycloakRealm(Organization organization) throws ApplicationException {

    try {
      keycloakAdminRepository.syncIPAGroupsInCurrentRealm(organization);
      keycloakAdminRepository.syncFederationMainRealm();
      keycloakLogger.info("IPA Groups synced in current keycloak realm.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  /* Set current user(who created the organization) as admin on Keycloak */
  public Response setAsAdmin(Organization organization) throws ApplicationException, InternalError, IOException {
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));

      keycloakAdminRepository.setManagementRoles(user, organization);
      keycloakLogger.info("User set as admin");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public Response setRegisteredUserAsAdmin(User user, Organization organization) throws ApplicationException {
    try {
      keycloakAdminRepository.setManagementRoles(user, organization);
      keycloakLogger.info("User set as admin");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public Response syncMainFederation() throws ApplicationException, IOException {
    try {
      keycloakAdminRepository.syncFederationMainRealm();
      keycloakLogger.info("Main user federation synced");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public void syncRealmUserFederation(Organization organization)
      throws InternalError, IOException, ApplicationException {
    try {
      keycloakAdminRepository.syncFederationInCurrentRealm(organization);
      keycloakLogger.info("Main user federation synced");
    } catch (Exception e) {
      throw new ApplicationException("Error creating user, please try again.");

    }
  }

  public void forgotPasswordWithUser(User user) throws ApplicationException, IOException {
    try {
      keycloakAdminRepository.forgotPassword(user.getUsername());
      keycloakLogger.info("User password email sent to " + user.getUsername() + "");
    } catch (Exception e) {
      keycloakLogger.error("Error happened when password email sent: " + e);
      throw new ApplicationException("Email already exists.");

    }
  }

  public PlainResponse forgotPasswordWithEmail(String email) throws IOException {
    PlainResponse plainResponse = new PlainResponse();
    try {
      User user = userAdminRepository.getUserByEmail(email);
      keycloakAdminRepository.forgotPassword(user.getUsername());
      keycloakLogger.info("User password email sent to " + email + "");
      plainResponse.setSuccess(true);
      plainResponse
          .setMessage("An email with link to reset password will be sent to " + email + " if it matches our records."
              + "Please check your email.");
      return plainResponse;
    } catch (ApplicationException e) {
      keycloakLogger.error("Error happened when password email sent: " + e);
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Error sending password mail. Please try again.");
      return plainResponse;
    }
  }

  public Response sendPasswordMailRegisteredUser(User user) throws ApplicationException {
    try {
      keycloakAdminRepository.forgotPassword(user.getUsername());
      keycloakLogger.info("User password email sent to " + user.getUsername() + "");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      keycloakLogger.error("Error password email send: " + e);
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public PlainResponse changePassword(LoginRequest loginRequest) throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));
      if (!user.getUsername().equals(loginRequest.getUsername())) {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("User does not exist.");
        return plainResponse;
      }
      LoginResponse loginResponse = accountService.login(loginRequest);
      if (loginResponse.getIdToken() != null) {
        keycloakAdminRepository.forgotPassword(user.getUsername());
        keycloakLogger.info("User change password mail sent.");
        plainResponse.setSuccess(true);
        plainResponse.setMessage("Change password mail is sent to user.");
        return plainResponse;
      } else {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("You are not authorized.");
        return plainResponse;
      }

    } catch (Exception e) {
      keycloakLogger.error("Error happened when password change mail sent: " + e);
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured changing the password. Please try again.");
      return plainResponse;
    }
  }

  public Response isPasswordUpdated(User user) {
    try {
      Boolean isUpdated = keycloakAdminRepository.isPasswordUpdated(user);
      if (isUpdated) {
        return new Response(true, UUID.randomUUID().toString());
      } else {
        return new Response(false, UUID.randomUUID().toString());
      }
    } catch (Exception e) {
      keycloakLogger.error("Error happened when password updated: " + e);
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public String getClientSecret(String organizationName, String clientId) {
    try {
      return keycloakAdminRepository.getClientSecret(organizationName, clientId);
    } catch (Exception e) {
      return null;
    }
  }

}
