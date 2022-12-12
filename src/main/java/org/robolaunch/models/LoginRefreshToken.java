package org.robolaunch.models;

import java.io.Serializable;

public class LoginRefreshToken implements Serializable {

    private String refreshToken;

    public LoginRefreshToken() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
