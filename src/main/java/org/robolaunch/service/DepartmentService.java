package org.robolaunch.service;

import java.io.IOException;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Department;
import org.robolaunch.models.DepartmentBasic;
import org.robolaunch.models.GroupMember;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.Result;
import org.robolaunch.models.User;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.models.response.ResponseTeamMembers;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.KeycloakAdminRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class DepartmentService {
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
  OrganizationService organizationService;

  @Inject
  GroupService groupService;

  @Inject
  JsonWebToken jwt;

  @LoggerName("departmentService")
  Logger departmentLogger;

  public Result createDepartmentError() {
    departmentLogger.error("Error creating department.");
    return new Result("Error creating department. Please try again.", false);
  }

  public Result changeDepartmentNameError(Response changeNameResponse) {
    departmentLogger.error("Error changing department name.");
    return new Result("Error changing department name. " + changeNameResponse.getResourceId(), false);
  }

  public Result deleteDepartmentError(Response deleteDepartmentResponse) {
    departmentLogger.error("Error deleting department .");
    return new Result("Error deleting department. " + deleteDepartmentResponse.getResourceId(), false);
  }

  public Result deleteUserFromDepartmentError() {
    departmentLogger.error("Error deleting user from department.");
    return new Result("Error deleting user from department. ", false);
  }

  public Result deleteUserManagershipFromDepartmentError() {
    departmentLogger.error("Error deleting user managership from department.");
    return new Result("Error deleting user managership from department. ", false);
  }

  /*
   * Checks if the current user(taken from jwt) is the manager of given
   * department.
   * HELPER FUNCTION
   */
  public Boolean isCurrentUserManagerDepartment(Organization organization, DepartmentBasic department)
      throws ApplicationException {
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));

      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();

      Organization dept = new Organization();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          dept.setName(org.getName());
          break;
        }
      }

      Set<User> departmentMemberManagers = groupRepository.getUsers(dept, "membermanager_user");
      Iterator<User> ite = departmentMemberManagers.iterator();
      while (ite.hasNext()) {
        User manager = ite.next();
        if (manager.getUsername().equals(user.getUsername())) {
          return true;
        }
      }
      return false;

    } catch (Exception e) {
      throw new ApplicationException("Error checking if current user is manager of department.");
    }
  }

  /*
   * Checks if the current user(taken from jwt) is the manager of given
   * department.
   * HELPER FUNCTION
   */
  public Boolean isCurrentUserManagerTeam(String teamId)
      throws ApplicationException {
    try {
      User user = new User();
      user.setUsername(jwt.getClaim("preferred_username"));

      Organization dept = new Organization();
      dept.setName(teamId);

      Set<User> teamMemberManagers = groupRepository.getUsers(dept, "membermanager_user");
      Iterator<User> ite = teamMemberManagers.iterator();
      while (ite.hasNext()) {
        User manager = ite.next();
        if (manager.getUsername().equals(user.getUsername())) {
          return true;
        }
      }
      return false;

    } catch (Exception e) {
      throw new ApplicationException("Error checking if current user is manager of department.");
    }
  }

  /*
   * Checks if the given department exists on the given organization
   * HELPERT FUNCTION
   */
  public Boolean doesDepartmentExists(Organization organization, DepartmentBasic department)
      throws ApplicationException {
    try {
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          departmentLogger.info("Department exists.");
          return true;
        }
      }
      departmentLogger.info("Department does not exist.");
      return false;
    } catch (Exception e) {
      departmentLogger.error("Error checking department existence: " + e.getMessage());
      throw new ApplicationException("Error checking department existence.");

    }
  }

  /*
   * Checks if the given team exists on the given organization
   * HELPERT FUNCTION
   */
  public Boolean doesTeamExists(Organization organization, String teamId)
      throws ApplicationException {
    try {
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();
      while (it.hasNext()) {
        Department d = it.next();
        if (d.getId().equals(teamId)) {
          departmentLogger.info("Department exists.");
          return true;
        }
      }
      departmentLogger.info("Department does not exist.");
      return false;
    } catch (Exception e) {
      departmentLogger.error("Error checking department existence: " + e.getMessage());
      throw new ApplicationException("Error checking department existence.");

    }
  }

  /* Creating IPA Group for department. */
  public String createIPAGroupForDepartment(Organization organization, DepartmentBasic department)
      throws ApplicationException {
    try {
      String departmentName = groupAdminRepository.createSubgroup(organization, department);
      departmentLogger.info("IPA Group for department created.");
      return departmentName;
    } catch (Exception e) {
      departmentLogger.error("Error happened when creating IPA Department group: " + e.getMessage());
    }
    return null;
  }

  /*
   * When creating department, set organization's manager to manager of the newly
   * created department.
   */
  public void setInitialManagersForDepartment(Organization organization, String departmentName)
      throws ApplicationException {
    try {
      DepartmentBasic department = new DepartmentBasic();
      department.setName(departmentName);
      groupAdminRepository.setInitialManagersForDepartment(organization, department);
      departmentLogger.info("Initial manager set for department.");
    } catch (Exception e) {
      departmentLogger.error("Error happened when setting initial manager for department: " + e.getMessage());
    }
  }

  /* Adding the given user to the given department. */
  public PlainResponse addUserToDepartment(User user, Organization organization, DepartmentBasic department) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          groupRepository.addUserToGroup(user, org);
          break;
        }
      }
      departmentLogger.info("User added to department.");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User added to department.");
    } catch (Exception e) {
      departmentLogger.error("Error adding user to department: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured while adding user to department.");
    }
    return plainResponse;

  }

  /*
   * Change the description of the group(department) on FreeIPA. We use
   * description field as
   * name.
   */
  public PlainResponse changeDepartmentName(Organization organization, DepartmentBasic department, String newName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      if (department.getName().equals("admins")) {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("You cannot change the name of managers team.");
      }

      Boolean isExists = false;

      ArrayList<Department> departments = groupRepository.getTeams(organization, "member_group");

      Iterator<Department> duplicateIterator = departments.iterator();

      while (duplicateIterator.hasNext()) {
        Organization og = new Organization();
        og.setName(duplicateIterator.next().getId());
        if (groupRepository.getGroupDescription(og).equals(newName)) {
          isExists = true;
          break;
        }
      }

      if (isExists) {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("Team with this name already exists.");
      }

      Iterator<Department> it = departments.iterator();

      Organization dept = new Organization();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          dept.setName(d.getId());
          break;
        }
      }
      groupAdminRepository.changeDepartmentName(organization, dept, newName);
      departmentLogger.info("Team name changed.");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Team name is successfully changed.");
    } catch (Exception e) {
      departmentLogger.error("Error happened when changing department name: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured while changing team name.");
    }

    return plainResponse;
  }

  /* Adding the given user to given department as manager. */
  public void addUserToDepartmentAsManager(User user, Organization organization, DepartmentBasic department)
      throws ApplicationException {
    try {
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();

      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          groupAdminRepository.addUserToGroupAsManager(user, org);
          break;
        }
      }

      departmentLogger.info("User " + user.getUsername() + " added to group as manager");
    } catch (Exception e) {
      departmentLogger.error("Error happened when adding user to group " + e.getMessage());
      throw new ApplicationException("Error happened while adding user to department.");

    }
  }

  /* Delete user from department, also delete from manager lists in case. */
  public PlainResponse deleteUserFromDepartment(User user, Organization organization, DepartmentBasic department) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          groupRepository.removeUserFromGroup(user, org);
          if (groupRepository.isGroupManager(user, org)) {
            groupAdminRepository.removeUserManagerFromGroup(user, org);
          }
          break;
        }
      }
      departmentLogger.info("User " + user.getUsername() + " removed from group");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User removed from the team.");
    } catch (Exception e) {
      departmentLogger.error("Error deleting user from group: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured while deleting user from the team.");
    }
    return plainResponse;
  }

  /* Converting manager to user in department. */
  public PlainResponse deleteUserManagershipFromDepartment(User user, Organization organization,
      DepartmentBasic department) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          groupAdminRepository.removeUserManagerFromGroup(user, org);
          break;
        }
      }
      groupAdminRepository.removeUserManagerFromGroup(user, organization);
      departmentLogger.info("User " + user.getUsername() + " removed from organization");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User managership is removed successfully.");
    } catch (Exception e) {
      departmentLogger.error("Error deleting user managership from group: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured while deleting user managership from the team.");
    }
    return plainResponse;
  }

  /* Deleting a department from organization */
  public PlainResponse deleteDepartment(Organization organization, DepartmentBasic department) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String departmentName = department.getName();
      if (departmentName.equals("admins") && departmentName.equals("invitedUsers")) {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("This team cannot be deleted.");
        return plainResponse;
      }
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();

      Organization dept = new Organization();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(departmentName)) {
          dept.setName(d.getId());
          break;
        }
      }
      groupAdminRepository.deleteGroup(dept);
      departmentLogger.info("Team deleted.");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Team successfully deleted.");
      return plainResponse;
    } catch (Exception e) {
      departmentLogger.error("Error deleting department: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured while deleting team.");
      return plainResponse;
    }
  }

  public Response deleteManagersDepartment(Organization organization) {
    try {
      String departmentName = "managers";
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();

      Organization dept = new Organization();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(departmentName)) {
          dept.setName(d.getId());
          break;
        }
      }
      groupAdminRepository.deleteGroup(dept);
      departmentLogger.info("Department deleted.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());

    }
  }

  public Response deleteInvitedUsersDepartment(Organization organization) {
    try {
      String departmentName = "invitedUsers";
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();

      Organization dept = new Organization();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(departmentName)) {
          dept.setName(d.getId());
          break;
        }
      }
      groupAdminRepository.deleteGroup(dept);
      departmentLogger.info("Department deleted.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());

    }
  }

  /* Get users of the given department. */
  public ResponseTeamMembers getDepartmentUsers(Organization organization, DepartmentBasic department)
      throws ApplicationException {
    ResponseTeamMembers responseTeamMembers = new ResponseTeamMembers();
    try {
      List<Department> departments = groupRepository.getTeams(organization, "member_group");
      Iterator<Department> it = departments.iterator();
      Organization dept = new Organization();
      while (it.hasNext()) {
        Department d = it.next();
        Organization org = new Organization();
        org.setName(d.getId());
        if (groupRepository.getGroupDescription(org).equals(department.getName())) {
          dept.setName(d.getId());
          break;
        }
      }
      ArrayList<GroupMember> members = groupRepository.getGroupMembers(dept);
      departmentLogger.info("Department" + department.getName() + " members sent.");
      responseTeamMembers.setMessage("Team members sent.");
      responseTeamMembers.setSuccess(true);
      responseTeamMembers.setData(members);
    } catch (Exception e) {
      departmentLogger.error("Error sending members of " + department.getName() + ". Error:" + e.getMessage());
      responseTeamMembers.setMessage("Error sending team members.");
      responseTeamMembers.setSuccess(false);
    }
    return responseTeamMembers;

  }

  /* Get managers of the given department. */
  public ArrayList<User> getTeamManagers(Organization organization, String teamId) {
    try {
      ArrayList<User> teamManagers = new ArrayList<>();
      Organization team = new Organization();
      team.setName(teamId);
      JsonNode members = groupAdminRepository.getGroupField(team, "membermanager_user");
      for (int i = 0; i < members.size(); i++) {
        System.out.println("User: " + userAdminRepository.getUserByUsername(members.get(i).asText()));
        teamManagers.add(userAdminRepository.getUserByUsername(members.get(i).asText()));
      }
      departmentLogger.info("Team managers sent");
      return teamManagers;
    } catch (Exception e) {
      departmentLogger.error("Error sending managers.");
      return null;
    }
  }

  public Response addCreatedIPAGroupAsMember(Organization organization, String departmentName)
      throws ApplicationException {
    try {
      DepartmentBasic department = new DepartmentBasic();
      department.setName(departmentName);
      groupRepository.addSubgroupToGroup(organization, department);
      departmentLogger.info("IPA Group for department added as member to organization.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      departmentLogger.error("Error happened when adding IPA Department group as member: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  /* Adding given department to the given organization as subgroup. */
  public Response addIPAGroupAsMember(Organization organization, DepartmentBasic department)
      throws ApplicationException {
    try {
      String departmentName = groupAdminRepository.getGroupNameFromDescription(organization, department.getName());
      System.out.println("Department name:::::::::.. " + departmentName + " === " + department.getName());
      groupRepository.addSubgroupToGroup(organization, department);
      departmentLogger.info("IPA Group for department added as member to organization.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      departmentLogger.error("Error happened when adding IPA Department group as member: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public String createManagersDepartment(Organization organization) throws ApplicationException {
    try {
      DepartmentBasic managersDepartment = new DepartmentBasic();
      managersDepartment.setName("managers");
      System.out.println("Organization - department will be created: " + organization.getName());
      String departmentName = groupAdminRepository.createSubgroup(organization, managersDepartment);
      departmentLogger.info("Managers group created.");
      return departmentName;
    } catch (Exception e) {
      departmentLogger.error("Error happened when creating IPA Group for organization " + e.getMessage());
      return null;
    }
  }

  public Response addManagersDepartmentAsMember(Organization organization, String department)
      throws ApplicationException {
    try {
      DepartmentBasic managersDepartment = new DepartmentBasic();
      managersDepartment.setName(department);
      groupRepository.addSubgroupToGroup(organization, managersDepartment);
      departmentLogger.info("Managers group added as member.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      departmentLogger.error("Error happened when creating IPA Group for organization " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public Response addFounderToManagersDepartment(Organization organization, String department)
      throws InternalError, IOException, ApplicationException {
    try {
      setInitialManagersForDepartment(organization, department);
      departmentLogger.info("Founder added to managers group.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      departmentLogger.error("Error happened when adding founder to managers group " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }

  }
}
