package org.robolaunch.core.concretes;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.microprofile.config.ConfigProvider;

import org.robolaunch.core.abstracts.IPALogin;

public class IPAUserLogin implements IPALogin {
    String freeIpaURL;
    private String username;
    private String password;

    public IPAUserLogin() {
        this.freeIpaURL = ConfigProvider.getConfig().getValue("freeipa.url", String.class);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public List<HttpCookie> login() throws IOException, InternalError {
        URL url = new URL(this.freeIpaURL + "/ipa/session/login_password");
        HttpsURLConnection userConnection = (HttpsURLConnection) url.openConnection();
        userConnection.setRequestMethod("POST");
        userConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        userConnection.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(
                userConnection.getOutputStream());
        String credentials = "user=" + this.username + "&" + "password=" + this.password;
        wr.writeBytes(credentials);
        wr.close();
        String cookiesHeader = userConnection.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
        System.out.println("IPA Cookies: " + cookies);
        return cookies;
    }

    /**
     * To update expired password. If admin sets user password. It should be updated
     * when user logged in.
     */
    public void changePassword(String newPassword) throws IOException, InternalError {
        URL url = new URL(this.freeIpaURL + "/ipa/session/change_password");
        HttpsURLConnection userConnection = (HttpsURLConnection) url.openConnection();
        userConnection.setRequestMethod("POST");
        userConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        userConnection.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(
                userConnection.getOutputStream());
        String credentials = "user=" + this.username + "&" + "old_password=" + this.password + "&new_password="
                + newPassword;
        wr.writeBytes(credentials);
        wr.close();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(userConnection.getInputStream()));
        String line;
        // Status code is not working properly for this reason message should be read to
        // be ensure
        while ((line = bufferedReader.readLine()) != null) {
            // System.out.println(line);
            if (line.contains("rejected")) {
                throw new InternalError("Password change operation is rejected" + userConnection.getResponseCode());
            }
        }

        // To catch additonal error which is mostly related about health of IPA Service.
        if (userConnection.getResponseCode() != 200) {
            throw new InternalError("Operation is failed with code" + userConnection.getResponseCode());
        }
    }
}
