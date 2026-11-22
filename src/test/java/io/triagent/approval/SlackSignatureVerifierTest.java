package io.triagent.approval;

import static org.assertj.core.api.Assertions.assertThat;

import io.triagent.notification.SlackProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlackSignatureVerifierTest {

    /** Slack 공식 문서의 예시 값입니다. */
    static final String SECRET = "8f742231b10e8888abcd99yyyzzz85a5";

    static final String TIMESTAMP = "1531420618";

    static final String BODY =
            "token=xyzz0WbapA4vBCDEFasx0q6G&team_id=T1DC2JH3J&team_domain=testteamnow&channel_id=G8PSS9T3V&channel_name=foobar&user_id=U2CERLKJA&user_name=roadrunner&command=%2Fwebhook-collect&text=&response_url=https%3A%2F%2Fhooks.slack.com%2Fcommands%2FT1DC2JH3J%2F397700885554%2F96rGlfmibIGlgcZRskXaIFfN&trigger_id=398738663015.47445629121.803a0bc887a14d10d2c447fce8b6703c";

    static final String SIGNATURE =
            "v0=a2114d57b48eac39b9ad189dd8316235a7b4a8d21a10bd27519666489c69b503";

    @Test
    @DisplayName("Slack 문서의 예시 서명을 통과시킨다")
    void validSignature() {
        assertThat(
                        verifierAt(Instant.ofEpochSecond(1531420618))
                                .isValid(TIMESTAMP, SIGNATURE, BODY))
                .isTrue();
    }

    @Test
    @DisplayName("본문이 바뀌면 거부한다")
    void tamperedBody() {
        assertThat(
                        verifierAt(Instant.ofEpochSecond(1531420618))
                                .isValid(TIMESTAMP, SIGNATURE, BODY + "&x=1"))
                .isFalse();
    }

    @Test
    @DisplayName("5분보다 오래된 요청은 거부한다")
    void expiredRequest() {
        assertThat(
                        verifierAt(Instant.ofEpochSecond(1531420618 + 301))
                                .isValid(TIMESTAMP, SIGNATURE, BODY))
                .isFalse();
    }

    @Test
    @DisplayName("Signing Secret이 없으면 모든 요청을 거부한다")
    void noSecret() {
        SlackSignatureVerifier verifier =
                new SlackSignatureVerifier(
                        new SlackProperties(null, ""),
                        Clock.fixed(Instant.ofEpochSecond(1531420618), ZoneOffset.UTC));

        assertThat(verifier.isValid(TIMESTAMP, SIGNATURE, BODY)).isFalse();
    }

    private static SlackSignatureVerifier verifierAt(Instant now) {
        return new SlackSignatureVerifier(
                new SlackProperties(null, SECRET), Clock.fixed(now, ZoneOffset.UTC));
    }
}
