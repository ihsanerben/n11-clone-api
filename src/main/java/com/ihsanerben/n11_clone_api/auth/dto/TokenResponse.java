package com.ihsanerben.n11_clone_api.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
