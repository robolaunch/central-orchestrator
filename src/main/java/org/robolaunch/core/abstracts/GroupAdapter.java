package org.robolaunch.core.abstracts;

import java.util.ArrayList;
import org.robolaunch.model.account.User;
import org.robolaunch.model.ipa.IPAGroup;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface GroupAdapter {
        String toRequest(IPAGroup Group) throws JsonProcessingException;

        String toGroupRequest(User user, String group) throws JsonProcessingException;

        String toRequestWithUser(User user, String group)
                        throws JsonProcessingException;

        String toCreateGroup(String group) throws JsonProcessingException;

        ArrayList<String> toCreateSubgroup(String group, String subgroup)
                        throws JsonProcessingException;

        String toDeleteGroup(String group) throws JsonProcessingException;

        String toAssignSubgroup(String group, String subgroup) throws JsonProcessingException;

        String toGetWithAllFlag(String group) throws JsonProcessingException;

        String toGetUsersWithParams(String group) throws JsonProcessingException;

        String toGetGroup(String group) throws JsonProcessingException;

        String toChangeGroup(String subgroup, String newName) throws JsonProcessingException;

        String toCreateDNSRecord(String group, String IPAddress, String zone)
                        throws JsonProcessingException;

        String toDeleteDNSRecord(String group, String IPAddress, String zone)
                        throws JsonProcessingException;
}
