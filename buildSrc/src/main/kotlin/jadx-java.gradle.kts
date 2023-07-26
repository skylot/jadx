plugins {
	java
	checkstyle
}

val jadxVersion: String by rootProject.extra

group = "io.github.skylot"
version = jadxVersion

dependencies {
	implementation("org.slf4j:slf4j-api:2.0.7")
	compileOnly("org.jetbrains:annotations:24.0.1")

	testImplementation("ch.qos.logback:logback-classic:1.4.8")
	testImplementation("org.hamcrest:hamcrest-library:2.2")
	testImplementation("org.mockito:mockito-core:5.4.0")
	testImplementation("org.assertj:assertj-core:3.24.2")

	testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testCompileOnly("org.jetbrains:annotations:24.0.1")
}

repositories {
	mavenCentral()
	// required for `aapt-proto` and `r8`
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
		testLogging.showExceptions = true
	}
}
