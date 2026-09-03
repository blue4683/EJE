package com.skala.miniproject.history.dto;

import java.util.List;

public record ProAccessDto(
        boolean locked,
        boolean available,
        String detailUrl,
        String upgradePath,
        List<String> lockedFeatures
) {
}
