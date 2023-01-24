package org.robolaunch.dto.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.robolaunch.dto.UserDTO;
import org.robolaunch.model.account.User;

@Mapper(componentModel = "cdi")
public interface UserMapper {

   @Mapping(target = "email", ignore = true)
   UserDTO userToUserDTO(User user);
}
