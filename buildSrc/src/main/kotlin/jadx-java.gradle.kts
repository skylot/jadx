import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
	java
	checkstyle

	id("jadx-rewrite")
	id("net.ltgt.errorprone")
	id("net.ltgt.nullaway")
}

val jadxVersion: String by rootProject.extra
val jadxBuildJavaVersion: Int? by rootProject.extra
val jadxBuildChecksMode: String by rootProject.extra

group = "io.github.skylot"
version = jadxVersion

dependencies {
	implementation("org.slf4j:slf4j-api:2.0.17")
	compileOnly("org.jetbrains:annotations:26.1.0")

	testImplementation("ch.qos.logback:logback-classic:1.5.32")
	testImplementation("org.assertj:assertj-core:3.27.7")

	testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testCompileOnly("org.jetbrains:annotations:26.1.0")

	errorprone("com.google.errorprone:error_prone_core:2.48.0")
	errorprone("com.uber.nullaway:nullaway:0.13.1")
}

repositories {
	mavenCentral()
	// required for: aapt-proto, r8, smali
	google()
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

tasks {
	compileJava {
		options.encoding = "UTF-8"
		// options.compilerArgs = listOf("-Xlint:deprecation")
	}
	jar {
		manifest {
			attributes("jadx-version" to jadxVersion)
		}
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

tasks.withType<JavaCompile>().configureEach {
	val checkEnabled = jadxBuildChecksMode != "off"
	if (checkEnabled) {
		options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
	}
	options.errorprone {
		isEnabled = checkEnabled
		allErrorsAsWarnings = jadxBuildChecksMode == "warn"
		excludedPaths = ".*/test/.*"
		nullaway {
			if (jadxBuildChecksMode == "error") {
				error()
			}
			annotatedPackages.add("jadx")
		}
		// TODO: fix and enable all checks
		disable("MixedMutabilityReturnType")
		disable("EqualsGetClass")
		disable("OperatorPrecedence")
		disable("UnusedVariable")
		disable("ImmutableEnumChecker")
	}
}
