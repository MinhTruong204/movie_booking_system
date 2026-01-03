package com.viecinema.auth.mapper;

import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // Define this interface as a Spring bean
public interface UserMapper {
    User registerRequestToUser(RegisterRequest registerRequest);

    RegisterResponse toRegisterResponse(User user);
}
