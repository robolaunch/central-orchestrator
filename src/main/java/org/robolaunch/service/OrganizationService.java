package org.robolaunch.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Department;
import org.robolaunch.models.GroupMember;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.Result;
import org.robolaunch.models.User;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.models.response.ResponseOrganizationMembers;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

import io.quarkus.arc.log.LoggerName;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@ApplicationScoped
public class OrganizationService {
  DynamicGraphQLClient graphqlClient;

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
  JsonWebToken jwt;

  @LoggerName("organizationService")
  Logger organizationLogger;

  @ConfigProperty(name = "kogito.dataindex.http.url")
  String dataIndexUrl;

  @PostConstruct
  public void initializeApis() throws IOException {
    this.graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
        .url(dataIndexUrl + "/graphql/")
        .build();

  }

  public Result errorHandler() {
    organizationLogger.info("Error occured organization service");
    return new Result("Error creating organization. Please try again.", false);
  }

  public Boolean getOrganizationType(Organization organization) throws ApplicationException {
    try {
      Boolean organizationType = organization.isEnterprise();
      return organizationType;
    } catch (Exception e) {
      organizationLogger.error("Error while getting organization type", e);
      throw new ApplicationException("Error while getting organization type", e);
    }
  }

  public Result successResult() {
    return new Result("Organization created successfully.", true);
  }

  /* Does organization exist */
  public Boolean doesOrganizationExist(Organization organization) throws InternalError, IOException {
    try {
      Boolean doesOrganizationExist;
      if (organization.isEnterprise()) {
        doesOrganizationExist = groupAdminRepository.doesGroupExist(organization);
      } else {
        Organization individualOrganization = new Organization();
        individualOrganization.setName("org-" + organization.getName());
        doesOrganizationExist = groupAdminRepository.doesGroupExist(individualOrganization);
      }
      organizationLogger.info("Organization exists: " + doesOrganizationExist);
      return doesOrganizationExist;
    } catch (Exception e) {
      organizationLogger.error("Error while checking if organization exists", e);
      return null;
    }
  }

  /* Does organization exist */
  public Boolean doesOrganizationExistForRoboticsCloud(Organization organization) throws InternalError, IOException {
    try {
      Boolean doesOrganizationExist = groupAdminRepository.doesGroupExist(organization);
      organizationLogger.info("Organization exists: " + doesOrganizationExist);
      return doesOrganizationExist;
    } catch (Exception e) {
      organizationLogger.error("Error while checking if organization exists", e);
      return null;
    }
  }

