package io.triagent;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 기능 패키지(alert, incident, investigation 등) 사이에 순환 의존이 생기면 빌드를 실패시킵니다.
 * <p>
 * https://www.archunit.org/userguide/html/000_Index.html#_slices
 */
@AnalyzeClasses(packages = "io.triagent", importOptions = ImportOption.DoNotIncludeTests.class)
class PackageDependencyTest {

	@ArchTest
	static final ArchRule featurePackagesAreFreeOfCycles = slices().matching("io.triagent.(*)..")
		.should()
		.beFreeOfCycles();
}
