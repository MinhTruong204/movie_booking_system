package com.viecinema.auth.mapper;

import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring") // Define this interface as a Spring bean
public interface UserMapper {
//    @Mapping(target = "gender", expression = "java(registerRequest.getGender() != null ? com.viecinema.common.enums.Gender.valueOf(registerRequest.getGender().toUpperCase()) : null)")
    User registerRequestToUser(RegisterRequest registerRequest);
    RegisterResponse toRegisterResponse(User user);
}
