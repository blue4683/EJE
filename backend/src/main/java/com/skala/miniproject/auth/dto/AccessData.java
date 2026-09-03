package com.skala.miniproject.auth.dto;

public record AccessData(String accessToken, String tokenType, long expiresIn) {
}
