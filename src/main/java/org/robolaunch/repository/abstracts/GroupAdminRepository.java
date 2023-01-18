package org.robolaunch.repository.abstracts;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Department;
import org.robolaunch.models.DepartmentBasic;
import org.robolaunch.models.GroupMember;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;

import com.fasterxml.jackson.databind.JsonNode;

public interface GroupAdminRepository {

        void makeRequest(String body) throws IOException, InternalError, ApplicationException;

        void appendCookie(HttpCookie cookie);

        List<HttpCookie> getCurrentCookies();

        void clearCookies() throws URISyntaxException;

        void addUserToGroup(User user, Organization group)
                        throws IOException, InternalError, ApplicationException;

        void addUserToGroupAsManager(User user, Organization group)
                        throws InternalError,
                        IOException, ApplicationException;

        void removeUserFromGroup(User user, Organization organization)
                        throws InternalError, IOException;

        String getGroupDescription(String groupName) throws InternalError, IOException;

        public ArrayList<Department> getTeams(Organization group, String fieldName)
                        throws InternalError, IOException;

        Boolean doesGroupExist(Organization group) throws InternalError, IOException;

        JsonNode getGroupField(Organization group, String fieldName)
                        throws InternalError, IOException;

        void createGroup(Organization organization)
                        throws InternalError, IOException, ApplicationException;

        String createSubgroup(Organization organization, String teamName)
                        throws InternalError, IOException, ApplicationException;

        void deleteGroup(Organization group)
                        throws InternalError, IOException, ApplicationException;

        void removeUserManagerFromGroup(User user, Organization organization)
                        throws InternalError, IOException, ApplicationException;

        void changeTeamName(Organization organization, String oldTeamName, String newTeamName)
                        throws InternalError, IOException, ApplicationException;

        String getGroupNameFromDescription(Organization organization, String description)
                        throws InternalError, IOException;

        void setInitialManagersForDepartment(Organization organization, DepartmentBasic department)
                        throws InternalError, IOException, ApplicationException;

        Boolean isGroupManager(User user, Organization group)
                        throws InternalError, IOException;

        Boolean isGroupMemberByEmail(String email, Organization group)
                        throws InternalError, IOException;

        Boolean isGroupMember(User user, Organization group)
                        throws InternalError, IOException;

        Set<User> getUsers(Organization group, String fieldName)
                        throws InternalError, IOException;

        ArrayList<GroupMember> getGroupMembers(Organization group)
                        throws InternalError, IOException;

        String getGroupDescription(Organization group)
                        throws InternalError, IOException;

        void addSubgroupToGroup(Organization organization, String teamName)
                        throws InternalError, IOException;

}
