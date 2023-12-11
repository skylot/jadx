import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.Locale

plugins {
	id("com.github.ben-manes.versions") version "0.50.0"
	id("se.patrikerdes.use-latest-versions") version "0.2.18"
	id("com.diffplug.spotless") version "6.23.3"
}

val jadxVersion by extra { System.getenv("JADX_VERSION") ?: "dev" }
println("jadx version: $jadxVersion")
version = jadxVersion

allprojects {
	apply(plugin = "java")
	apply(plugin = "checkstyle")
	apply(plugin = "com.diffplug.spotless")
	apply(plugin = "com.github.ben-manes.versions")
	apply(plugin = "se.patrikerdes.use-latest-versions")

	repositories {
		mavenCentral()
	}

	configure<SpotlessExtension> {
		java {
			importOrderFile("$rootDir/config/code-formatter/eclipse.importorder")
			eclipse().configFile("$rootDir/config/code-formatter/eclipse.xml")
			removeUnusedImports()
			commonFormatOptions()
		}
		kotlin {
			ktlint().editorConfigOverride(mapOf("indent_style" to "tab"))
			commonFormatOptions()
		}
		kotlinGradle {
			ktlint()
			commonFormatOptions()
		}
		format("misc") {
			target("**/*.gradle", "**/*.xml", "**/.gitignore", "**/.properties")
			targetExclude(".gradle/**", ".idea/**", "*/build/**")
			commonFormatOptions()
		}
	}

	tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
		rejectVersionIf {
			// disallow release candidates as upgradable versions from stable versions
			isNonStable(candidate.version) && !isNonStable(currentVersion)
		}
	}
}

fun FormatExtension.commonFormatOptions() {
	lineEndings = LineEnding.UNIX
	encoding = Charsets.UTF_8
	trimTrailingWhitespace()
	endWithNewline()
}

fun isNonStable(version: String): Boolean {
	val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase(Locale.getDefault()).contains(it) }
	val regex = "^[0-9,.v-]+(-r)?$".toRegex()
	val isStable = stableKeyword || regex.matches(version)
	return isStable.not()
}

val copyArtifacts by tasks.registering(Copy::class) {
	val jarCliPattern = "jadx-cli-(.*)-all.jar".toPattern()
	from(tasks.getByPath(":jadx-cli:installShadowDist")) {
		exclude("**/*.jar")
		filter { line ->
			jarCliPattern.matcher(line).replaceAll("jadx-$1-all.jar")
				.replace("-jar \"\\\"\$CLASSPATH\\\"\"", "-cp \"\\\"\$CLASSPATH\\\"\" jadx.cli.JadxCLI")
				.replace("-jar \"%CLASSPATH%\"", "-cp \"%CLASSPATH%\" jadx.cli.JadxCLI")
		}
	}
	val jarGuiPattern = "jadx-gui-(.*)-all.jar".toPattern()
	from(tasks.getByPath(":jadx-gui:installShadowDist")) {
		exclude("**/*.jar")
		filter { line -> jarGuiPattern.matcher(line).replaceAll("jadx-$1-all.jar") }
	}
	from(tasks.getByPath(":jadx-gui:installShadowDist")) {
		include("**/*.jar")
		rename("jadx-gui-(.*)-all.jar", "jadx-$1-all.jar")
	}
	into(layout.buildDirectory.dir("jadx"))
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val pack by tasks.registering(Zip::class) {
	from(copyArtifacts)
	archiveFileName.set("jadx-$jadxVersion.zip")
	destinationDirectory.set(layout.buildDirectory)
}

val copyExe by tasks.registering(Copy::class) {
	group = "jadx"
	description = "Copy exe to build dir"

	// next task dependencies not needed, but gradle throws warning because of same output dir
	mustRunAfter("jar")
	mustRunAfter(pack)

	from(tasks.getByPath("jadx-gui:createExe"))
	include("*.exe")
	into(layout.buildDirectory)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val distWinBundle by tasks.registering(Copy::class) {
	group = "jadx"
	description = "Copy bundle to build dir"

	dependsOn(tasks.getByPath(":jadx-gui:distWinWithJre"))

	// next task dependencies not needed, but gradle throws warning because of same output dir
	mustRunAfter("jar")
	mustRunAfter(pack)

	from(tasks.getByPath("jadx-gui:distWinWithJre").outputs) {
		include("*.zip")
	}
	into(layout.buildDirectory)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val dist by tasks.registering {
	group = "jadx"
	description = "Build jadx distribution zip"

	dependsOn(pack)

	val os = DefaultNativePlatform.getCurrentOperatingSystem()
	if (os.isWindows) {
		if (project.hasProperty("bundleJRE")) {
			println("Build win bundle with JRE")
			dependsOn(distWinBundle)
		} else {
			dependsOn(copyExe)
		}
	}
}

val cleanBuildDir by tasks.registering(Delete::class) {
	group = "jadx"
	delete(layout.buildDirectory)
}
tasks.getByName("clean").dependsOn(cleanBuildDir)
