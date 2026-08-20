package com.likelion.drjudge.domain.share.dto.response;

public record ShareLinkResponse(
        String shareToken,
        String shareUrl
) {
    public static ShareLinkResponse of(String token, String frontendBaseUrl) {
        return new ShareLinkResponse(token, frontendBaseUrl + "/share/" + token);
    }
}