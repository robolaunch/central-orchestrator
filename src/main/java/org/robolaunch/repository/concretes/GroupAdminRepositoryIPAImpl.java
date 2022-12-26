package org.robolaunch.repository.concretes;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.core.abstracts.GroupAdapter;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Department;
import org.robolaunch.models.DepartmentBasic;
import org.robolaunch.models.GroupMember;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.GroupRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class GroupAdminRepositoryIPAImpl implements GroupAdminRepository {
  @Inject
  GroupAdapter groupAdapter;
  private final CookieManager cookieManager = new CookieManager();

  @Inject
  GroupRepository groupRepository;

  @ConfigProperty(name = "freeipa.url")
  String freeIpaURL;

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

  /* Helper function to authorize user by token */
  public void appendCookie(HttpCookie cookie) {
    cookieManager.getCookieStore().add(null, cookie);
  }

  public List<HttpCookie> getCurrentCookies() {
    return cookieManager.getCookieStore().getCookies();
  }

  public void clearCookies() throws URISyntaxException {
    cookieManager.getCookieStore().removeAll();
  }

  @Override
  public void addUserToGroup(User user,
      Organization organization)
      throws IOException, InternalError, ApplicationException {
    String requestData = groupAdapter.toRequestWithUser(user, organization);
    String createRequest = String.format("{\"id\": 0, \"method\": \"group_add_member/1\", \"params\": %s} ",
        requestData);
    makeRequest(createRequest);
  }

  @Override
  public JsonNode getGroupField(Organization group, String fieldName)
      throws InternalError, IOException {
    String requestData = groupAdapter.toGetWithAllFlag(group);
    String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
        requestData);
    JsonNode requestedField = makeRequestForGroupField(getAllRequest, fieldName);

    return requestedField;
  }

  @Override
  public void addUserToGroupAsManager(User user, Organization group)
      throws InternalError, IOException, ApplicationException {
    String groupRequest = groupAdapter.toGroupRequest(user, group);
    String addRequest = String.format("{\"id\": 0, \"method\": \"group_add_member_manager/1\", \"params\": %s}",
        groupRequest);
    makeRequest(addRequest);
  }

  @Override
  public void createGroup(Organization organization) throws IOException, InternalError, ApplicationException {
    String requestData = groupAdapter.toCreateGroup(organization);
    String createRequest = String.format("{\"id\": 0, \"method\": \"group_add/1\", \"params\": %s} ", requestData);
    makeRequest(createRequest);
  }

  @Override
  public String createSubgroup(Organization organization, String teamName)
      throws InternalError, IOException, ApplicationException {
    DepartmentBasic depBasic = new DepartmentBasic();
    depBasic.setName(teamName);
    ArrayList<String> array = groupAdapter.toCreateSubgroup(organization, depBasic);
    String requestData = array.get(0);
    String createRequest = String.format("{\"id\": 0, \"method\": \"group_add/1\", \"params\": %s} ", requestData);
    makeRequest(createRequest);
    return array.get(1);
  }

  @Override
  public void deleteGroup(Organization group) throws InternalError, IOException, ApplicationException {
    String requestDelete = groupAdapter.toDeleteGroup(group);
    String deleteRequest = String.format("{\"id\": 0, \"method\": \"group_del/1\", \"params\": %s} ",
        requestDelete);
    makeRequest(deleteRequest);
  }

  @Override
  public void removeUserManagerFromGroup(User user,
      Organization organization)
      throws InternalError, IOException, ApplicationException {
    String requestData = groupAdapter.toRequestWithUser(user, organization);
    String deleteRequest = String.format("{\"id\": 0, \"method\": \"group_remove_member_manager/1\", \"params\": %s} ",
        requestData);
    makeRequest(deleteRequest);
  }

  @Override
  public Boolean doesGroupExist(Organization group) throws InternalError, IOException {
    String requestGroup = groupAdapter.toGetGroup(group);

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
    Organization org = new Organization();
    org.setName(oldTeamName);
    String requestChange = groupAdapter.toChangeGroup(org, newTeamName);
    String changeRequest = String.format("{\"id\": 0, \"method\": \"group_mod/1\", \"params\": %s} ",
        requestChange);
    makeRequest(changeRequest);
  }

  @Override
  public String getGroupNameFromDescription(Organization organization, String description)
      throws InternalError, IOException {
    List<Department> departments = groupRepository.getTeams(organization, "member_group");
    for (Department department : departments) {
      if (department.getName().equals(description)) {
        return department.getId();
      }
    }
    return null;
  }

  @Override
  public String getGroupDescription(String groupName) throws InternalError, IOException {
    Organization group = new Organization();
    group.setName(groupName);
    String requestData = groupAdapter.toGetWithAllFlag(group);
    String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
        requestData);
    JsonNode requestedField = makeRequestForGroupField(getAllRequest, "description");
    if (requestedField != null) {
      return requestedField.get(0).asText();
    }
    return null;
  }

  @Override
  public void setInitialManagersForDepartment(Organization organization, DepartmentBasic department)
      throws InternalError, IOException {
    Organization departmentIPAFormat = new Organization();

    departmentIPAFormat.setName(department.getName());
    Set<User> parentMemberManagers = groupRepository.getUsers(organization, "membermanager_user");
    Set<User> subgroupMemberManagers = groupRepository.getUsers(departmentIPAFormat, "membermanager_user");

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
    List<GroupMember> groupMembers = getGroupMembers(group);
    return groupMembers.stream().anyMatch(member -> member.getUsername().equals(user.getUsername()));
  }

  @Override
  public Set<User> getUsers(Organization organization, String fieldName)
      throws InternalError, IOException {
    String requestData = groupAdapter.toGetWithAllFlag(organization);

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
  public ArrayList<GroupMember> getGroupMembers(Organization group)
      throws InternalError, IOException {
    String requestData = groupAdapter.toGetUsersWithParams(group);
    String getAllRequest = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}",
        requestData);
    JsonNode requestedField = makeRequestForGroupMembers(getAllRequest);
    ArrayList<GroupMember> groupMembers = new ArrayList<>();

    requestedField.forEach(user -> {
      try {
        User usr = new User();
        usr.setUsername(user.get("uid").get(0).asText());
        if (!usr.getUsername().equals("bigboss")) {
          GroupMember groupMember = new GroupMember();
          groupMember.setUsername(user.get("uid").get(0).asText());
          groupMember.setFirstName(user.get("givenname").get(0).asText());
          groupMember.setLastName(user.get("sn").get(0).asText());
          groupMember.setEmail(user.get("mail").get(0).asText());
          groupMember.setAdmin(isGroupManager(usr, group));
          if (user.get("uid").get(0).asText().startsWith("invited")) {
            if (getGroupDescription(group).equals("invitedUsers")) {
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
  public String getGroupDescription(Organization group) throws InternalError, IOException {
    String requestData = groupAdapter.toGetWithAllFlag(group);
    String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
        requestData);
    JsonNode requestedField = makeRequestForGroupField(getAllRequest, "description");
    if (requestedField != null) {
      return requestedField.get(0).asText();
    }
    return null;
  }

}