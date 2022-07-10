package jadx.tests.api.extensions.profiles;

import java.util.function.Consumer;

import jadx.tests.api.IntegrationTest;

public enum TestProfile implements Consumer<IntegrationTest> {
	DX_J8("dx-j8", test -> {
		test.useTargetJavaVersion(8);
		test.useDexInput("dx");
	}),
	D8_J8("d8-j8", test -> {
		test.useTargetJavaVersion(8);
		test.useDexInput("d8");
	}),
	D8_J11("d8-j11", test -> {
		test.useTargetJavaVersion(11);
		test.useDexInput("d8");
	}),
	D8_J11_DESUGAR("d8-j11-desugar", test -> {
		test.useTargetJavaVersion(11);
		test.useDexInput("d8");
		test.keepParentClassOnInput();
		test.getArgs().getPluginOptions().put("java-convert.d8-desugar", "yes");
	}),
	JAVA8("java-8", test -> {
		test.useTargetJavaVersion(8);
		test.useJavaInput();
	}),
	JAVA11("java-11", test -> {
		test.useTargetJavaVersion(11);
		test.useJavaInput();
	}),
	JAVA17("java-17", test -> {
		test.useTargetJavaVersion(17);
		test.useJavaInput();
	}),
	ECJ_DX_J8("ecj-dx-j8", test -> {
		test.useEclipseCompiler();
		test.useTargetJavaVersion(8);
		test.useDexInput();
	}),
	ECJ_J8("ecj-j8", test -> {
		test.useEclipseCompiler();
		test.useTargetJavaVersion(8);
		test.useJavaInput();
	}),
	ALL("all", null);

	private final String description;
	private final Consumer<IntegrationTest> setup;

	TestProfile(String description, Consumer<IntegrationTest> setup) {
		this.description = description;
		this.setup = setup;
	}

	@Override
	public void accept(IntegrationTest test) {
		this.setup.accept(test);
		test.setOutDirSuffix(description);
	}

	public String getDescription() {
		return description;
	}
}
