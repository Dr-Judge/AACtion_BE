package com.likelion.drjudge.domain.share.dto.response;

public record ShareLinkResponse(
        String shareToken,
        String shareUrl
) {
    public static ShareLinkResponse of(String token) {
        return new ShareLinkResponse(token, "https://doctorjudge.app/share/" + token);
    }
}