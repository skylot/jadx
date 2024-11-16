plugins {
	id("org.openrewrite.rewrite")
}

repositories {
	mavenCentral()
}

dependencies {
	rewrite("org.openrewrite.recipe:rewrite-testing-frameworks:2.22.0")
	rewrite("org.openrewrite.recipe:rewrite-logging-frameworks:2.16.0")
	rewrite("org.openrewrite.recipe:rewrite-migrate-java:2.29.0")
	rewrite("org.openrewrite.recipe:rewrite-static-analysis:1.20.0")
}

tasks {
	rewrite {
		// exclusion("src/test/java/jadx/tests/integration")

		// activeRecipe("org.openrewrite.java.migrate.Java8toJava11")

		// checkstyle auto fix
		// activeRecipe("org.openrewrite.staticanalysis.CodeCleanup")
		// setCheckstyleConfigFile(file("$rootDir/config/checkstyle/checkstyle.xml"))

		// logging
		// activeRecipe("org.openrewrite.java.logging.slf4j.Slf4jBestPractices")
		// activeRecipe("org.openrewrite.java.logging.slf4j.LoggersNamedForEnclosingClass")
		// activeRecipe("org.openrewrite.java.logging.slf4j.ParameterizedLogging")
		// activeRecipe("org.openrewrite.java.logging.PrintStackTraceToLogError")

		// testing
		activeRecipe("org.openrewrite.java.testing.assertj.Assertj")
	}
}
