package org.robolaunch.core.concretes;

import javax.enterprise.context.ApplicationScoped;

import org.robolaunch.models.LoginRequest;
import org.robolaunch.models.User;

@ApplicationScoped
public class AuthHelper {
    public LoginRequest generateLoginRequest(User user, String password) {
        LoginRequest login = new LoginRequest();
        login.setUsername(user.getUsername());
        login.setPassword(password);

        return login;
    }
}
