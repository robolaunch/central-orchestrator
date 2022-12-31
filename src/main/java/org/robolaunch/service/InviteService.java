package org.robolaunch.service;

import java.io.IOException;
import java.util.Iterator;

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
import org.robolaunch.models.User;
import org.robolaunch.models.response.PlainResponse;
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

  public PlainResponse isUserInvited(Organization organization, User user, String token) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      Boolean isInvited = false;
      Boolean isTokenValid = false;
      ResponseTeamMembers members = departmentService.getTeamUsers(organization, "invitedUsers");
      Iterator<GroupMember> it = members.getData().iterator();
      while (it.hasNext()) {
        GroupMember member = it.next();
        String[] lastNameParts = member.getLastName().split(",");
        for (int i = 0; i < lastNameParts.length; i++) {
          String[] tokenParts = lastNameParts[i].split("&");
          if (tokenParts[0].equals(organization.getName()) && tokenParts[2].equals(token)
              && tokenParts[3].equals("pending")) {
            isInvited = true;
            isTokenValid = true;
            break;
          }
        }
      }
      if (isInvited && isTokenValid) {
        plainResponse.setSuccess(true);
        plainResponse.setMessage("User is invited.");
      } else {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("User is not invited.");
      }

    } catch (Exception e) {
      inviteLogger.error("Error checking if user is invited: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error checking if user is invited.");
    }
    return plainResponse;
  }

  public PlainResponse isPresentUserInvited(Organization organization, User user, String token) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      Boolean isInvited = false;
      Boolean isTokenValid = false;
      String email = jwt.getClaim("email");
      if (email != user.getEmail()) {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("User is not invited.");
      }
      ResponseTeamMembers members = departmentService.getTeamUsers(organization, "invitedUsers");
      Iterator<GroupMember> it = members.getData().iterator();
      while (it.hasNext()) {
        GroupMember member = it.next();
        String[] lastNameParts = member.getLastName().split(",");
        for (int i = 0; i < lastNameParts.length; i++) {
          String[] tokenParts = lastNameParts[i].split("&");
          if (tokenParts[0].equals(organization.getName()) && tokenParts[2].equals(token)
              && tokenParts[3].equals("pending")) {
            isInvited = true;
            isTokenValid = true;
            break;
          }
        }
      }
      if (isInvited && isTokenValid) {
        plainResponse.setSuccess(true);
        plainResponse.setMessage("User is invited.");
      } else {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("User is not invited.");
      }

    } catch (Exception e) {
      inviteLogger.error("Error checking if user is invited: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error checking if user is invited.");
    }
    return plainResponse;
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
      departmentService.deleteUserFromTeam(theUser, organization, "invitedUsers");
    } catch (Exception e) {
      throw new ApplicationException("Error deleting user from invited users group.");
    }
  }

  public PlainResponse deleteUserFromInvitedUsersDepartment(Organization organization, User user)
      throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
    try {
      User theUser = userRepository.getUserByFirstName(user.getEmail());
      departmentService.deleteUserFromTeam(theUser, organization, "invitedUsers");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User deleted from invited users group.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error deleting user from invited users group.");
    }
    return plainResponse;
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

  public PlainResponse createInvitedUserRecordOnFreeIPA(String token, String email, Organization organization,
      GroupMember groupMember) {
    PlainResponse plainResponse = new PlainResponse();
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
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Invited user record created.");
    } catch (Exception e) {
      inviteLogger.error("Error creating invited user record on FreeIPA: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error creating invited user record on FreeIPA: " + e.getMessage());
    }
    return plainResponse;
  }

  public PlainResponse addInvitedUserRecordToDepartment(String token, String email, Organization organization,
      GroupMember groupMember) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      User user = userAdminRepository.getUserByFirstName(email);
      DepartmentBasic department = new DepartmentBasic();
      department.setName("invitedUsers");

      departmentService.addUserToTeam(user, organization, department.getName());
      inviteLogger.info("Invited user record added to department.");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Invited user record added to department.");
    } catch (Exception e) {
      inviteLogger.error("Error adding invited user record to department: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error adding invited user record to team: " + e.getMessage());
    }
    return plainResponse;
  }

  public PlainResponse addAcceptanceUserToIPAGroup(User user, Organization organization) throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
    try {
      User theUser = userRepository.getUserByEmail(user.getEmail());
      groupAdminRepository.addUserToGroup(theUser, organization);
      inviteLogger.info("User " + theUser.getUsername() + " accepted the invitation and added to group "
          + organization.getName());
      plainResponse.setSuccess(true);
      plainResponse.setMessage("User " + theUser.getUsername() + " accepted the invitation and added to group "
          + organization.getName());
    } catch (Exception e) {
      inviteLogger.error("Error happened when adding user to group " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error happened when adding user to group " + e.getMessage());
    }
    return plainResponse;
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
      departmentService.addUserToTeam(user, organization, department.getName());
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
      departmentService.addUserToTeam(user, organization, department.getName());
      inviteLogger.info("Invited user record updated.");
    } catch (Exception e) {
      inviteLogger.error("Error updating invited user record: " + e.getMessage());
    }
  }

  public Boolean checkDuplicateInvite(String token, String email, Organization organization,
      GroupMember groupMember) throws ApplicationException {
    Boolean isDuplicate = false;
    try {
      ResponseTeamMembers members = departmentService.getTeamUsers(organization, "invitedUsers");
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

  public PlainResponse updateInvitedUserRecordAccepted(User user, String token, Organization organization)
      throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
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
      plainResponse.setSuccess(true);
      plainResponse
          .setMessage("User " + invitedUserRecord.getUsername() + " accepted the invitation and added to group "
              + organization.getName());
    } catch (Exception e) {
      inviteLogger.error("Error updating invited user record: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error happened when adding user to group " + e.getMessage());
    }
    return plainResponse;
  }

  public PlainResponse updateInvitedUserRecordRejected(User user, String token, Organization organization)
      throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
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
      plainResponse.setSuccess(true);
      plainResponse
          .setMessage("User " + invitedUserRecord.getUsername() + " rejected the invitation and removed from group "
              + organization.getName());
    } catch (Exception e) {
      inviteLogger.error("Error updating invited user record: " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error happened when removing user from group " + e.getMessage());
    }
    return plainResponse;
  }

  public PlainResponse updateInvitedUserRecordDeleted(User user, Organization organization)
      throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
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
      plainResponse.setSuccess(true);
      plainResponse
          .setMessage("User " + invitedUserRecord.getUsername() + " deleted the invitation and removed from group "
              + organization.getName());
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error happened when removing user from group " + e.getMessage());
    }
    return plainResponse;
  }

  public String createInvitedUsersDepartment(Organization organization) throws ApplicationException {
    try {
      String invitedUsersGroupName = groupAdminRepository.createSubgroup(organization, "invitedUsers");

      inviteLogger.info("invitedUsers group created.");
      return invitedUsersGroupName;
    } catch (Exception e) {
      inviteLogger.error("Error happened when creating IPA Group for organization " + e.getMessage());
      organizationService.deleteOrganizationGroup(organization);
      throw new ApplicationException("Error creating organization. Please try again.");

    }
  }

  public PlainResponse addInvitedUsersDepartmentAsMember(Organization organization, String teamName)
      throws ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
    try {
      groupRepository.addSubgroupToGroup(organization, teamName);

      inviteLogger.info("Managers group added as member.");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Managers group added as member.");
    } catch (Exception e) {
      inviteLogger.error("Error happened when creating IPA Group for organization " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error happened when creating IPA Group for organization " + e.getMessage());
    }
    return plainResponse;
  }

  public PlainResponse addFounderToInvitedUsersTeam(Organization organization, String invitedUsersGroupName)
      throws InternalError, IOException, ApplicationException {
    PlainResponse plainResponse = new PlainResponse();
    try {
      DepartmentBasic dept = new DepartmentBasic();
      dept.setName(invitedUsersGroupName);
      groupAdminRepository.setInitialManagersForDepartment(organization, dept);
      inviteLogger.info("Founder added to managers group.");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Founder added to managers group.");
    } catch (Exception e) {
      inviteLogger.error("Error happened when adding founder to managers group " + e.getMessage());
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error happened when adding founder to managers group " + e.getMessage());
    }
    return plainResponse;
  }

}
