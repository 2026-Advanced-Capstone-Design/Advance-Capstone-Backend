package com.factcheck.youtube.util;

import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YoutubeUrlUtilsTest {

    @Nested
    @DisplayName("extractVideoId 성공 케이스")
    class Success {

        @Test
        @DisplayName("youtu.be 단축 URL에서 videoId를 추출한다")
        void shortUrl() {
            String url = "https://youtu.be/dQw4w9WgXcQ";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("www.youtu.be 단축 URL에서 videoId를 추출한다")
        void shortUrlWithWww() {
            String url = "https://www.youtu.be/dQw4w9WgXcQ";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("youtube.com/watch?v= URL에서 videoId를 추출한다")
        void watchUrl() {
            String url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("youtube.com/watch?v= 에 다른 파라미터가 섞여있어도 videoId를 추출한다")
        void watchUrlWithExtraParams() {
            String url = "https://www.youtube.com/watch?t=30&v=dQw4w9WgXcQ&feature=share";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("m.youtube.com 모바일 URL에서 videoId를 추출한다")
        void mobileUrl() {
            String url = "https://m.youtube.com/watch?v=dQw4w9WgXcQ";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("youtube.com/shorts/ URL에서 videoId를 추출한다")
        void shortsUrl() {
            String url = "https://www.youtube.com/shorts/dQw4w9WgXcQ";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("youtube.com/embed/ URL에서 videoId를 추출한다")
        void embedUrl() {
            String url = "https://www.youtube.com/embed/dQw4w9WgXcQ";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("videoId에 하이픈(-)과 언더스코어(_)가 포함되어도 추출한다")
        void videoIdWithSpecialChars() {
            String url = "https://youtu.be/aB3-cD_efGh";
            assertThat(YoutubeUrlUtils.extractVideoId(url)).isEqualTo("aB3-cD_efGh");
        }
    }

    @Nested
    @DisplayName("extractVideoId 실패 케이스")
    class Failure {

        @Test
        @DisplayName("host가 없는 잘못된 URL이면 INVALID_YOUTUBE_URL 예외가 발생한다")
        void invalidUrl() {
            String url = "not-a-url";
            assertThatThrownBy(() -> YoutubeUrlUtils.extractVideoId(url))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        ErrorCode code = ((BusinessException) ex).getErrorCode();
                        assertThat(code).isEqualTo(ErrorCode.INVALID_YOUTUBE_URL);
                    });
        }

        @Test
        @DisplayName("지원하지 않는 youtube URL 형식이면 UNSUPPORTED_YOUTUBE_URL_FORMAT 예외가 발생한다")
        void unsupportedFormat() {
            String url = "https://www.youtube.com/channel/UCxxxxxx";
            assertThatThrownBy(() -> YoutubeUrlUtils.extractVideoId(url))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        ErrorCode code = ((BusinessException) ex).getErrorCode();
                        assertThat(code).isEqualTo(ErrorCode.UNSUPPORTED_YOUTUBE_URL_FORMAT);
                    });
        }

        @Test
        @DisplayName("youtube가 아닌 다른 도메인이면 UNSUPPORTED_YOUTUBE_URL_FORMAT 예외가 발생한다")
        void nonYoutubeDomain() {
            String url = "https://www.google.com/watch?v=dQw4w9WgXcQ";
            assertThatThrownBy(() -> YoutubeUrlUtils.extractVideoId(url))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        ErrorCode code = ((BusinessException) ex).getErrorCode();
                        assertThat(code).isEqualTo(ErrorCode.UNSUPPORTED_YOUTUBE_URL_FORMAT);
                    });
        }

        @ParameterizedTest
        @DisplayName("videoId가 11자가 아니면 INVALID_VIDEO_ID 예외가 발생한다")
        @ValueSource(strings = {
                "https://youtu.be/short",          // 5자
                "https://youtu.be/thisvideoidistoolong"  // 너무 긴 경우
        })
        void invalidVideoIdLength(String url) {
            assertThatThrownBy(() -> YoutubeUrlUtils.extractVideoId(url))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        ErrorCode code = ((BusinessException) ex).getErrorCode();
                        assertThat(code).isEqualTo(ErrorCode.INVALID_VIDEO_ID);
                    });
        }

        @Test
        @DisplayName("videoId에 허용되지 않는 특수문자가 있으면 INVALID_VIDEO_ID 예외가 발생한다")
        void invalidVideoIdChars() {
            String url = "https://youtu.be/dQw4w9WgX@@";
            assertThatThrownBy(() -> YoutubeUrlUtils.extractVideoId(url))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        ErrorCode code = ((BusinessException) ex).getErrorCode();
                        assertThat(code).isEqualTo(ErrorCode.INVALID_VIDEO_ID);
                    });
        }

        @Test
        @DisplayName("youtube.com/watch URL에 v 파라미터가 없으면 UNSUPPORTED_YOUTUBE_URL_FORMAT 예외가 발생한다")
        void watchUrlWithoutVideoId() {
            String url = "https://www.youtube.com/watch?list=PLxxxxxx";
            assertThatThrownBy(() -> YoutubeUrlUtils.extractVideoId(url))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        ErrorCode code = ((BusinessException) ex).getErrorCode();
                        assertThat(code).isEqualTo(ErrorCode.UNSUPPORTED_YOUTUBE_URL_FORMAT);
                    });
        }
    }
}
