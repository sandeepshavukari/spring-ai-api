package com.sandeep.dto.request;

import com.sandeep.model.Role;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateRolesRequest {

    @NotEmpty(message = "At least one role required")
    private Set<Role> roles;
}
