package org.robolaunch.core.abstracts;

import org.robolaunch.model.account.User;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface UserAdapter {
    String toCreateRequest(User user) throws JsonProcessingException;

    String toCreateRequestWithPassword(User user, String password) throws JsonProcessingException;

    String toUsernameRequest(User user) throws JsonProcessingException;

    String toModel(String username) throws JsonProcessingException;

    String toPasswordRequest(User user, String string) throws JsonProcessingException;

    String toGetUserWithAll(User user) throws JsonProcessingException;

    String findByEmail(String email) throws JsonProcessingException;

    String findByFirstName(String firstName) throws JsonProcessingException;

    String findByUsername(String username) throws JsonProcessingException;

    String toUserUpdate(String currentUsername, User user) throws JsonProcessingException;

    String toUserDelete(User user) throws JsonProcessingException;
}
