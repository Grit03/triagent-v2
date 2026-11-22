package io.triagent.analysis;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.triagent.action.ActionResponse;
import io.triagent.action.ActionService;
import io.triagent.action.ActionStatus;
import io.triagent.action.ActionType;
import io.triagent.action.CreateActionRequest;
import io.triagent.incident.IncidentSnapshot;
import io.triagent.investigation.Evidence;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * 에이전트 회귀 평가. 장애 시나리오(src/test/resources/eval/scenarios)를 실제 Claude에 넣고, 기대한 조치를 제안하는 비율을 잽니다. 도구를
 * 호출하지 않고 텍스트로만 요청했다고 쓴 경우도 실패로 셉니다.
 *
 * <p>실제 API를 호출해 비용이 들기 때문에 `./gradlew test`와 CI에서는 실행하지 않고 `./gradlew eval`로만 실행합니다. 조치 요청은 DB 대신
 * Mockito 목에 기록하므로 아무것도 저장되거나 실행되지 않습니다.
 */
@Tag("eval")
class AgentEvalTest {

    /** expectedAction이 null이면 어떤 조치도 제안하지 않아야 통과입니다. */
    record Scenario(
            String description,
            ActionType expectedAction,
            IncidentSnapshot incident,
            List<Evidence> evidences) {}

    record Result(String name, Scenario scenario, List<String> outcomes, int passed) {}

    @Test
    @DisplayName("시나리오별로 Claude가 기대한 조치를 제안하는 비율을 측정한다")
    void measureAgent() throws IOException {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        assumeThat(apiKey).as("ANTHROPIC_API_KEY가 없으면 평가를 건너뜁니다").isNotBlank();
        String model = System.getProperty("eval.model", "claude-haiku-5-5");
        String effort = System.getProperty("eval.effort", "low");
        int repeat = Integer.getInteger("eval.repeat", 3);
        AnthropicProperties properties = new AnthropicProperties(apiKey, model, 4000, effort);

        List<Result> results = new ArrayList<>();
        AnthropicClient client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        try {
            for (Resource resource : scenarioFiles()) {
                Scenario scenario =
                        JsonMapper.shared().readValue(resource.getInputStream(), Scenario.class);
                String name = resource.getFilename().replace(".json", "");
                results.add(evaluate(client, properties, name, scenario, repeat));
            }
        } finally {
            client.close();
        }
        String report = report(model, effort, repeat, results);
        System.out.println(report);
        Path path = Path.of("build/reports/eval/report.md");
        Files.createDirectories(path.getParent());
        Files.writeString(path, report);
    }

    private static List<Resource> scenarioFiles() throws IOException {
        Resource[] resources =
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:eval/scenarios/*.json");
        return Arrays.stream(resources)
                .sorted(Comparator.comparing(Resource::getFilename))
                .toList();
    }

    /** 같은 시나리오를 여러 번 돌립니다. 모델 응답은 매번 달라질 수 있어 한 번의 결과로는 판단할 수 없기 때문입니다. */
    private static Result evaluate(
            AnthropicClient client,
            AnthropicProperties properties,
            String name,
            Scenario scenario,
            int repeat) {
        Set<ActionType> expected =
                scenario.expectedAction() == null
                        ? EnumSet.noneOf(ActionType.class)
                        : EnumSet.of(scenario.expectedAction());
        List<String> outcomes = new ArrayList<>();
        int passed = 0;
        for (int run = 0; run < repeat; run++) {
            Set<ActionType> proposed = EnumSet.noneOf(ActionType.class);
            ActionService actionService = mock(ActionService.class);
            given(actionService.request(any(), any(), any()))
                    .willAnswer(
                            invocation -> {
                                CreateActionRequest request = invocation.getArgument(1);
                                proposed.add(request.type());
                                return new ActionResponse(
                                        (long) proposed.size(),
                                        1L,
                                        "payment-api",
                                        request.type(),
                                        request.reason(),
                                        ActionStatus.PENDING,
                                        invocation.getArgument(2),
                                        Instant.now(),
                                        null,
                                        null,
                                        null,
                                        null);
                            });
            try {
                String analysis =
                        new ClaudeIncidentAnalyzer(client, properties, actionService)
                                .analyze(scenario.incident(), scenario.evidences());
                saveAnalysis(name, run, analysis);
                // 도구를 호출하지 않았는데 텍스트로 요청했다고 쓰면, 기대 조치와 맞더라도 실패입니다.
                boolean falseClaim = proposed.isEmpty() && RequestClaims.claimsRequest(analysis);
                String outcome = proposed.isEmpty() ? "없음" : proposed.toString();
                outcomes.add(falseClaim ? outcome + " (텍스트는 요청했다고 씀)" : outcome);
                passed += proposed.equals(expected) && !falseClaim ? 1 : 0;
            } catch (RuntimeException ex) {
                outcomes.add("오류(" + ex.getMessage() + ")");
            }
        }
        return new Result(name, scenario, outcomes, passed);
    }

    /** 실패 원인을 나중에 확인할 수 있도록 분석 텍스트를 남깁니다. */
    private static void saveAnalysis(String name, int run, String analysis) {
        try {
            Path path = Path.of("build/reports/eval/analyses/%s-%d.md".formatted(name, run + 1));
            Files.createDirectories(path.getParent());
            Files.writeString(path, analysis);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String report(String model, String effort, int repeat, List<Result> results) {
        StringBuilder report = new StringBuilder("## 에이전트 평가 결과\n\n");
        report.append("모델 `%s`, effort `%s`, 시나리오마다 %d회 실행\n\n".formatted(model, effort, repeat));
        report.append("| 시나리오 | 기대 조치 | 통과 | 실제 제안 |\n| --- | --- | --- | --- |\n");
        int passed = 0;
        for (Result result : results) {
            ActionType expected = result.scenario().expectedAction();
            report.append(
                    "| %s | %s | %d/%d | %s |\n"
                            .formatted(
                                    result.name(),
                                    expected == null ? "없음" : expected,
                                    result.passed(),
                                    result.outcomes().size(),
                                    String.join(", ", result.outcomes())));
            passed += result.passed();
        }
        int total = results.size() * repeat;
        report.append(
                "\n전체 통과율: %d/%d (%.0f%%)\n".formatted(passed, total, 100.0 * passed / total));
        report.append(
                "\n\"(텍스트는 요청했다고 씀)\"은 도구를 호출하지 않았는데 분석에 요청했다고 쓴 경우로, 실패로 셉니다."
                        + " 분석 텍스트는 `build/reports/eval/analyses`에 있습니다.\n");
        return report.toString();
    }
}
