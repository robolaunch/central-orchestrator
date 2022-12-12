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
import org.robolaunch.repository.abstracts.GroupRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class GroupRepositoryIPAImpl implements GroupRepository {
    @Inject
    GroupAdapter groupAdapter;
    private final CookieManager cookieManager = new CookieManager();

    @ConfigProperty(name = "freeipa.url")
    String freeIpaURL;

    /* Helper function to create request. */
    private void makeRequest(String body) throws IOException, InternalError {
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
        wr.close();
        ObjectMapper mapper = new ObjectMapper();
        wr.close();
        if (!result.contains("\"error\": null")) {
            JsonNode rootNode = mapper.readTree(result);
            throw new ApplicationException(rootNode.get("error").get("message").asText());
        }
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
        if (!result.contains("\"error\": null")) {
            JsonNode rootNode = mapper.readTree(result);
            throw new ApplicationException(rootNode.get("error").get("message").asText());
        }
        return actualObj;
    }

    /* Helper function to authorize user by token */
    public void appendCookie(HttpCookie cookie) {
        cookieManager.getCookieStore().add(null, cookie);
    }

    public void clearCookies() {
        cookieManager.getCookieStore().removeAll();
    }

    public List<HttpCookie> getCurrentCookies() {
        return cookieManager.getCookieStore().getCookies();
    }

    @Override
    public void addSubgroupToGroup(Organization organization, DepartmentBasic department)
            throws InternalError, IOException {
        String groupRequest = groupAdapter.toAssignSubgroup(organization, department);
        String addRequest = String.format("{\"id\": 0, \"method\": \"group_add_member/1\", \"params\": %s}",
                groupRequest);
        makeRequest(addRequest);
    }

    @Override
    public void addUserToGroup(User user,
            Organization organization)
            throws IOException, InternalError {
        String requestData = groupAdapter.toRequestWithUser(user, organization);
        String createRequest = String.format("{\"id\": 0, \"method\": \"group_add_member/1\", \"params\": %s} ",
                requestData);
        makeRequest(createRequest);
    }

    @Override
    public void removeUserFromGroup(User user,
            Organization organization)
            throws InternalError, IOException {
        String requestData = groupAdapter.toRequestWithUser(user, organization);
        String deleteRequest = String.format("{\"id\": 0, \"method\": \"group_remove_member/1\", \"params\": %s} ",
                requestData);
        makeRequest(deleteRequest);
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
    public ArrayList<Department> getTeams(Organization group, String fieldName)
            throws InternalError, IOException {
        String requestData = groupAdapter.toGetWithAllFlag(group);
        String getAllRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
                requestData);
        JsonNode requestedField = makeRequestForGroupField(getAllRequest, fieldName);
        ArrayList<Department> departments = new ArrayList<>();
        if (requestedField != null) {
            requestedField.forEach(department -> {
                Organization organization = new Organization();
                organization.setName(department.asText());
                Department dept = new Department();

                dept.setId(department.asText());
                try {
                    dept.setName(getGroupDescription(organization));
                } catch (InternalError | IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    dept.setUsers(getGroupMembers(organization));
                } catch (InternalError | IOException e) {
                    e.printStackTrace();
                }
                departments.add(dept);
            });
        }

        return departments;
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

    @Override
    public JsonNode getGroup(Organization group) throws InternalError, IOException {
        String requestGroup = groupAdapter.toGetGroup(group);
        String getRequest = String.format("{\"id\": 0, \"method\": \"group_show/1\", \"params\": %s}",
                requestGroup);
        JsonNode organizationNode = makeRequestForGroup(getRequest);
        System.out.println("Organization: " + organizationNode);
        return organizationNode;
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
            } catch (InternalError e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        return groupMembers;
    }

    @Override
    public Boolean isGroupMember(User user, Organization group)
            throws InternalError, IOException {
        List<GroupMember> groupMembers = getGroupMembers(group);
        return groupMembers.stream().anyMatch(member -> member.getUsername().equals(user.getUsername()));
    }

    @Override
    public Boolean isGroupMemberByEmail(String email, Organization group)
            throws InternalError, IOException {
        List<GroupMember> groupMembers = getGroupMembers(group);
        return groupMembers.stream().anyMatch(member -> member.getEmail().equals(email));
    }

    @Override
    public Boolean isGroupManager(User user, Organization group)
            throws InternalError, IOException {
        Set<User> managers = getUsers(group, "membermanager_user");
        return managers.stream().anyMatch(manager -> manager.getUsername().equals(user.getUsername()));
    }

    @Override
    public Integer getRoboticsCloudCount(Organization organization) {
        return 0;
    }

}
