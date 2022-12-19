package org.robolaunch.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.core.abstracts.RandomGenerator;
import org.robolaunch.core.concretes.RandomGeneratorImpl;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.DepartmentBasic;
import org.robolaunch.models.GroupMember;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.Result;
import org.robolaunch.models.User;
import org.robolaunch.models.response.ResponseTeamMembers;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;
import org.robolaunch.repository.abstracts.UserAdminRepository;
import org.robolaunch.repository.abstracts.UserRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class InviteService {
  @Inject
  Logger log;

  @Inject
  JsonWebToken jwt;

  @Inject
  GroupService groupService;

  @Inject
  UserRepository userRepository;

  @Inject
  GroupRepository groupRepository;

  @Inject
  GroupAdminRepository groupAdminRepository;

  @Inject
  UserAdminRepository userAdminRepository;

  @Inject
  OrganizationService organizationService;

  @Inject
  DepartmentService departmentService;

  @Inject
  AccountService accountService;

  @LoggerName("inviteLogger")
  Logger inviteLogger;

  public Result validationHandler() {
    return new Result("Validation failed.", false);
  }

  public Result alreadyMemberHandler() {
    return new Result("User is already member of the organization.", false);
  }

  public Result inviteErrorHandler() {
    return new Result("Error inviting user.", false);
  }

  public Result duplicateInviteError() {
    return new Result(
        "The user has an active invitation to this organization. Please remove it before inviting this user again.",
        false);
  }

  public Result acceptInviteError() {
    return new Result(
        "Error accepting invitation, please try again.",
        false);
  }

  public Result deleteInviteError() {
    return new Result(
        "Error deleting invitation, please try again.",
        false);
  }

  public Result rejectInviteError() {
    return new Result(
        "Error rejecting invitation, please try again.",
        false);
  }

  public Response isUserInvited(Organization organization, User user, String token) {
    try {
      Boolean isInvited = false;
      Boolean isTokenValid = false;
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");
      ResponseTeamMembers members = departmentService.getDepartmentUsers(organization, department);
      Iterator<GroupMember> it = members.getData().iterator();
      while (it.hasNext()) {
        GroupMember member = it.next();
        String[] lastNameParts = member.getLastName().split(",");
        for (int i = 0; i < lastNameParts.length; i++) {
          String[] tokenParts = lastNameParts[i].split("&");
          if (tokenParts[0].equals(organization.getName()) && tokenParts[2].equals(token)
              && tokenParts[3].equals("pending")) {
            System.out.println("Enters");
            isInvited = true;
            isTokenValid = true;
            break;
          }
        }
      }
      if (isInvited && isTokenValid) {
        return new Response(true, UUID.randomUUID().toString());
      } else {
        return new Response(false, UUID.randomUUID().toString());
      }

    } catch (Exception e) {
      inviteLogger.error("Error checking if user is invited: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public Response isPresentUserInvited(Organization organization, User user, String token) {
    try {
      Boolean isInvited = false;
      Boolean isTokenValid = false;
      String email = jwt.getClaim("email");
      if (email != user.getEmail()) {
        return new Response(false, UUID.randomUUID().toString());
      }
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");
      ResponseTeamMembers members = departmentService.getDepartmentUsers(organization, department);
      Iterator<GroupMember> it = members.getData().iterator();
      while (it.hasNext()) {
        GroupMember member = it.next();
        String[] lastNameParts = member.getLastName().split(",");
        for (int i = 0; i < lastNameParts.length; i++) {
          String[] tokenParts = lastNameParts[i].split("&");
          if (tokenParts[0].equals(organization.getName()) && tokenParts[2].equals(token)
              && tokenParts[3].equals("pending")) {
            System.out.println("Enters");
            isInvited = true;
            isTokenValid = true;
            break;
          }
        }
      }
      if (isInvited && isTokenValid) {
        return new Response(true, UUID.randomUUID().toString());
      } else {
        return new Response(false, UUID.randomUUID().toString());
      }

    } catch (Exception e) {
      inviteLogger.error("Error checking if user is invited: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public String createTokenForInvitedUser() {
    try {
      RandomGenerator randomGenerator = new RandomGeneratorImpl();
      String token = randomGenerator.generateRandomString(32);
      inviteLogger.info("Token created.");
      return token;
    } catch (Exception e) {
      inviteLogger.error("Error creating token for invited user: " + e.getMessage());
    }
    return null;
  }

  public void deleteUserFromInvitedUsersDepartmentWithEmail(Organization organization, String email)
      throws ApplicationException {
    try {
      User theUser = userRepository.getUserByFirstName(email);
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");
      departmentService.deleteUserFromDepartment(theUser, organization, department);
    } catch (Exception e) {
      throw new ApplicationException("Error deleting user from invited users group.");
    }
  }

  public Response deleteUserFromInvitedUsersDepartment(Organization organization, User user)
      throws ApplicationException {
    try {
      User theUser = userRepository.getUserByFirstName(user.getEmail());
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");
      departmentService.deleteUserFromDepartment(theUser, organization, department);
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public void deleteInvitedUserRecord(String email)
      throws ApplicationException {
    try {
      User theUser = userRepository.getUserByFirstName(email);
      accountService.deleteUserFromFreeIPA(theUser);
    } catch (Exception e) {
      throw new ApplicationException("Error deleting user from invited users group.");
    }
  }

  public Response createInvitedUserRecordOnFreeIPA(String token, String email, Organization organization,
      GroupMember groupMember) {
    try {
      RandomGenerator randomGenerator = new RandomGeneratorImpl();
      String randomUsername = randomGenerator.generateRandomString(12);
      User user = new User();
      user.setUsername("invited-" + randomUsername);
      user.setFirstName(email);
      user.setLastName(token);
      user.setInvitedBy(groupMember.getUsername());
      user.setInvitedOrganization(organization.getName());
      user.setInvitedStatus("pending");

      userAdminRepository.createUser(user);
      inviteLogger.info("Invited user record created.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      inviteLogger.error("Error creating invited user record on FreeIPA: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());

    }
  }

  public Response addInvitedUserRecordToDepartment(String token, String email, Organization organization,
      GroupMember groupMember) {
    try {
      User user = userAdminRepository.getUserByFirstName(email);
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");

      departmentService.addUserToDepartment(user, organization, department);
      inviteLogger.info("Invited user record added to department.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      inviteLogger.error("Error adding invited user record to department: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());

    }
  }

  public Response addAcceptanceUserToIPAGroup(User user, Organization organization) throws ApplicationException {
    try {
      User theUser = userRepository.getUserByEmail(user.getEmail());
      groupAdminRepository.addUserToGroup(theUser, organization);
      inviteLogger.info("User " + theUser.getUsername() + " accepted the invitation and added to group "
          + organization.getName());
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      inviteLogger.error("Error happened when adding user to group " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public void editInvitedUserRecordDelete(String token, String email, Organization organization,
      GroupMember groupMember) {
    try {
      User user = userRepository.getUserByFirstName(email);

      String[] lastNameParts = user.getLastName().split(",");
      for (int i = 0; i < lastNameParts.length; i++) {
        String[] tokenParts = lastNameParts[i].split("&");

        if (!tokenParts[2].equals(token)) {
          lastNameParts[i] = tokenParts[0] + "&" + tokenParts[1] + "&" + tokenParts[2] + "&" + tokenParts[3];
          break;
        }
      }

      String newLastName = "";
      for (int i = 0; i < lastNameParts.length; i++) {
        newLastName += lastNameParts[i];
        if (i != lastNameParts.length - 1) {
          newLastName += ",";
        }
      }
      user.setLastName(newLastName);
      userAdminRepository.updateUser(user.getUsername(), user);
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");
      departmentService.addUserToDepartment(user, organization, department);
      inviteLogger.info("Invited user record updated.");
    } catch (Exception e) {
      inviteLogger.error("Error updating invited user record: " + e.getMessage());
    }
  }

  public void editInvitedUserRecord(String token, String email, Organization organization, GroupMember groupMember) {
    try {
      User user = userRepository.getUserByFirstName(email);
      user.setLastName(user.getLastName() + "," + organization.getName() + "&" + groupMember.getUsername() + "&"
          + token + "&" + "pending");

      userAdminRepository.updateUser(user.getUsername(), user);
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");
      departmentService.addUserToDepartment(user, organization, department);
      inviteLogger.info("Invited user record updated.");
    } catch (Exception e) {
      inviteLogger.error("Error updating invited user record: " + e.getMessage());
    }
  }

  public Boolean checkDuplicateInvite(String token, String email, Organization organization,
      GroupMember groupMember) throws ApplicationException {
    Boolean isDuplicate = false;
    try {
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");
      ResponseTeamMembers members = departmentService.getDepartmentUsers(organization, department);
      Iterator<GroupMember> it = members.getData().iterator();
      while (it.hasNext()) {
        GroupMember member = it.next();
        String[] lastNameParts = member.getLastName().split(",");
        for (int i = 0; i < lastNameParts.length; i++) {
          String[] tokenParts = lastNameParts[i].split("&");
          if (tokenParts[0].equals(organization.getName())
              && tokenParts[3].equals("pending")) {
            isDuplicate = true;
            break;
          }
        }
      }
      return isDuplicate;
    } catch (Exception e) {
      inviteLogger.error("Error checking duplicate invite: " + e.getMessage());
      throw new ApplicationException("Error checking duplicate invite.");
    }
  }

  public Response updateInvitedUserRecordAccepted(User user, String token, Organization organization)
      throws ApplicationException {
    try {
      String email = user.getEmail();
      User invitedUserRecord = userRepository.getUserByFirstName(email);
      String[] lastNameParts = invitedUserRecord.getLastName().split(",");
      for (int i = 0; i < lastNameParts.length; i++) {
        String[] tokenParts = lastNameParts[i].split("&");
        if (tokenParts[0].equals(organization.getName()) && tokenParts[2].equals(token)) {
          tokenParts[3] = "accepted";
          lastNameParts[i] = tokenParts[0] + "&" + tokenParts[1] + "&" + tokenParts[2] + "&" + tokenParts[3];
        }
      }
      String newLastName = "";
      for (int i = 0; i < lastNameParts.length; i++) {
        newLastName += lastNameParts[i];
        if (i != lastNameParts.length - 1) {
          newLastName += ",";
        }
      }
      invitedUserRecord.setLastName(newLastName);
      userAdminRepository.updateUser(invitedUserRecord.getUsername(), invitedUserRecord);
      inviteLogger.info("Invited user record updated.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      inviteLogger.error("Error updating invited user record: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());

    }
  }

  public Response updateInvitedUserRecordRejected(User user, String token, Organization organization)
      throws ApplicationException {
    try {
      String email = user.getEmail();
      User invitedUserRecord = userRepository.getUserByFirstName(email);
      String[] lastNameParts = invitedUserRecord.getLastName().split(",");
      for (int i = 0; i < lastNameParts.length; i++) {
        String[] tokenParts = lastNameParts[i].split("&");
        if (tokenParts[0].equals(organization.getName()) && tokenParts[2].equals(token)) {
          tokenParts[3] = "rejected";
          lastNameParts[i] = tokenParts[0] + "&" + tokenParts[1] + "&" + tokenParts[2] + "&" + tokenParts[3];
        }
      }
      String newLastName = "";
      for (int i = 0; i < lastNameParts.length; i++) {
        newLastName += lastNameParts[i];
        if (i != lastNameParts.length - 1) {
          newLastName += ",";
        }
      }
      invitedUserRecord.setLastName(newLastName);
      userAdminRepository.updateUser(invitedUserRecord.getUsername(), invitedUserRecord);
      inviteLogger.info("Invited user record updated.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      inviteLogger.error("Error updating invited user record: " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public Response updateInvitedUserRecordDeleted(User user, Organization organization)
      throws ApplicationException {
    try {
      String email = user.getEmail();
      User invitedUserRecord = userRepository.getUserByFirstName(email);
      String[] lastNameParts = invitedUserRecord.getLastName().split(",");
      for (int i = 0; i < lastNameParts.length; i++) {
        String[] tokenParts = lastNameParts[i].split("&");
        if (tokenParts[0].equals(organization.getName())) {
          lastNameParts[i] = null;
        }
      }
      String newLastName = "";
      for (int i = 0; i < lastNameParts.length; i++) {
        newLastName += lastNameParts[i];
        if (i != lastNameParts.length - 1) {
          newLastName += ",";
        }
      }
      invitedUserRecord.setLastName(newLastName);
      userAdminRepository.updateUser(invitedUserRecord.getUsername(), invitedUserRecord);
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      return new Response(false, UUID.randomUUID().toString());
    }
  }

  public String createInvitedUsersDepartment(Organization organization) throws ApplicationException {
    try {
      inviteLogger.info("Creating invited users department.");
      DepartmentBasic managersDepartment = new DepartmentBasic();
      managersDepartment.setName("invitedUsers");

      String invitedUsersGroupName = groupAdminRepository.createSubgroup(organization, managersDepartment);

      inviteLogger.info("invitedUsers group created.");
      return invitedUsersGroupName;
    } catch (Exception e) {
      inviteLogger.error("Error happened when creating IPA Group for organization " + e.getMessage());
      organizationService.deleteOrganizationGroup(organization);
      throw new ApplicationException("Error creating organization. Please try again.");

    }
  }

  public Response addInvitedUsersDepartmentAsMember(Organization organization, String invitedUsersGroupName)
      throws ApplicationException {
    try {
      DepartmentBasic dept = new DepartmentBasic();
      dept.setName(invitedUsersGroupName);
      groupRepository.addSubgroupToGroup(organization, dept);

      inviteLogger.info("Managers group added as member.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      inviteLogger.error("Error happened when creating IPA Group for organization " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());

    }
  }

  public Response addFounderToInvitedUsersDepartment(Organization organization, String invitedUsersGroupName)
      throws InternalError, IOException, ApplicationException {
    try {
      DepartmentBasic dept = new DepartmentBasic();
      dept.setName(invitedUsersGroupName);
      System.out.println("The invited users group name: " + invitedUsersGroupName);
      groupAdminRepository.setInitialManagersForDepartment(organization, dept);
      inviteLogger.info("Founder added to managers group.");
      return new Response(true, UUID.randomUUID().toString());
    } catch (Exception e) {
      inviteLogger.error("Error happened when adding founder to managers group " + e.getMessage());
      return new Response(false, UUID.randomUUID().toString());

    }

  }

}
