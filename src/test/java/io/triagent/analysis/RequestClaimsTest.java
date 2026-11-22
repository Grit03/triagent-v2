package io.triagent.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestClaimsTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1.8.0 롤백을 승인 요청했습니다 (승인 필요)",
                "롤백 승인 대기 요청을 등록했습니다.",
                "재시작 요청을 보냈습니다.",
                "- v1.8.0 롤백 (승인 대기, 조치 ID 1)",
            })
    void claims(String analysis) {
        assertThat(RequestClaims.claimsRequest(analysis)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "롤백을 검토하세요 (제안하지 않음): 배포 이력이 없습니다.",
                "조치는 요청하지 않았습니다. DB 쪽 확인이 먼저입니다.",
                "*권장 조치*: payment-db 커넥션 풀 설정을 확인하세요.",
                "",
            })
    void noClaim(String analysis) {
        assertThat(RequestClaims.claimsRequest(analysis)).isFalse();
    }
}
