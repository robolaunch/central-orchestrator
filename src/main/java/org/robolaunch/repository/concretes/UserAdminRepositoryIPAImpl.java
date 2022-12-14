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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.core.abstracts.RandomGenerator;
import org.robolaunch.core.abstracts.UserAdapter;
import org.robolaunch.core.concretes.RandomGeneratorImpl;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.InvitedUser;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;
import org.robolaunch.repository.abstracts.UserAdminRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class UserAdminRepositoryIPAImpl implements UserAdminRepository {
  private final CookieManager cookieManager = new CookieManager();

  @Inject
  UserAdapter userAdapter;

  @ConfigProperty(name = "freeipa.url")
  String freeIpaURL;

  @Override
  public void appendCookie(HttpCookie cookie) {
    cookieManager.getCookieStore().add(null, cookie);
  }

  @Override
  public void clearCookies() {
    cookieManager.getCookieStore().removeAll();
  }

  /**
   * Create user on FreeIPA with Keycloak user properties
   * 
   * @throws ApplicationException
   */
  @Override
  public void makeRequest(String body) throws IOException, InternalError, ApplicationException {
    URL url = new URL(this.freeIpaURL + "/ipa/session/json");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    // connection.setRequestProperty("Content-Type",
    // "application/x-www-form-urlencoded; charset=utf-8");
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

  @Override
  public Number makeRequestWithMail(String body) throws IOException, InternalError {
    System.out.println("Definitely enters...");
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
    System.out.println("Saf result: " + result);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualObj = mapper.readTree(result);
    wr.close();
    if (!result.contains("\"error\": null")) {
      throw new InternalError("Error happened when making request for given body:" + body);
    }
    System.out.println(actualObj.get("result").get("result"));
    return actualObj.get("result").get("result").size();

  }

  @Override
  public Boolean doesEmailExist(String email) throws InternalError, IOException {
    String getRequest = userAdapter.findByEmail(email);
    System.out.println("Get request: " + getRequest);
    String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
    if (makeRequestWithMail(body).intValue() > 0) {
      return true;
    }
    return false;
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
      JsonNode rootNode = mapper.readTree(result);
      throw new ApplicationException("Error make request: " + rootNode.get("error").get("message").asText());
    }
    return actualObj.get("result").get("result");
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
    System.out.println("User json: " + userJson);
    if (userJson.get("uid") == null) {
      System.out.println("Enters");
      return null;
    }
    User user = new User();
    user.setUsername(userJson.get("uid").get(0).asText());
    user.setFirstName(userJson.get("givenname").get(0).asText());
    user.setLastName(userJson.get("sn").get(0).asText());
    return user;
  }

  @Override
  public String createUserFromInviteWithPassword(InvitedUser user)
      throws InternalError, IOException, ApplicationException {
    User usr = new User();
    usr.setUsername(user.getUsername());
    usr.setEmail(user.getEmail());
    usr.setFirstName(user.getFirstName());
    usr.setLastName(user.getLastName());

    RandomGenerator randomGenerator = new RandomGeneratorImpl();
    String tempPassword = randomGenerator.generateRandomString(8);
    String userRequest = userAdapter.toCreateRequestWithPassword(usr, tempPassword);
    String createRequest = String.format("{\"id\": 0, \"method\": \"user_add/1\", \"params\": %s} ", userRequest);
    makeRequest(createRequest);
    // connection.setRequestMethod(httpMeth);
    return tempPassword;
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

}
