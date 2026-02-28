package com.space.space_bundle.core.port.out.dto;

import java.util.List;

public record PackageResponseDto(
        boolean success,
        List<PackageDto> packages
) {}