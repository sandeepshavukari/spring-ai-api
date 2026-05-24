package com.sandeep.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sandeep.model.AuthProvider;
import com.sandeep.model.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Set<Role> roles;
    private AuthProvider provider;
    private String profileImageUrl;
    private int dailyRequestCount;
    private LocalDateTime createdAt;
}
