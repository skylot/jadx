package jadx.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class TestEmptySyntheticClasses extends BaseCliIntegrationTest {
	private static final String SAMPLES = "samples/empty-synthetic/";

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void testMappedClass(boolean showEmptySyntheticClasses) throws Exception {
		Path mappings = testDir.resolve("classes.tiny");
		Files.writeString(mappings, "v1\toriginal\trenamed\n"
				+ "CLASS\tsample/Empty\tmapped/DGDF$1\n"
				+ "CLASS\tsample/Base\tmapped/RenamedBase\n");
		List<String> options = options(showEmptySyntheticClasses);
		options.addAll(List.of("--mappings-path", mappings.toString()));
		assertThat(execJadxCli(buildArgs(options, SAMPLES + "Empty.smali", SAMPLES + "Base.smali"))).isZero();

		Path sources = outputDir.resolve("sources");
		assertThat(sources.resolve("mapped/RenamedBase.java")).isRegularFile();
		assertThat(sources.resolve("sample/Empty.java")).doesNotExist();
		Path emptyClass = sources.resolve("mapped/DGDF$1.java");
		if (showEmptySyntheticClasses) {
			assertThat(emptyClass).isRegularFile();
			assertThat(Files.readString(emptyClass))
					.contains("package mapped;", "import java.io.Serializable;")
					.contains("public final class DGDF$1 extends RenamedBase implements Serializable {")
					.contains("{" + System.lineSeparator() + "}");
		} else {
			assertThat(emptyClass).doesNotExist();
		}
	}

	@ParameterizedTest
	@CsvSource({ "false, true", "true, true", "false, false", "true, false" })
	public void testNestedClassAndConstructorCleanup(boolean showEmptySyntheticClasses, boolean moveInnerClasses) throws Exception {
		Path mappings = testDir.resolve("classes.tiny");
		Files.writeString(mappings, "v1\toriginal\trenamed\n"
				+ "CLASS\tsample/Outer\tmapped/OuterAlias\n"
				+ "CLASS\tsample/Outer$Marker\tmapped/OuterAlias$MarkerAlias\n"
				+ "CLASS\tsample/Outer$Target\tmapped/OuterAlias$TargetAlias\n");
		List<String> options = options(showEmptySyntheticClasses);
		options.addAll(List.of("--mappings-path", mappings.toString()));
		if (!moveInnerClasses) {
			options.add("--no-move-inner-classes");
		}
		assertThat(execJadxCli(buildArgs(options,
				SAMPLES + "Outer.smali", SAMPLES + "Marker.smali", SAMPLES + "Target.smali"))).isZero();

		Path sources = outputDir.resolve("sources/mapped");
		String outerCode = Files.readString(sources.resolve("OuterAlias.java"));
		Path markerFile = sources.resolve("OuterAlias$MarkerAlias.java");
		String targetName;
		String targetCode;
		if (moveInnerClasses) {
			targetName = "TargetAlias";
			targetCode = outerCode;
			assertThat(markerFile).doesNotExist();
			assertThat(sources.resolve("OuterAlias$TargetAlias.java")).doesNotExist();
			if (showEmptySyntheticClasses) {
				assertThat(outerCode).contains("public static final class MarkerAlias {");
			} else {
				assertThat(outerCode).doesNotContain("MarkerAlias");
			}
		} else {
			targetName = "OuterAlias$TargetAlias";
			targetCode = Files.readString(sources.resolve(targetName + ".java"));
			assertThat(outerCode).doesNotContain("class MarkerAlias", "class TargetAlias");
			if (showEmptySyntheticClasses) {
				assertThat(markerFile).isRegularFile();
				assertThat(Files.readString(markerFile)).contains("public final class OuterAlias$MarkerAlias {");
			} else {
				assertThat(markerFile).doesNotExist();
			}
		}
		// The marker declaration must not keep the synthetic forwarding constructor or its dummy argument.
		assertThat(targetCode).containsOnlyOnce(targetName + "(int ").doesNotContain(", MarkerAlias", ", OuterAlias$MarkerAlias");
		assertThat(outerCode).contains("return new " + targetName + "(7);").doesNotContain("null");
	}

	private static List<String> options(boolean showEmptySyntheticClasses) {
		List<String> options = new ArrayList<>(List.of("--config", "none", "--comments-level", "none", "--no-res"));
		if (showEmptySyntheticClasses) {
			options.add("--show-empty-synthetic-classes");
		}
		return options;
	}
}
