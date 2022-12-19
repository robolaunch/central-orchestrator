package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;

import com.fasterxml.jackson.databind.JsonNode;

public interface UserRepository {
        void appendCookie(HttpCookie cookie);

        void clearCookies();

        void makeRequest(String body) throws IOException, InternalError, ApplicationException;

        Number makeRequestWithMail(String body) throws IOException, InternalError;

        JsonNode makeRequestForGroups(String body)
                        throws IOException, InternalError;

        void changePassword(String oldPassword, String newPassword, String username)
                        throws MalformedURLException, ProtocolException, IOException;

        JsonNode makeRequestForUser(String email) throws MalformedURLException, ProtocolException, IOException;

        void updateUser(String currentUsername, User user) throws IOException, InternalError, ApplicationException;

        ArrayList<Organization> getOrganizations(User user) throws IOException;

        Boolean doesEmailExist(String email) throws InternalError, IOException;

        User getUserByEmail(String email) throws IOException;

        User getUserByFirstName(String firstName) throws IOException;

        Boolean doesFirstNameExist(String firstName) throws IOException;

}
