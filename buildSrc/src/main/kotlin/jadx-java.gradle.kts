import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
	java
	checkstyle

	id("jadx-rewrite")
}

val jadxVersion: String by rootProject.extra

group = "io.github.skylot"
version = jadxVersion

dependencies {
	implementation("org.slf4j:slf4j-api:2.0.16")
	compileOnly("org.jetbrains:annotations:26.0.0")

	testImplementation("ch.qos.logback:logback-classic:1.5.9")
	testImplementation("org.assertj:assertj-core:3.26.3")

	testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testCompileOnly("org.jetbrains:annotations:26.0.0")
}

repositories {
	mavenCentral()
	// required for: aapt-proto, r8, smali
	google()
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

tasks {
	compileJava {
		options.encoding = "UTF-8"
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
