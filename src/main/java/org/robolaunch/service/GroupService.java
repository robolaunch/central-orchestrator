package org.robolaunch.service;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.core.abstracts.IPAAdmin;
import org.robolaunch.core.concretes.IPAAdminLogin;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.User;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class GroupService {
  @Inject
  Logger log;

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
  AccountService accountService;

  @Inject
  OrganizationService organizationService;

  @Inject
  DepartmentService departmentService;

  @Inject
  JsonWebToken jwt;

  @LoggerName("groupService")
  Logger groupLogger;

  /*
   * Granting admin privileges, needed when user permissions not enough.
   * HELPER FUNCTION.
   */
  public void grantAdminPrivileges() {
    try {
      IPAAdmin adminLogin = new IPAAdminLogin();
      List<HttpCookie> cookies = adminLogin.login();
      cookies.forEach(cookie -> {
        groupAdminRepository.appendCookie(cookie);
        userAdminRepository.appendCookie(cookie);
      });
      groupLogger.info("Admin privileges granted");
    } catch (Exception e) {
      groupLogger.error("Error happened when granting admin privileges " + e.getMessage());
    }
  }

  public void addUserToIPAGroup(User user, Organization organization) throws ApplicationException {
    try {
      groupAdminRepository.addUserToGroup(user, organization);
      groupLogger.info("User " + user.getUsername() + " added user to group");
    } catch (Exception e) {
      groupLogger.error("Error happened when adding user to group " + e.getMessage());
      throw new ApplicationException("Error happened while adding user to organization. Please try again.");

    }
  }

  /* Add as user and as manager to group(who created the organization) */
  public Response addFounderToIPAGroup(Organization organization) throws ApplicationException {
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));
      System.out.println("Keske: " + jwt.claim("preferred_username"));
      System.out.println("fdss: " + jwt.getIssuer());
      System.out.println("Token: " + jwt.containsClaim("preferred_username"));
      System.out.println("Token: " + jwt.getRawToken());
      System.out.println("Username: " + user.getUsername());
      System.out.println("bu da başka bir videonun konusu olsun");
      groupAdminRepository.addUserToGroup(user, organization);
      groupAdminRepository.addUserToGroupAsManager(user, organization);
      groupLogger.info("Admin " + user.getUsername() + " added to group as manager");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      groupLogger.error("Error happened when adding user to group " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public void assignToDefaultGroup(User user) throws ApplicationException, IOException {
    try {
      Organization organization = new Organization();
      organization.setName("fm_users");
      groupAdminRepository.addUserToGroup(user, organization);
      groupLogger.info("User " + user.getUsername() + " assigned to default group");
    } catch (ApplicationException e) {
      throw new ApplicationException(e.getMessage());
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

  public void jwtTest() {
    try {
      System.out.println("username: " + jwt.getClaim("preferred_username"));
      System.out.println("issuer: " + jwt.getIssuer());
      System.out.println("Username?: " + jwt.containsClaim("preferred_username"));
      System.out.println("Token: " + jwt.getRawToken());
      System.out.println("Name: " + jwt.getName());
      System.out.println("claim names: " + jwt.getClaimNames());
    } catch (Exception e) {
      // TODO: handle exception
    }
  }

}
