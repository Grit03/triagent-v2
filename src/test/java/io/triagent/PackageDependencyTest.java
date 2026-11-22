package io.triagent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * 패키지 사이의 의존 규칙을 검사합니다.
 *
 * <p>패키지는 기능별(action, incident, audit 등)로 나누고 레이어별 하위 패키지는 두지 않습니다. 대신 레이어 사이의 방향은 아래 규칙으로 강제합니다.
 *
 * <p>https://www.archunit.org/userguide/html/000_Index.html
 */
@AnalyzeClasses(packages = "io.triagent", importOptions = ImportOption.DoNotIncludeTests.class)
class PackageDependencyTest {

    @ArchTest
    static final ArchRule featurePackagesAreFreeOfCycles =
            slices().matching("io.triagent.(*)..").should().beFreeOfCycles();

    /** 컨트롤러는 서비스를 거쳐서만 데이터에 접근합니다. 트랜잭션과 권한 검사가 서비스에 있기 때문입니다. */
    @ArchTest
    static final ArchRule controllersDoNotAccessRepositories =
            noClasses()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .dependOnClassesThat()
                    .areAssignableTo(Repository.class);

    /** 컨트롤러는 엔티티 대신 응답 DTO를 다룹니다. 엔티티 필드를 바꿔도 API 응답이 따라 바뀌지 않게 합니다. */
    @ArchTest
    static final ArchRule controllersDoNotExposeEntities =
            noClasses()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .dependOnClassesThat()
                    .areAnnotatedWith(Entity.class);

    /** 다른 기능의 데이터가 필요하면 그 기능의 서비스를 호출합니다. 저장소를 직접 쓰면 그 기능의 규칙을 건너뛰게 됩니다. */
    @ArchTest
    static final ArchRule repositoriesStayInsideTheirFeature =
            classes()
                    .that()
                    .areAssignableTo(Repository.class)
                    .and()
                    .resideInAPackage("io.triagent..")
                    .should(onlyBeAccessedFromTheirOwnPackage());

    /**
     * 엔티티는 자기 기능 패키지 밖으로 나가지 않습니다. 다른 기능에는 {@code IncidentSnapshot} 같은 값 객체나 ID만 넘깁니다. 엔티티끼리도
     * {@code @ManyToOne} 대신 ID로 참조합니다.
     */
    @ArchTest
    static final ArchRule entitiesStayInsideTheirFeature =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should(onlyBeAccessedFromTheirOwnPackage());

    /** 컨트롤러는 요청의 진입점이므로 다른 클래스가 의존하지 않습니다. */
    @ArchTest
    static final ArchRule nothingDependsOnControllers =
            noClasses().should().dependOnClassesThat().areAnnotatedWith(RestController.class);

    private static ArchCondition<JavaClass> onlyBeAccessedFromTheirOwnPackage() {
        return new ArchCondition<>("only be accessed by classes in the same package") {
            @Override
            public void check(JavaClass target, ConditionEvents events) {
                for (Dependency dependency : target.getDirectDependenciesToSelf()) {
                    JavaClass origin = dependency.getOriginClass();
                    if (!origin.getPackageName().equals(target.getPackageName())) {
                        events.add(
                                SimpleConditionEvent.violated(
                                        dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }
}
