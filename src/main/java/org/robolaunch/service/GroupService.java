package org.robolaunch.service;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.model.account.Organization;
import org.robolaunch.model.account.User;
import org.robolaunch.model.response.PlainResponse;
import org.robolaunch.repository.abstracts.AccountRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class GroupService {
  @Inject
  Logger log;

  @Inject
  AccountRepository accountRepository;

  @Inject
  KeycloakAdminRepository keycloakAdminRepository;

  @Inject
  AccountService accountService;

  @Inject
  OrganizationService organizationService;

  @Inject
  TeamService departmentService;

  @Inject
  JsonWebToken jwt;

  @LoggerName("groupService")
  Logger groupLogger;

  public void addUserToIPAGroup(User user, Organization organization) throws ApplicationException {
    try {
      accountRepository.addUserToGroup(user, organization);
      groupLogger.info("User " + user.getUsername() + " added user to group");
    } catch (Exception e) {
      groupLogger.error("Error happened when adding user to group " + e.getMessage());
      throw new ApplicationException("Error happened while adding user to organization. Please try again.");

    }
  }

  /* Add as user and as manager to group(who created the organization) */
  public PlainResponse addFounderToIPAGroup(Organization organization) throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));
      accountRepository.addUserToGroup(user, organization);
      accountRepository.addUserToGroupAsManager(user, organization);

      // Add Also Big Boss to the group. This is needed for managing robotics clouds.
      User bigBossUser = new User();
      bigBossUser.setUsername("bigboss");
      accountRepository.addUserToGroup(bigBossUser, organization);

      groupLogger.info("Admin " + user.getUsername() + " added to group as manager");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User added to group");
    } catch (Exception e) {
      groupLogger.error("Error happened when adding user to group " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error happened while adding user to organization. Please try again.");
    }
    return plainResponse;
  }

  public void assignToDefaultGroup(User user) throws ApplicationException, IOException {
    try {
      Organization organization = new Organization();
      organization.setName("fm_users");
      accountRepository.addUserToGroup(user, organization);
      groupLogger.info("User " + user.getUsername() + " assigned to default group");
    } catch (ApplicationException e) {
      groupLogger.error("Error happened when assigning user to default group " + e.getMessage());
    }
  }

  public Organization convertOrganizationName(Organization organization) {
    try {
      Organization newOrganization = new Organization();

      newOrganization.setName("org-" + organization.getName());
      newOrganization.setEnterprise(organization.isEnterprise());
      return newOrganization;
    } catch (Exception e) {
      return null;
    }
  }

  public Organization convertOrganizationNameForEnterprise(Organization organization) {
    try {
      Organization newOrganization = new Organization();
      newOrganization.setName(organization.getName());
      newOrganization.setEnterprise(organization.isEnterprise());
      return newOrganization;
    } catch (Exception e) {
      return null;
    }
  }
}
