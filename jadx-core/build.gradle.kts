plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-plugins:jadx-input-api"))
	api(project(":jadx-commons:jadx-zip"))

	implementation("com.google.code.gson:gson:2.13.2")

	testImplementation("org.apache.commons:commons-lang3:3.20.0")

	testImplementation(project(":jadx-plugins:jadx-dex-input"))
	testRuntimeOnly(project(":jadx-plugins:jadx-smali-input"))
	testRuntimeOnly(project(":jadx-plugins:jadx-java-convert"))
	testRuntimeOnly(project(":jadx-plugins:jadx-java-input"))
	testRuntimeOnly(project(":jadx-plugins:jadx-raung-input"))

	testImplementation("org.eclipse.jdt:ecj") {
		version {
			prefer("3.33.0")
			strictly("[3.33, 3.34[") // from 3.34 compiled with Java 17
		}
	}
	testImplementation("tools.profiler:async-profiler:4.2")
}

val jadxTestJavaVersion = getTestJavaVersion()

fun getTestJavaVersion(): Int? {
	val envVarName = "JADX_TEST_JAVA_VERSION"
	val testJavaVer = System.getenv(envVarName)?.toInt() ?: return null
	val currentJavaVer = java.toolchain.languageVersion.get().asInt()
	if (testJavaVer < currentJavaVer) {
		throw GradleException("'$envVarName' can't be set to lower version than $currentJavaVer")
	}
	println("Set Java toolchain for core tests to version '$testJavaVer'")
	return testJavaVer
}

tasks.named<Test>("test") {
	jadxTestJavaVersion?.let { testJavaVer ->
		javaLauncher =
			javaToolchains.launcherFor {
				languageVersion = JavaLanguageVersion.of(testJavaVer)
			}
	}

	// disable cache to allow test's rerun,
	// because most tests are integration and depends on plugins and environment
	outputs.cacheIf { false }

	// exclude temp tests
	exclude("**/tmp/*")
}
