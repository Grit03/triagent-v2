package io.triagent.analysis;

import java.util.regex.Pattern;

/**
 * 분석 텍스트가 조치를 "요청했다"고 주장하는지 판별합니다. 평가에서 도구를 호출하지 않았는데 요청했다고 쓴 경우(텍스트와 실제 동작의 불일치)를 실패로 잡는 데 씁니다.
 *
 * <p>"요청하지 않았습니다", "(제안하지 않음)"처럼 부정하는 문장은 걸리지 않도록 완료형 표현만 찾습니다.
 */
final class RequestClaims {

    private static final Pattern CLAIM = Pattern.compile("요청했|요청을\\s*(등록|보냈|올렸)|등록했|승인\\s*대기");

    private RequestClaims() {}

    static boolean claimsRequest(String analysis) {
        return analysis != null && CLAIM.matcher(analysis).find();
    }
}
