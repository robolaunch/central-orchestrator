package org.robolaunch.core.concretes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.robolaunch.core.abstracts.GroupAdapter;
import org.robolaunch.core.abstracts.RandomGenerator;
import org.robolaunch.models.DepartmentBasic;
import org.robolaunch.models.Group;
import org.robolaunch.models.Organization;
import org.robolaunch.models.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class GroupIPAAdapter implements GroupAdapter {

        @Override
        public String toRequest(Group group) throws JsonProcessingException {
                var params = new ArrayList<String>();
                params.add(group.getName());
                HashMap<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();
                data.put("groups", params);
                Object[] request = new Object[] {
                                params,
                                data,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        @Override
        public String toRequestWithUser(User user, Organization organization) throws JsonProcessingException {
                var params = new ArrayList<String>();
                params.add(organization.getName());
                var userArray = new ArrayList<String>();
                userArray.add(user.getUsername());

                HashMap<String, ArrayList<String>> groupData = new HashMap<String, ArrayList<String>>();
                groupData.put("user", userArray);
                Object[] request = new Object[] {
                                params,
                                groupData,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        @Override
        public String toGroupRequest(User user, Organization organization) throws JsonProcessingException {
                List<String> userGroup = new ArrayList<String>();
                userGroup.add(organization.getName());

                List<String> username = new ArrayList<String>();
                username.add(user.getUsername());
                Map userMap = new HashMap();
                userMap.put("user", username);
                userMap.put("version", "2.245");

                Object[] request = new Object[] {
                                userGroup,
                                userMap
                };

                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;

        }

        @Override
        public String toCreateGroup(Organization organization) throws JsonProcessingException {
                var params = new ArrayList<String>();
                params.add(organization.getName());
                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("description", organization.getName().substring(4));
                versionInfo.put("version", "2.245");

                Object[] request = new Object[] {
                                params,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        /* Returns the request and the generated subgroup name. */
        @Override
        public ArrayList<String> toCreateSubgroup(Organization organization, DepartmentBasic department)
                        throws JsonProcessingException {
                var params = new ArrayList<String>();
                RandomGenerator randomGenerator = new RandomGeneratorImpl();
                params.add(organization.getName() + "-dep-" + randomGenerator.generateRandomString(8));
                ArrayList<String> array = new ArrayList<String>();

                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("description", department.getName());
                versionInfo.put("version", "2.245");
                Object[] request = new Object[] {
                                params,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

                array.add(result);
                array.add(params.get(0));
                return array;
        }

        @Override
        public String toDeleteGroup(Organization group) throws JsonProcessingException {
                var deletedGroupArray = new ArrayList<ArrayList<String>>();
                var deletedGroup = new ArrayList<String>();
                deletedGroup.add(group.getName());
                deletedGroupArray.add(deletedGroup);
                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("version", "2.245");
                Object[] request = new Object[] {
                                deletedGroupArray,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        @Override
        public String toAssignSubgroup(Organization group, DepartmentBasic subgroup) throws JsonProcessingException {
                List<String> upperGroup = new ArrayList<String>();
                upperGroup.add(group.getName());

                List<String> lowerGroup = new ArrayList<String>();
                lowerGroup.add(subgroup.getName());
                Map userMap = new HashMap();
                userMap.put("group", lowerGroup);
                userMap.put("version", "2.245");

                Object[] request = new Object[] {
                                upperGroup,
                                userMap
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;

        }

        @Override
        public String toGetWithAllFlag(Organization organization) throws JsonProcessingException {
                List<String> groupName = new ArrayList<String>();
                groupName.add(organization.getName());
                Map versionInfo = new HashMap();
                versionInfo.put("all", true);
                versionInfo.put("version", "2.245");
                Object[] request = new Object[] {
                                groupName,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

                return result;
        }

        @Override
        public String toGetUsersWithParams(Organization group) throws JsonProcessingException {
                List<String> emptyArray = new ArrayList<String>();
                Map otherParams = new HashMap();
                List<String> groupName = new ArrayList<String>();
                groupName.add(group.getName());
                otherParams.put("in_group", groupName);
                otherParams.put("version", "2.245");
                Object[] request = new Object[] {
                                emptyArray,
                                otherParams,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        @Override
        public String toChangeGroup(Organization department, String newName) throws JsonProcessingException {
                List<String> groupName = new ArrayList<String>();
                groupName.add(department.getName());

                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("description", newName);
                versionInfo.put("version", "2.245");
                Object[] request = new Object[] {
                                groupName,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        @Override
        public String toGetGroup(Organization group) throws JsonProcessingException {
                List<String> groupName = new ArrayList<String>();
                groupName.add(group.getName());
                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("version", "2.245");
                Object[] request = new Object[] {
                                groupName,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        @Override
        public String toCreateDNSRecord(Organization organization, String IPAddress, String zone)
                        throws JsonProcessingException {
                ArrayList<String> params = new ArrayList<String>();
                params.add(zone);
                params.add(organization.getName());

                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("a_part_ip_address", IPAddress);
                versionInfo.put("version", "2.245");
                Object[] request = new Object[] {
                                params,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

        @Override
        public String toDeleteDNSRecord(Organization organization, String IPAddress, String zone)
                        throws JsonProcessingException {
                ArrayList params = new ArrayList();
                params.add(zone);
                HashMap<String, String> dnsName = new HashMap<String, String>();
                dnsName.put("__dns_name__", organization.getName());
                params.add(dnsName);

                ArrayList<String> recordArray = new ArrayList<String>();
                recordArray.add(IPAddress);
                HashMap versionInfo = new HashMap();
                versionInfo.put("arecord", recordArray);
                versionInfo.put("version", "2.245");

                Object[] request = new Object[] {
                                params,
                                versionInfo,
                };
                ObjectMapper objectMapper = new ObjectMapper();
                var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                return result;
        }

}