  /* Creating IPA Group for organization. */
  public Response createIPAGroupForOrganization(Organization organization)
      throws ApplicationException, InternalError, IOException {
    try {
      /*
       * Check if organization name has one of the following, if so, throw error. May
       * be converted to regex in the future.
       */
      if (organization.getName().contains("-")) {
        if (organization.getName().split("-").length > 2) {
          return new Response(false, UUID.randomUUID().toString());
        }
      }
      if (organization.getName().contains(" ")
          || organization.getName().contains("_") || organization.getName().contains("$")) {
        return new Response(false, UUID.randomUUID().toString());
      }
      groupAdminRepository.createGroup(organization);
      organizationLogger.info("Organization created: " + organization.getName());
      return new Response(true, UUID.randomUUID().toString());
    } catch (ApplicationException e) {
      organizationLogger.error("Error happened when creating IPA Group for organization " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public Organization formatDefaultOrganization(User user) throws InternalError, IOException {
    String suffix = "default-org-" + user.getUsername();
    Organization organization = new Organization();
    organization.setName(suffix);
    return organization;
  }

  public Response createDefaultOrganization(Organization organization)
      throws ApplicationException, InternalError, IOException {
    try {
      groupAdminRepository.createGroup(organization);
      organizationLogger.info("IPA Group for default organization created.");
      return new Response(true,
          UUID.randomUUID().toString());
    } catch (ApplicationException e) {
      return new Response(false,
          UUID.randomUUID().toString());
    }

  }

  public void deleteDefaultOrganization(Organization organization)
      throws ApplicationException, InternalError, IOException {
    try {
      groupAdminRepository.deleteGroup(organization);
      organizationLogger.info("IPA Group for default organization deleted.");
    } catch (ApplicationException e) {
      throw new ApplicationException(e.getMessage());
    }

  }

  public void addUserToDefaultOrganization(User user, Organization organization)
      throws ApplicationException, InternalError, IOException {
    try {
      groupAdminRepository.addUserToGroup(user, organization);
      groupAdminRepository.addUserToGroupAsManager(user, organization);
    } catch (ApplicationException e) {
      organizationLogger.error("Error adding user to default organization " + e.getMessage());
      throw new ApplicationException(e.getMessage());
    }
  }

  /* Get users of the given organization. */
  public ResponseOrganizationMembers getOrganizationUsers(Organization organization) throws ApplicationException {
    ResponseOrganizationMembers responseOrganizationMembers = new ResponseOrganizationMembers();
    try {
      ArrayList<GroupMember> members = groupRepository.getGroupMembers(organization);
      responseOrganizationMembers.setSuccess(true);
      responseOrganizationMembers.setMessage("Organization users sent.");
      responseOrganizationMembers.setData(members);
      organizationLogger.info("Members sent.");
    } catch (Exception e) {
      organizationLogger.error("Error sending members: " + e.getMessage());
      responseOrganizationMembers.setSuccess(false);
      responseOrganizationMembers.setMessage("Error sending members.");
    }
    return responseOrganizationMembers;
  }

  /*
   * Delete user from organization, loop over the subgroups(departments) and
   * delete from subgroups too.
   * Also delete from manager lists of the subgroups.
   */
  public PlainResponse deleteUserFromOrganization(User user, Organization organization) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      Boolean isMemberOrganization = groupRepository.isGroupMember(user, organization);

      System.out.println("Is member of upper org: " + isMemberOrganization);
      User currentUser = new User();
      currentUser.setUsername(jwt.getClaim("preferred_username"));

      if (currentUser.getUsername() == user.getUsername()) {
        throw new ApplicationException("You cannot delete yourself from organization.");
      }

      if (isMemberOrganization) {
        if (groupRepository.isGroupManager(user, organization)) {
          Set<User> managers = groupRepository.getUsers(organization, "membermanager_user");
          if (managers.size() == 1) {
            plainResponse.setSuccess(false);
            plainResponse.setMessage("Only manager cannot be deleted from organization.");
            return plainResponse;
          }
          groupAdminRepository.removeUserManagerFromGroup(user, organization);
        }
        groupRepository.removeUserFromGroup(user, organization);
      }

      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      departments.forEach(department -> {
        /* department's description is read as its name on FreeIPA */
        Organization lowerGroup = new Organization();
        lowerGroup.setName(department.getId());
        try {
          Boolean isMember = groupRepository.isGroupMember(user, lowerGroup);
          if (isMember) {
            if (groupRepository.isGroupManager(user, lowerGroup)) {
              groupAdminRepository.removeUserManagerFromGroup(user, lowerGroup);
            }
            groupRepository.removeUserFromGroup(user, lowerGroup);

          }
        } catch (MalformedURLException e) {
          e.printStackTrace();
        } catch (InternalError e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (ApplicationException e) {
          e.printStackTrace();
        }
      });
      organizationLogger.info("User " + user.getUsername() + " removed from organization");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User deleted from organization.");
    } catch (Exception e) {
      organizationLogger.error("Error deleting user from group: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured while deleting user from organization.");
    }
    return plainResponse;
  }

  /* Converting manager to user in organization. */
  public PlainResponse deleteUserManagershipFromOrganization(User user, Organization organization) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      groupAdminRepository.removeUserManagerFromGroup(user, organization);
      organizationLogger.info("User " + user.getUsername() + " removed from organization");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User managership deleted from organization.");
    } catch (Exception e) {
      organizationLogger.error("Error deleting user managership from group: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured while deleting user managership from organization.");
    }
    return plainResponse;
  }

  /* Get all departments of the given organization. */
  public ArrayList<Department> getDepartments(Organization organization) throws ApplicationException {
    try {
      ArrayList<Department> departments = groupRepository.getTeams(organization, "member_group");
      organizationLogger.info("Departments sent.");
      return departments;
    } catch (Exception e) {
      organizationLogger.error("Error sending departments: " + e.getMessage());
      throw new ApplicationException("Cannot get departments.");

    }
  }

