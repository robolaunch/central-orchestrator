package org.robolaunch.core.concretes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.robolaunch.core.abstracts.UserAdapter;
import org.robolaunch.models.IPAUser;
import org.robolaunch.models.User;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class UserIPAAdapter implements UserAdapter {

    @Override
    @Deprecated
    public String toCreateRequest(User user) throws JsonProcessingException {
        var params = new ArrayList<String>();
        params.add(user.getUsername());
        var ipaUser = new IPAUser();
        ipaUser.setGivenname(user.getFirstName());
        ipaUser.setSn(user.getInvitedOrganization() + "&" + user.getInvitedBy() + "&" + user.getLastName()
                + "&" + user.getInvitedStatus());
        ipaUser.setVersion("2.246");

        Object[] request = new Object[] {
                params,
                ipaUser,
        };
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    public String toUsernameRequest(User user) throws JsonProcessingException {
        var params = new ArrayList<String>();
        params.add(user.getUsername());
        HashMap<String, String> versionInfo = new HashMap<>();
        versionInfo.put("version", "2.246");
        Object[] request = new Object[] {
                params,
                versionInfo,
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String toModel(String username) throws JsonProcessingException {
        var params = new ArrayList<String>();
        params.add(username);
        HashMap<String, String> versionInfo = new HashMap<>();
        versionInfo.put("version", "2.246");
        Object[] request = new Object[] {
                params,
                versionInfo,
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String toPasswordRequest(User user, String pass) throws JsonProcessingException {
        List<String> userPass = new ArrayList<String>();
        userPass.add(user.getUsername());
        userPass.add(pass);
        HashMap<String, String> versionMap = new HashMap<String, String>();
        versionMap.put("version", "2.246");
        Object[] request = new Object[] {
                userPass,
                versionMap
        };

        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String toCreateRequestWithPassword(User user, String password) throws JsonProcessingException {
        var params = new ArrayList<String>();
        params.add(user.getUsername());
        var mailVar = new ArrayList<String>();
        mailVar.add(user.getEmail());
        var ipaUser = new IPAUser();
        ipaUser.setGivenname(user.getFirstName());
        ipaUser.setSn(user.getLastName());
        ipaUser.setVersion("2.246");
        ipaUser.setPassExpirationDefault();
        ipaUser.setUserpassword(password);
        ipaUser.setMail(mailVar);
        Object[] request = new Object[] {
                params,
                ipaUser,
        };
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String toGetUserWithAll(User user) throws JsonProcessingException {
        List<String> userPass = new ArrayList<String>();
        userPass.add(user.getUsername());
        HashMap<String, Object> versionMap = new HashMap<String, Object>();
        versionMap.put("all", true);
        versionMap.put("version", "2.246");
        Object[] request = new Object[] {
                userPass,
                versionMap
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String findByEmail(String email) throws JsonProcessingException {
        var emptyArray = new ArrayList<String>();
        var params = new ArrayList<String>();
        HashMap<String, Object> versionMap = new HashMap<String, Object>();
        params.add(email);
        versionMap.put("mail", params);
        versionMap.put("version", "2.246");
        Object[] request = new Object[] {
                emptyArray,
                versionMap
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String findByUsername(String username) throws JsonProcessingException {
        var params = new ArrayList<String>();
        HashMap<String, Object> versionMap = new HashMap<String, Object>();
        params.add(username);
        versionMap.put("version", "2.246");
        Object[] request = new Object[] {
                params,
                versionMap
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String findByFirstName(String firstName) throws JsonProcessingException {
        var emptyArray = new ArrayList<String>();
        HashMap<String, Object> versionMap = new HashMap<String, Object>();
        versionMap.put("givenname", firstName);
        versionMap.put("version", "2.246");
        Object[] request = new Object[] {
                emptyArray,
                versionMap
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String toUserUpdate(String currentUsername, User user) throws JsonProcessingException {
        var currentUser = new ArrayList<String>();
        currentUser.add(currentUsername);
        HashMap<String, String> userMap = new HashMap<String, String>();
        userMap.put("rename", user.getUsername());
        userMap.put("givenname", user.getFirstName());
        userMap.put("sn", user.getLastName());
        userMap.put("version", "2.246");
        Object[] request = new Object[] {
                currentUser,
                userMap
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        return result;
    }

    @Override
    public String toUserDelete(User user) throws JsonProcessingException {
        var deletedUserArray = new ArrayList<ArrayList<String>>();
        var deletedUser = new ArrayList<String>();
        deletedUser.add(user.getUsername());
        deletedUserArray.add(deletedUser);
        HashMap<String, String> versionInfo = new HashMap<String, String>();
        versionInfo.put("version", "2.246");
        Object[] request = new Object[] {
                deletedUserArray,
                versionInfo,
        };
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        System.out.println("result: " + result);
        return result;
    }

}
