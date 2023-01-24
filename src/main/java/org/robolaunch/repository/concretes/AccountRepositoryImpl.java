package org.robolaunch.repository.concretes;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.core.abstracts.GroupAdapter;
import org.robolaunch.core.abstracts.RandomGenerator;
import org.robolaunch.core.abstracts.UserAdapter;
import org.robolaunch.core.concretes.RandomGeneratorImpl;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.model.account.UserGroupMember;
import org.robolaunch.model.account.Organization;
import org.robolaunch.model.account.Team;
import org.robolaunch.model.account.User;
import org.robolaunch.repository.abstracts.AccountRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class AccountRepositoryImpl implements AccountRepository {
   private final CookieManager cookieManager = new CookieManager();

   @Inject
   private GroupAdapter groupAdapter;
   @Inject
   private UserAdapter userAdapter;

   @ConfigProperty(name = "freeipa.url")
   String freeIpaURL;

   /* Helper function to authorize user by token */
   @Override
   public void appendCookie(HttpCookie cookie) {
      cookieManager.getCookieStore().add(null, cookie);
   }

   @Override
   public List<HttpCookie> getCurrentCookies() {
      return cookieManager.getCookieStore().getCookies();
   }

   @Override
   public void clearCookies() {
      cookieManager.getCookieStore().removeAll();
   }

   @Override
   public void makeRequest(String body) throws IOException, InternalError, ApplicationException {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));

      connection.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();

      wr.close();
      if (!result.contains("\"error\": null")) {
         JsonNode rootNode = mapper.readTree(result);
         throw new ApplicationException(rootNode.get("error").get("message").asText());
      }
   }

   private JsonNode makeRequestForGroup(String body)
         throws IOException, InternalError {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
      connection.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(result);
      return actualObj;
   }

   /* Helper function to get member managers request. */
   private JsonNode makeRequestForGroupMembers(String body)
         throws IOException, InternalError {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
      connection.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(result);
      JsonNode requestedData = actualObj.get("result").get("result");
      wr.close();
      if (!result.contains("\"error\": null")) {
         JsonNode rootNode = mapper.readTree(result);
         throw new ApplicationException(rootNode.get("error").get("message").asText());
      }
      return requestedData;
   }

   /* Helper function to get member managers request. */
   private JsonNode makeRequestForGroupField(String body, String fieldName)
         throws IOException, InternalError {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
      connection.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(result);
      JsonNode requestedData = actualObj.get("result").get("result").get(fieldName);
      wr.close();
      if (!result.contains("\"error\": null")) {
         JsonNode rootNode = mapper.readTree(result);
         throw new ApplicationException(rootNode.get("error").get("message").asText());
      }
      return requestedData;
   }

   @Override
   public Number makeRequestWithMail(String body) throws IOException, InternalError {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
      connection.setDoOutput(true);

      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";

      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(result);
      wr.close();
      if (!result.contains("\"error\": null")) {
         throw new InternalError("Error happened when making request for given body:" + body);
      }
      return actualObj.get("result").get("result").size();

   }

   /* Getting a specific field of user (String ones) */
   @Override
   public String makeRequestWithResponse(String body, String objectName)
         throws IOException, InternalError, ApplicationException {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
      connection.setDoOutput(true);

      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(result);
      String userMail = actualObj.get("result").get("result").get(0).get(objectName).get(0).textValue();
      wr.close();
      if (!result.contains("\"error\": null")) {
         JsonNode rootNode = mapper.readTree(result);
         throw new ApplicationException(rootNode.get("error").get("message").asText());
      }

      return userMail;

   }

   @Override
   public JsonNode makeRequestForUser(String body) throws IOException {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
      connection.setDoOutput(true);

      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(result);

      wr.close();
      if (!result.contains("\"error\": null")) {
         throw new InternalError("Error happend when making request for given body:" + body);
      }
      return actualObj.get("result").get("result").get(0);
   }

   /* Getting user groups */
   @Override
   public JsonNode makeRequestForGroups(String body)
         throws IOException, InternalError {
      URL url = new URL(this.freeIpaURL + "/ipa/session/json");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Referer", this.freeIpaURL + "/ipa");
      connection.setRequestProperty("Cookie", StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
      connection.setDoOutput(true);

      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.write(body.getBytes("UTF-8"), 0, body.getBytes("UTF-8").length);

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualObj = mapper.readTree(result);
      JsonNode groups = actualObj.get("result").get("result").get(0).get("memberof_group");

      if (!result.contains("\"error\": null")) {
         JsonNode rootNode = mapper.readTree(result);
         throw new ApplicationException(rootNode.get("error").get("message").asText());
      }
      return groups;
   }

   @Override
   public void removeUserFromGroup(User user,
         Organization organization)
         throws InternalError, IOException {
      String requestData = groupAdapter.toRequestWithUser(user, organization.getName());
      String deleteRequest = String.format("{\"id\": 0, \"method\": \"group_remove_member/1\", \"params\": %s} ",
            requestData);
      makeRequest(deleteRequest);
   }

   @Override
   public void addUserToGroup(User user,
         Organization organization)
         throws IOException, InternalError, ApplicationException {
      String requestData = groupAdapter.toRequestWithUser(user, organization.getName());
      String createRequest = String.format("{\"id\": 0, \"method\": \"group_add_member/1\", \"params\": %s} ",
            requestData);
      makeRequest(createRequest);
   }

   @Override
   public JsonNode getGroupField(Organization organization, String fieldName)
         throws InternalError, IOException {
      String requestData = groupAdapter.toGetWithAllFlag(organization.getName());
      String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
            requestData);
      JsonNode requestedField = makeRequestForGroupField(getAllRequest, fieldName);

      return requestedField;
   }

   @Override
   public void addUserToGroupAsManager(User user, Organization organization)
         throws InternalError, IOException, ApplicationException {
      String groupRequest = groupAdapter.toGroupRequest(user, organization.getName());
      String addRequest = String.format("{\"id\": 0, \"method\": \"group_add_member_manager/1\", \"params\": %s}",
            groupRequest);
      makeRequest(addRequest);
   }

   @Override
   public void createGroup(Organization organization) throws IOException, InternalError, ApplicationException {
      String requestData = groupAdapter.toCreateGroup(organization.getName());
      String createRequest = String.format("{\"id\": 0, \"method\": \"group_add/1\", \"params\": %s} ", requestData);
      makeRequest(createRequest);
   }

   @Override
   public String createSubgroup(Organization organization, String teamName)
         throws InternalError, IOException, ApplicationException {
      ArrayList<String> array = groupAdapter.toCreateSubgroup(organization.getName(), teamName);
      String requestData = array.get(0);
      String createRequest = String.format("{\"id\": 0, \"method\": \"group_add/1\", \"params\": %s} ", requestData);
      makeRequest(createRequest);
      return array.get(1);
   }

   @Override
   public void deleteGroup(Organization organization) throws InternalError, IOException, ApplicationException {
      String requestDelete = groupAdapter.toDeleteGroup(organization.getName());
      String deleteRequest = String.format("{\"id\": 0, \"method\": \"group_del/1\", \"params\": %s} ",
            requestDelete);
      makeRequest(deleteRequest);
   }

   @Override
   public void removeUserManagerFromGroup(User user,
         Organization organization)
         throws InternalError, IOException, ApplicationException {
      String requestData = groupAdapter.toRequestWithUser(user, organization.getName());
      String deleteRequest = String.format(
            "{\"id\": 0, \"method\": \"group_remove_member_manager/1\", \"params\": %s} ",
            requestData);
      makeRequest(deleteRequest);
   }

   @Override
   public Boolean doesGroupExist(Organization organization) throws InternalError, IOException {
      String requestGroup = groupAdapter.toGetGroup(organization.getName());

      String getRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
            requestGroup);
      JsonNode organizationNode = makeRequestForGroup(getRequest);
      if (!organizationNode.get("error").asText().equals("null")) {
         if (organizationNode.get("error").get("name").asText().equals("NotFound")) {
            return false;
         }
         return null;
      } else {
         return true;
      }
   }

   @Override
   public void changeTeamName(Organization organization, String oldTeamName, String newTeamName)
         throws InternalError, IOException, ApplicationException {
      String requestChange = groupAdapter.toChangeGroup(oldTeamName, newTeamName);
      String changeRequest = String.format("{\"id\": 0, \"method\": \"group_mod/1\", \"params\": %s} ",
            requestChange);
      makeRequest(changeRequest);
   }

   @Override
   public ArrayList<Team> getTeams(Organization organization, String fieldName)
         throws InternalError, IOException {
      String requestData = groupAdapter.toGetWithAllFlag(organization.getName());
      String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
            requestData);
      JsonNode requestedField = makeRequestForGroupField(getAllRequest, fieldName);
      ArrayList<Team> departments = new ArrayList<>();
      if (requestedField != null) {
         requestedField.forEach(department -> {
            Organization organizationInt = new Organization();
            organizationInt.setName(department.asText());
            Team dept = new Team();

            dept.setId(department.asText());
            try {
               dept.setName(getGroupDescription(organizationInt));
            } catch (InternalError | IOException e1) {
               e1.printStackTrace();
            }

            try {
               dept.setUsers(getGroupMembers(organizationInt));
            } catch (InternalError | IOException e) {
               e.printStackTrace();
            }
            departments.add(dept);
         });
      }

      return departments;
   }

   @Override
   public String getGroupNameFromDescription(Organization organization, String description)
         throws InternalError, IOException {
      List<Team> departments = getTeams(organization, "member_group");
      for (Team department : departments) {
         if (department.getName().equals(description)) {
            return department.getId();
         }
      }
      return null;
   }

   @Override
   public String getGroupDescription(String groupName) throws InternalError, IOException {
      String requestData = groupAdapter.toGetWithAllFlag(groupName);
      String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
            requestData);
      JsonNode requestedField = makeRequestForGroupField(getAllRequest, "description");
      if (requestedField != null) {
         return requestedField.get(0).asText();
      }
      return null;
   }

   @Override
   public Boolean isGroupMemberByEmail(String email, Organization group)
         throws InternalError, IOException {
      List<UserGroupMember> groupMembers = getGroupMembers(group);
      return groupMembers.stream().anyMatch(member -> member.getEmail().equals(email));
   }

   @Override
   public void setInitialManagersForTeam(Organization organization, Team team)
         throws InternalError, IOException {
      Organization departmentIPAFormat = new Organization();

      departmentIPAFormat.setName(team.getName());
      Set<User> parentMemberManagers = getUsers(organization, "membermanager_user");
      Set<User> subgroupMemberManagers = getUsers(departmentIPAFormat, "membermanager_user");

      Iterator<User> upperUsers = parentMemberManagers.iterator();

      if (subgroupMemberManagers == null || subgroupMemberManagers.size() == 0) {
         while (upperUsers.hasNext()) {
            User parentManager = new User();
            parentManager.setUsername(upperUsers.next().getUsername());
            addUserToGroupAsManager(parentManager, departmentIPAFormat);
            addUserToGroup(parentManager, departmentIPAFormat);
         }
      } else {
         while (upperUsers.hasNext()) {
            User parentManager = new User();
            parentManager.setUsername(upperUsers.next().getUsername());
            if (!subgroupMemberManagers.contains(parentManager)) {
               addUserToGroupAsManager(parentManager, departmentIPAFormat);
               addUserToGroup(parentManager, departmentIPAFormat);
            }
         }
      }
   }

   @Override
   public Boolean isGroupManager(User user, Organization group)
         throws InternalError, IOException {
      Set<User> managers = getUsers(group, "membermanager_user");
      return managers.stream().anyMatch(manager -> manager.getUsername().equals(user.getUsername()));
   }

   @Override
   public Boolean isGroupMember(User user, Organization group)
         throws InternalError, IOException {
      List<UserGroupMember> groupMembers = getGroupMembers(group);
      return groupMembers.stream().anyMatch(member -> member.getUsername().equals(user.getUsername()));
   }

   @Override
   public Set<User> getUsers(Organization organization, String fieldName)
         throws InternalError, IOException {
      String requestData = groupAdapter.toGetWithAllFlag(organization.getName());

      String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
            requestData);
      JsonNode requestedField = makeRequestForGroupField(getAllRequest, fieldName);

      Set<User> users = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));
      if (requestedField != null) {
         requestedField.forEach(user -> {
            User newUser = new User();
            newUser.setUsername(user.asText());
            users.add(newUser);
         });
      }
      return users;
   }

   @Override
   public ArrayList<UserGroupMember> getGroupMembers(Organization organization)
         throws InternalError, IOException {
      String requestData = groupAdapter.toGetUsersWithParams(organization.getName());
      String getAllRequest = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}",
            requestData);
      JsonNode requestedField = makeRequestForGroupMembers(getAllRequest);
      ArrayList<UserGroupMember> groupMembers = new ArrayList<>();

      requestedField.forEach(user -> {
         try {
            User usr = new User();
            usr.setUsername(user.get("uid").get(0).asText());
            if (!usr.getUsername().equals("bigboss")) {
               UserGroupMember groupMember = new UserGroupMember();
               groupMember.setUsername(user.get("uid").get(0).asText());
               groupMember.setFirstName(user.get("givenname").get(0).asText());
               groupMember.setLastName(user.get("sn").get(0).asText());
               groupMember.setEmail(user.get("mail").get(0).asText());
               groupMember.setAdmin(isGroupManager(usr, organization));
               if (user.get("uid").get(0).asText().startsWith("invited")) {
                  if (getGroupDescription(organization.getName()).equals("invitedUsers")) {
                     groupMembers.add(groupMember);
                  }
               } else {
                  groupMembers.add(groupMember);
               }
            }
         } catch (InternalError e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         }

      });
      return groupMembers;
   }

   @Override
   public String getGroupDescription(Organization organization) throws InternalError, IOException {
      String requestData = groupAdapter.toGetWithAllFlag(organization.getName());
      String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
            requestData);
      JsonNode requestedField = makeRequestForGroupField(getAllRequest, "description");
      if (requestedField != null) {
         return requestedField.get(0).asText();
      }
      return null;
   }

   @Override
   public void addSubgroupToGroup(Organization organization, String teamName)
         throws InternalError, IOException {
      String groupRequest = groupAdapter.toAssignSubgroup(organization.getName(), teamName);
      String addRequest = String.format("{\"id\": 0, \"method\": \"group_add_member/1\", \"params\": %s}",
            groupRequest);
      makeRequest(addRequest);
   }

   @Override
   public void createUser(User user) throws IOException, InternalError, ApplicationException {
      String userRequest = userAdapter.toCreateRequest(user);
      String createRequest = String.format("{\"id\": 0, \"method\": \"user_add/1\", \"params\": %s} ", userRequest);
      makeRequest(createRequest);
      // connection.setRequestMethod(httpMeth);
   }

   @Override
   public String createUserWithPassword(User user) throws IOException, InternalError, ApplicationException {
      RandomGenerator randomGenerator = new RandomGeneratorImpl();
      String tempPassword = randomGenerator.generateRandomString(8);
      String userRequest = userAdapter.toCreateRequestWithPassword(user, tempPassword);
      String createRequest = String.format("{\"id\": 0, \"method\": \"user_add/1\", \"params\": %s} ", userRequest);
      makeRequest(createRequest);
      // connection.setRequestMethod(httpMeth);
      return tempPassword;
   }

   @Override
   public User getUserByEmail(String email) throws IOException {
      String getRequest = userAdapter.findByEmail(email);
      String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
      JsonNode userJson = makeRequestForUser(body);
      if (userJson.get("uid") == null) {
         return null;
      }
      User user = new User();
      user.setUsername(userJson.get("uid").get(0).asText());
      user.setFirstName(userJson.get("givenname").get(0).asText());
      user.setLastName(userJson.get("sn").get(0).asText());
      return user;
   }

   @Override
   public void updateUser(String currentUsername, User user) throws InternalError, IOException, ApplicationException {
      String userRequest = userAdapter.toUserUpdate(currentUsername, user);
      String updateRequest = String.format("{\"id\": 0, \"method\": \"user_mod/1\", \"params\": %s} ", userRequest);
      makeRequest(updateRequest);
   }

   @Override
   public void deleteUser(User user) throws InternalError, IOException, ApplicationException {
      String userRequest = userAdapter.toUsernameRequest(user);
      String deleteRequest = String.format("{\"id\": 0, \"method\": \"user_del/1\", \"params\": %s} ", userRequest);
      makeRequest(deleteRequest);
   }

   @Override
   public String getUserMail(String username) throws InternalError, IOException, ApplicationException {
      String userRequest = userAdapter.toModel(username);
      String getUserRequest = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s} ", userRequest);
      return makeRequestWithResponse(getUserRequest, "mail");
   }

   @Override
   public void updateUserPassword(User user, String password) throws IOException, InternalError, ApplicationException {
      String userRequest = userAdapter.toPasswordRequest(user, password);
      String createRequest = String.format("{\"id\": 0, \"method\": \"passwd/1\", \"params\": %s} ", userRequest);
      makeRequest(createRequest);
   }

   @Override
   public User getUserByFirstName(String firstName) throws IOException {
      String getRequest = userAdapter.findByFirstName(firstName);
      String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
      JsonNode userJson = makeRequestForUser(body);
      User user = new User();
      user.setUsername(userJson.get("uid").get(0).asText());
      user.setFirstName(userJson.get("givenname").get(0).asText());
      user.setLastName(userJson.get("sn").get(0).asText());

      return user;
   }

   @Override
   public User getUserByUsername(String username) throws IOException {
      String getRequest = userAdapter.findByUsername(username);

      String body = String.format("{\"id\": 0, \"method\": \"user_show/1\", \"params\": %s}", getRequest);
      JsonNode userJson = makeRequestForUser(body);
      User user = new User();
      user.setUsername(userJson.get("uid").get(0).asText());
      user.setFirstName(userJson.get("givenname").get(0).asText());
      user.setLastName(userJson.get("sn").get(0).asText());
      user.setEmail(userJson.get("mail").get(0).asText());

      return user;
   }

   @Override
   public ArrayList<Organization> getOrganizations(User user) throws IOException {
      String getRequest = userAdapter.toGetUserWithAll(user);
      String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
      JsonNode requestedField = makeRequestForGroups(body);
      ArrayList<Organization> organizations = new ArrayList<>();

      requestedField.forEach(organization -> {
         if (organization.asText().equals("ipausers")
               || organization.asText().equals("fm_admins")
               || organization.asText().equals("fm_users")
               || organization.asText().contains("-dep-")) {
         } else {
            Organization org = new Organization();
            org.setName(organization.asText());
            if (organization.asText().startsWith("org-")) {
               org.setEnterprise(false);
            } else {
               org.setEnterprise(true);
            }
            organizations.add(org);
         }

      });
      return organizations;
   }

   @Override
   public void changePassword(String oldPassword, String newPassword, String username)
         throws IOException {
      URL url = new URL(this.freeIpaURL + "/ipa/session/change_password");
      HttpsURLConnection userConnection = (HttpsURLConnection) url.openConnection();
      userConnection.setRequestMethod("POST");
      userConnection.setRequestProperty("Content-Type",
            "application/x-www-form-urlencoded");
      userConnection.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(
            userConnection.getOutputStream());
      String credentials = "user=" + username + "&" + "old_password=" + oldPassword + "&new_password="
            + newPassword;
      wr.writeBytes(credentials);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(userConnection.getInputStream()));
      String line;
      String result = "";

      while ((line = bufferedReader.readLine()) != null) {
         result += line;
      }

      ObjectMapper mapper = new ObjectMapper();
      wr.close();
      if (!result.contains("\"error\": null")) {
         JsonNode rootNode = mapper.readTree(result);
         throw new ApplicationException(rootNode.get("error").get("message").asText());
      }
      /*
       * // To catch additonal error which is mostly related about health of IPA
       * Service.
       * if (userConnection.getResponseCode() != 200) {
       * throw new ApplicationException("User change password operation rejected.");
       * }
       */
   }

   @Override
   public Boolean doesFirstNameExist(String firstName) throws IOException {
      String getRequest = userAdapter.findByFirstName(firstName);
      String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
      JsonNode userJson = makeRequestForUser(body);
      if (userJson != null) {
         return true;
      }
      return false;
   }

   @Override
   public Boolean doesEmailExist(String email) throws InternalError, IOException {
      String getRequest = userAdapter.findByEmail(email);
      String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
      if (makeRequestWithMail(body).intValue() > 0) {
         return true;
      }
      return false;
   }

   @Override
   public JsonNode getGroup(Organization organization) throws InternalError, IOException {
      String requestGroup = groupAdapter.toGetGroup(organization.getName());
      String getRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
            requestGroup);
      JsonNode organizationNode = makeRequestForGroup(getRequest);
      return organizationNode;
   }

}
