package com.sandeep.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Name must be 2–50 characters")
    private String name;

    private String profileImageUrl;
}
