package org.robolaunch.core.concretes;

import javax.enterprise.context.ApplicationScoped;

import org.robolaunch.model.account.LoginRequest;
import org.robolaunch.model.account.User;

@ApplicationScoped
public class AuthHelper {
    public LoginRequest generateLoginRequest(User user, String password) {
        LoginRequest login = new LoginRequest();
        login.setUsername(user.getUsername());
        login.setPassword(password);

        return login;
    }
}
