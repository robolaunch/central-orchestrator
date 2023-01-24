package org.robolaunch.model.account;

import java.io.Serializable;

public class LoginRefreshToken implements Serializable {

    private String refreshToken;

    public LoginRefreshToken() {
    }

    public LoginRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
