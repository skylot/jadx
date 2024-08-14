package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestComplexIf2 extends SmaliTest {

	// @formatter:off
	/*
		public void test() {
			if (this.isSaved) {
				throw new RuntimeException("Error");
			}
			if (LoaderUtils.isContextLoaderAvailable()) {
				this.savedContextLoader = LoaderUtils.getContextClassLoader();
				ClassLoader loader = this;
				if (this.project != null && "simple".equals(this.project)) {
					loader = getClass().getClassLoader();
				}
				LoaderUtils.setContextClassLoader(loader);
				this.isSaved = true;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmaliWithPkg("conditions", "TestComplexIf2")).code()
				.containsOne("if (this.project != null && \"simple\".equals(this.project)) {");
	}
}
