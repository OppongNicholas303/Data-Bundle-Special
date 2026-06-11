package com.space.space_bundle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// SingleOrderUserDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SingleOrderUserDTO {
    private String phoneNumber;
    private String name;
}
