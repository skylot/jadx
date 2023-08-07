package jadx.tests.integration.special;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestPackageInfoSupport extends SmaliTest {

	@Test
	public void test() {
		disableCompilation();
		List<ClassNode> classes = loadFromSmaliFiles();
		assertThat(searchCls(classes, "special.pkg1.package-info"))
				.satisfies(cls -> assertThat(cls.getAlias()).isEqualTo("package-info")) // shouldn't be renamed
				.code()
				.containsLines(
						"@Deprecated",
						"package special.pkg1;");
		assertThat(searchCls(classes, "special.pkg2.package-info"))
				.code()
				.containsLines(
						"@ApiStatus.Internal",
						"package special.pkg2;",
						"",
						"import org.jetbrains.annotations.ApiStatus;");
	}
}
