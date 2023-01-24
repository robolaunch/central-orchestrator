package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.model.account.UserGroupMember;
import org.robolaunch.model.account.Organization;
import org.robolaunch.model.account.Team;
import org.robolaunch.model.account.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface AccountRepository {

      // Make request
      public void makeRequest(String body) throws IOException, InternalError, ApplicationException;

      public JsonNode makeRequestForGroups(String body)
                  throws IOException, InternalError;

      public JsonNode makeRequestForUser(String email) throws MalformedURLException, ProtocolException, IOException;

      public String makeRequestWithResponse(String body, String objectName)
                  throws IOException, InternalError, ApplicationException;

      public Number makeRequestWithMail(String body) throws IOException, InternalError;

      // Cookie operations
      public void appendCookie(HttpCookie cookie);

      public List<HttpCookie> getCurrentCookies();

      public void clearCookies();

      // Group operations
      public void addSubgroupToGroup(Organization organization, String teamName)
                  throws InternalError, IOException;

      public void addUserToGroup(User user, Organization organization)
                  throws IOException, InternalError;

      public void addUserToGroupAsManager(User user, Organization group)
                  throws InternalError,
                  IOException, ApplicationException;

      public void removeUserFromGroup(User user, Organization organization)
                  throws InternalError, IOException;

      public ArrayList<Team> getTeams(Organization group, String fieldName)
                  throws InternalError, IOException;

      public JsonNode getGroup(Organization group) throws InternalError,
                  IOException;

      public JsonNode getGroupField(Organization group, String fieldName)
                  throws InternalError, IOException;

      public ArrayList<UserGroupMember> getGroupMembers(Organization group)
                  throws InternalError, IOException;

      public Boolean isGroupMember(User user, Organization group)
                  throws InternalError, IOException;

      public Boolean isGroupMemberByEmail(String email, Organization group)
                  throws InternalError, IOException;

      public Boolean isGroupManager(User user, Organization group)
                  throws InternalError, IOException;

      public String getGroupDescription(Organization group)
                  throws InternalError, IOException;

      public String getGroupDescription(String groupName) throws InternalError, IOException;

      public void createGroup(Organization organization)
                  throws InternalError, IOException, ApplicationException;

      public String createSubgroup(Organization organization, String teamName)
                  throws InternalError, IOException, ApplicationException;

      public void deleteGroup(Organization group)
                  throws InternalError, IOException, ApplicationException;

      public Boolean doesGroupExist(Organization group) throws InternalError, IOException;

      public void removeUserManagerFromGroup(User user, Organization organization)
                  throws InternalError, IOException, ApplicationException;

      public void changeTeamName(Organization organization, String oldTeamName, String newTeamName)
                  throws InternalError, IOException, ApplicationException;

      public String getGroupNameFromDescription(Organization organization, String description)
                  throws InternalError, IOException;

      public void setInitialManagersForTeam(Organization organization, Team team)
                  throws InternalError, IOException, ApplicationException;

      // User operations
      public Set<User> getUsers(Organization group, String fieldName)
                  throws InternalError, IOException;

      public void changePassword(String oldPassword, String newPassword, String username)
                  throws MalformedURLException, ProtocolException, IOException;

      public void updateUser(String currentUsername, User user) throws IOException, InternalError, ApplicationException;

      public ArrayList<Organization> getOrganizations(User user) throws IOException;

      public Boolean doesEmailExist(String email) throws InternalError, IOException;

      public User getUserByEmail(String email) throws IOException;

      public User getUserByFirstName(String firstName) throws IOException;

      public Boolean doesFirstNameExist(String firstName) throws IOException;

      public void createUser(User user) throws IOException, InternalError, ApplicationException;

      public String createUserWithPassword(User user) throws IOException, InternalError, ApplicationException;

      public void deleteUser(User user) throws IOException, InternalError, ApplicationException;

      public String getUserMail(String username) throws IOException, InternalError, ApplicationException;

      public void updateUserPassword(User user, String password)
                  throws IOException, InternalError, ApplicationException;

      public User getUserByUsername(String username) throws JsonProcessingException, IOException;

}
