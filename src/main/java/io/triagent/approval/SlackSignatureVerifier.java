package io.triagent.approval;

import io.triagent.notification.SlackProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 서명 = "v0=" + hex(HMAC-SHA256(signingSecret, "v0:" + 타임스탬프 + ":" + 원본 요청 본문))
 *
 * <p>https://docs.slack.dev/authentication/verifying-requests-from-slack
 */
@Component
class SlackSignatureVerifier {

    /** 오래된 요청을 다시 보내는 재전송 공격(replay attack)을 막기 위한 허용 시간 */
    static final Duration MAX_REQUEST_AGE = Duration.ofMinutes(5);

    private final String signingSecret;

    private final Clock clock;

    @Autowired
    SlackSignatureVerifier(SlackProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SlackSignatureVerifier(SlackProperties properties, Clock clock) {
        this.signingSecret = properties.signingSecret();
        this.clock = clock;
    }

    boolean isValid(String timestamp, String signature, String rawBody) {
        if (signingSecret == null
                || signingSecret.isBlank()
                || timestamp == null
                || signature == null) {
            return false;
        }
        if (!isRecent(timestamp)) {
            return false;
        }
        byte[] expected = sign("v0:" + timestamp + ":" + rawBody).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
        // 문자열 equals는 앞에서부터 다른 글자를 만나면 바로 끝나 응답 시간으로 서명을 추측당할 수 있습니다.
        return MessageDigest.isEqual(expected, actual);
    }

    private boolean isRecent(String timestamp) {
        try {
            Instant requestedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            Duration age = Duration.between(requestedAt, clock.instant()).abs();
            return age.compareTo(MAX_REQUEST_AGE) <= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    String sign(String baseString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(
                    new SecretKeySpec(
                            signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return "v0=" + HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 is not available", ex);
        }
    }
}
