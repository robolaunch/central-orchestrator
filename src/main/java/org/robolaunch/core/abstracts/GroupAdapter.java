package org.robolaunch.core.abstracts;

import java.util.ArrayList;

import org.robolaunch.models.DepartmentBasic;
import org.robolaunch.models.Group;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface GroupAdapter {
        String toRequest(Group Group) throws JsonProcessingException;

        String toGroupRequest(User user, Organization organization) throws JsonProcessingException;

        String toRequestWithUser(User user, Organization organization)
                        throws JsonProcessingException;

        String toCreateGroup(Organization organization) throws JsonProcessingException;

        ArrayList<String> toCreateSubgroup(Organization organization, DepartmentBasic department)
                        throws JsonProcessingException;

        String toDeleteGroup(Organization group) throws JsonProcessingException;

        String toAssignSubgroup(Organization group, DepartmentBasic subgroup) throws JsonProcessingException;

        String toGetWithAllFlag(Organization group) throws JsonProcessingException;

        String toGetUsersWithParams(Organization group) throws JsonProcessingException;

        String toGetGroup(Organization group) throws JsonProcessingException;

        String toChangeGroup(Organization department, String newName) throws JsonProcessingException;

        String toCreateDNSRecord(Organization organization, String IPAddress, String zone)
                        throws JsonProcessingException;

        String toDeleteDNSRecord(Organization organization, String IPAddress, String zone)
                        throws JsonProcessingException;
}
