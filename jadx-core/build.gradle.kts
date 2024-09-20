plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-plugins:jadx-input-api"))

	implementation("com.google.code.gson:gson:2.11.0")

	testImplementation("org.apache.commons:commons-lang3:3.17.0")

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
	testImplementation("tools.profiler:async-profiler:3.0")
}

tasks.test {
	exclude("**/tmp/*")

	// disable cache to allow test's rerun,
	// because most tests are integration and depends on plugins and environment
	outputs.cacheIf { false }
}
