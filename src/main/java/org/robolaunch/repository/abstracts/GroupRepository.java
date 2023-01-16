package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.robolaunch.models.Department;
import org.robolaunch.models.GroupMember;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;

import com.fasterxml.jackson.databind.JsonNode;

public interface GroupRepository {
        void appendCookie(HttpCookie cookie);

        List<HttpCookie> getCurrentCookies();

        void clearCookies();

        void addSubgroupToGroup(Organization organization, String teamName)
                        throws InternalError, IOException;

        void addUserToGroup(User user, Organization organization)
                        throws IOException, InternalError;

        void removeUserFromGroup(User user, Organization organization)
                        throws InternalError, IOException;

        ArrayList<Department> getTeams(Organization group, String fieldName)
                        throws InternalError, IOException;

        JsonNode getGroupField(Organization group, String fieldName)
                        throws InternalError, IOException;

        Set<User> getUsers(Organization group, String fieldName)
                        throws InternalError, IOException;

        JsonNode getGroup(Organization group) throws InternalError,
                        IOException;

        ArrayList<GroupMember> getGroupMembers(Organization group)
                        throws InternalError, IOException;

        Boolean isGroupMember(User user, Organization group)
                        throws InternalError, IOException;

        Boolean isGroupMemberByEmail(String email, Organization group)
                        throws InternalError, IOException;

        Boolean isGroupManager(User user, Organization group)
                        throws InternalError, IOException;

        String getGroupDescription(Organization group)
                        throws InternalError, IOException;

        Integer getRoboticsCloudCount(Organization organization);

}