  /* Adding the given user to given organization as manager */
  public PlainResponse addUserToOrganizationAsManager(User user, Organization organization) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      groupAdminRepository.addUserToGroupAsManager(user, organization);
      organizationLogger.info("User " + user.getUsername() + " added to group as manager");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User added to organization as manager.");
    } catch (Exception e) {
      organizationLogger.error("Error happened when adding user to group " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured adding user to organization as manager.");
    }
    return plainResponse;
  }

  public void deleteOrganizationGroup(Organization organization) throws ApplicationException {
    try {
      if (organization.getName().contains("org-default-")) {
        throw new ApplicationException("Default organization cannot be deleted.");
      }
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        groupAdminRepository.deleteGroup(org);
      }

      groupAdminRepository.deleteGroup(organization);
      organizationLogger.info("Organization deleted.");
    } catch (Exception e) {
      organizationLogger.error("Error deleting organization: " + e.getMessage());
      throw new ApplicationException("Error deleting organization.");

    }
  }

  /*
   * Checks if the given user is the manager of given organization
   * HELPER FUNCTION
   */
  public Boolean isUserManagerOrganization(User user, Organization organization) {
    try {
      Boolean isManager = groupRepository.isGroupManager(user, organization);
      return isManager;
    } catch (Exception e) {
      organizationLogger.error("Error checking if user is manager of organization: " + e.getMessage());
    }
    return false;

  }

  /*
   * Checks if the current user(taken from jwt) is the manager of given
   * organization
   * HELPER FUNCTION
   */
  public Boolean isCurrentUserManager(Organization organization) throws InternalError, IOException {
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));
      Boolean isManager = false;

      Set<User> parentMemberManagers = groupRepository.getUsers(organization, "membermanager_user");
      Iterator<User> it = parentMemberManagers.iterator();

      while (it.hasNext()) {
        User manager = it.next();
        if (manager.getUsername().equals(user.getUsername())) {
          isManager = true;
          break;
        }
      }
      return isManager;
    } catch (Exception e) {
      throw new ApplicationException("Error checking if user is manager of organization.");
    }

  }

  /*
   * Checks if the given user is present on the given organization.
   * HELPER FUNCTION
   */
  public Boolean isUserPresentInOrganization(User user, Organization organization) {
    try {
      Boolean isMemberUpperOrg = groupRepository.isGroupMember(user, organization);
      if (isMemberUpperOrg) {
        return true;
      } else {
        throw new ApplicationException("User is not a member of this organization.");
      }
    } catch (Exception e) {
      organizationLogger.error("Error checking if user is present in organization: " + e.getMessage());
      return false;
    }
  }

  /*
   * Get group members, it only fetches members(not managers), so every manager
   * must be a member of the current group.
   */
  public List<GroupMember> getGroupMembers(Organization organization) throws ApplicationException {
    try {
      List<GroupMember> members = groupRepository.getGroupMembers(organization);
      organizationLogger.info("Members sent.");
      return members;
    } catch (Exception e) {
      organizationLogger.error("Error sending members: " + e.getMessage());
      throw new ApplicationException("An error occured when getting users.");

    }
  }

  public ArrayList<Organization> getUserOrganizations() throws ApplicationException {
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));
      ArrayList<Organization> organizations = userRepository.getOrganizations(user);
      organizationLogger.info("User " + user.getUsername() + " organizations fetched");
      return organizations;
    } catch (Exception e) {
      organizationLogger.error("Error happend getting user group: " + e);
      throw new ApplicationException("Error getting user organization.");
    }
  }

  public Integer getRoboticsCloudCount(Organization organization, String departmentName) {
    System.out.println("Definitely been here!");
    System.out.println("Definitely been here!");

    /*
     * try {
     * String queryStr =
     * "{InitializeRoboticsCloud(where: {and: [{organization: {name: {equal:\""
     * + organization.getName()
     * + "\"}}},{departmentName: {equal:\""
     * + departmentName
     * + "\"}}]}) {id}}";
     * 
     * io.smallrye.graphql.client.Response response =
     * graphqlClient.executeSync(queryStr);
     * 
     * javax.json.JsonObject data = response.getData();
     * if (data != null) {
     * return data.getJsonArray("InitializeRoboticsCloud").size();
     * } else {
     * return 0;
     * }
     * } catch (Exception e) {
     * System.out.println("Error getting robotics cloud count: " + e);
     * return null;
     * }
     */
    return 4;
  }

  public Boolean isOrganizationEnterprise(Organization organization) {
    if (organization.getName().startsWith("org-")) {
      organizationLogger.info("Organization is not enterprise.");
      return false;
    } else {
      organizationLogger.error("Organization is enterprise.");
      return true;
    }
  }

}
