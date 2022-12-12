package org.robolaunch.core.concretes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.microprofile.config.ConfigProvider;
import org.robolaunch.core.abstracts.IPAAdmin;

public class IPAAdminLogin implements IPAAdmin {
    private String username;
    private String password;
    String freeIpaURL;

    public IPAAdminLogin() {
        this.freeIpaURL = ConfigProvider.getConfig().getValue("freeipa.url", String.class);
        this.username = ConfigProvider.getConfig().getValue("freeipa.username", String.class);
        this.password = ConfigProvider.getConfig().getValue("freeipa.password", String.class);
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
        HttpsURLConnection adminConnection = (HttpsURLConnection) url.openConnection();
        adminConnection.setRequestMethod("POST");
        adminConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        adminConnection.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(
                adminConnection.getOutputStream());
        String credentials = "user=" + this.username + "&" + "password=" + this.password;
        wr.writeBytes(credentials);
        wr.close();
        String cookiesHeader = adminConnection.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
        return cookies;
    }

    @Override
    /** That method is not implemented yet for Admin account. */
    public void changePassword(String newPassword) throws IOException, InternalError {
        // TODO Auto-generated method stub
    }

}
