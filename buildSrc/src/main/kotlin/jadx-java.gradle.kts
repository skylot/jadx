import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
	java
	checkstyle
}

val jadxVersion = rootProject.extra["jadxVersion"] as String
val jadxBuildJavaVersion = rootProject.extra["jadxBuildJavaVersion"] as Int?

group = "io.github.skylot"
version = jadxVersion

dependencies {
	implementation(libs.slf4j.api)
	compileOnly(libs.jetbrains.annotations)

	testImplementation(libs.logback.classic)
	testImplementation(libs.assertj)

	testImplementation(libs.junit)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testCompileOnly(libs.jetbrains.annotations)
}

java {
	jadxBuildJavaVersion?.let { buildJavaVer ->
		toolchain {
			languageVersion = JavaLanguageVersion.of(buildJavaVer)
		}
	}
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

checkstyle {
	toolVersion = libs.versions.checkstyle.get()
}

tasks {
	withType<JavaCompile> {
		options.encoding = "UTF-8"
		// options.compilerArgs = listOf("-Xlint:deprecation")
	}
	test {
		useJUnitPlatform()
		maxParallelForks = Runtime.getRuntime().availableProcessors()
		testLogging {
			showExceptions = true
			exceptionFormat = TestExceptionFormat.FULL
			showCauses = true
		}
	}
}
