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
import javax.net.ssl.HttpsURLConnection;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.core.abstracts.GroupAdapter;
import org.robolaunch.core.abstracts.UserAdapter;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;
import org.robolaunch.repository.abstracts.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.math.Primes.STOutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@ApplicationScoped
public class UserRepositoryIPAImpl implements UserRepository {
    private final CookieManager cookieManager = new CookieManager();

    @Inject
    UserAdapter userAdapter;

    @Inject
    GroupAdapter groupAdapter;

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

        wr.close();
        if (!result.contains("\"error\": null")) {
            throw new InternalError("Error happend when making request for given body:" + body);
        }
        return groups;
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
    public Boolean doesEmailExist(String email) throws InternalError, IOException {
        String getRequest = userAdapter.findByEmail(email);
        System.out.println("Get request: " + getRequest);
        String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
        if (makeRequestWithMail(body).intValue() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public User getUserByEmail(String email) throws IOException {
        String getRequest = userAdapter.findByEmail(email);
        String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
        JsonNode userJson = makeRequestForUser(body);

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
    public Boolean doesFirstNameExist(String firstName) throws IOException {
        String getRequest = userAdapter.findByFirstName(firstName);
        String body = String.format("{\"id\": 0, \"method\": \"user_find/1\", \"params\": %s}", getRequest);
        JsonNode userJson = makeRequestForUser(body);
        if (userJson != null) {
            return true;
        }
        return false;
    }

}
