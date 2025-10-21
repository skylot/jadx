import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
	java
	checkstyle

	id("jadx-rewrite")
}

val jadxVersion: String by rootProject.extra
val jadxBuildJavaVersion: Int? by rootProject.extra

group = "io.github.skylot"
version = jadxVersion

dependencies {
	implementation("org.slf4j:slf4j-api:2.0.17")
	compileOnly("org.jetbrains:annotations:26.0.2")

	testImplementation("ch.qos.logback:logback-classic:1.5.20")
	testImplementation("org.assertj:assertj-core:3.27.6")

	testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testCompileOnly("org.jetbrains:annotations:26.0.2")
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
