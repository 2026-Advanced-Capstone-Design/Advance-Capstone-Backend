package com.factcheck.youtube.util;

import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class YoutubeUrlUtils {

    public static String extractVideoId(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();

        if (host == null) {
            throw new BusinessException(ErrorCode.INVALID_YOUTUBE_URL);
        }

        host = host.replace("www.", "");

        // youtu.be/{videoId}
        if (host.equals("youtu.be")) {
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                String videoId = path.substring(1);
                return validateAndReturn(videoId);
            }
        }

        // youtube.com/watch?v={videoId}
        if (host.equals("youtube.com") || host.equals("m.youtube.com")) {
            String path = uri.getPath();

            if ("/watch".equals(path)) {
                String query = uri.getRawQuery();
                if (query != null) {
                    String videoId = Arrays.stream(query.split("&"))
                            .map(param -> param.split("=", 2))
                            .filter(arr -> arr.length == 2)
                            .filter(arr -> arr[0].equals("v"))
                            .map(arr -> URLDecoder.decode(arr[1], StandardCharsets.UTF_8))
                            .findFirst()
                            .orElse(null);
                    if (videoId != null) {
                        return validateAndReturn(videoId);
                    }
                }
            }

            // youtube.com/shorts/{videoId}
            if (path != null && path.startsWith("/shorts/")) {
                String videoId = path.substring("/shorts/".length()).split("/")[0];
                return validateAndReturn(videoId);
            }

            // youtube.com/embed/{videoId}
            if (path != null && path.startsWith("/embed/")) {
                String videoId = path.substring("/embed/".length()).split("/")[0];
                return validateAndReturn(videoId);
            }
        }

        throw new BusinessException(ErrorCode.UNSUPPORTED_YOUTUBE_URL_FORMAT);
    }

    private static String validateAndReturn(String videoId) {
        if (!videoId.matches("[A-Za-z0-9_-]{11}")) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
        }
        return videoId;
    }
}