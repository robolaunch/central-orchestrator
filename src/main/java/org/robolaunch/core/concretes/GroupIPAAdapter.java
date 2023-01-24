package org.robolaunch.core.concretes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

import org.robolaunch.core.abstracts.GroupAdapter;
import org.robolaunch.core.abstracts.RandomGenerator;
import org.robolaunch.model.account.User;
import org.robolaunch.model.ipa.IPAGroup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class GroupIPAAdapter implements GroupAdapter {

        @Override
        public String toRequest(IPAGroup group) throws JsonProcessingException {
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
        public String toRequestWithUser(User user, String group) throws JsonProcessingException {
                var params = new ArrayList<String>();
                params.add(group);
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
        public String toGroupRequest(User user, String group) throws JsonProcessingException {
                List<String> userGroup = new ArrayList<String>();
                userGroup.add(group);

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
        public String toCreateGroup(String group) throws JsonProcessingException {
                var params = new ArrayList<String>();
                params.add(group);
                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("description", group.substring(4));
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
        public ArrayList<String> toCreateSubgroup(String group, String subgroup)
                        throws JsonProcessingException {
                var params = new ArrayList<String>();
                RandomGenerator randomGenerator = new RandomGeneratorImpl();
                params.add(group + "-team-" + randomGenerator.generateRandomString(8));
                ArrayList<String> array = new ArrayList<String>();

                HashMap<String, String> versionInfo = new HashMap<String, String>();
                versionInfo.put("description", subgroup);
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
        public String toDeleteGroup(String group) throws JsonProcessingException {
                var deletedGroupArray = new ArrayList<ArrayList<String>>();
                var deletedGroup = new ArrayList<String>();
                deletedGroup.add(group);
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
        public String toAssignSubgroup(String group, String subgroup) throws JsonProcessingException {
                List<String> upperGroup = new ArrayList<String>();
                upperGroup.add(group);

                List<String> lowerGroup = new ArrayList<String>();
                lowerGroup.add(subgroup);
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
        public String toGetWithAllFlag(String group) throws JsonProcessingException {
                List<String> groupName = new ArrayList<String>();
                groupName.add(group);
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
        public String toGetUsersWithParams(String group) throws JsonProcessingException {
                List<String> emptyArray = new ArrayList<String>();
                Map otherParams = new HashMap();
                List<String> groupName = new ArrayList<String>();
                groupName.add(group);
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
        public String toChangeGroup(String subgroup, String newName) throws JsonProcessingException {
                List<String> groupName = new ArrayList<String>();
                groupName.add(subgroup);

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
        public String toGetGroup(String group) throws JsonProcessingException {
                List<String> groupName = new ArrayList<String>();
                groupName.add(group);
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
        public String toCreateDNSRecord(String group, String IPAddress, String zone)
                        throws JsonProcessingException {
                ArrayList<String> params = new ArrayList<String>();
                params.add(zone);
                params.add(group);

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
        public String toDeleteDNSRecord(String group, String IPAddress, String zone)
                        throws JsonProcessingException {
                ArrayList params = new ArrayList();
                params.add(zone);
                HashMap<String, String> dnsName = new HashMap<String, String>();
                dnsName.put("__dns_name__", group);
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
