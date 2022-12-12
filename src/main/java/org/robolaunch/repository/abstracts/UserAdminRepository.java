package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.InvitedUser;
import org.robolaunch.models.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface UserAdminRepository {
        void appendCookie(HttpCookie cookie);

        void clearCookies();

        void makeRequest(String body) throws IOException, InternalError, ApplicationException;

        String makeRequestWithResponse(String body, String objectName)
                        throws IOException, InternalError, ApplicationException;

        JsonNode makeRequestForUser(String email) throws MalformedURLException, ProtocolException, IOException;

        JsonNode makeRequestForGroups(String body)
                        throws IOException, InternalError;

        Number makeRequestWithMail(String body) throws IOException, InternalError;

        Boolean doesEmailExist(String email) throws InternalError, IOException;

        void createUser(User user) throws IOException, InternalError, ApplicationException;

        String createUserWithPassword(User user) throws IOException, InternalError, ApplicationException;

        void updateUser(String currentUsername, User user) throws IOException, InternalError, ApplicationException;

        User getUserByEmail(String email) throws IOException;

        void deleteUser(User user) throws IOException, InternalError, ApplicationException;

        String getUserMail(String username) throws IOException, InternalError, ApplicationException;

        User getUserByFirstName(String firstName) throws IOException;

        void updateUserPassword(User user, String password) throws IOException, InternalError, ApplicationException;

        String createUserFromInviteWithPassword(InvitedUser user)
                        throws JsonProcessingException, InternalError, IOException, ApplicationException;

        public User getUserByUsername(String username) throws JsonProcessingException, IOException;

}
