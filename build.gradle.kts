import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.Locale

plugins {
	id("com.github.ben-manes.versions") version "0.52.0"
	id("se.patrikerdes.use-latest-versions") version "0.2.19"
	id("com.diffplug.spotless") version "6.25.0"
}

val jadxVersion by extra { System.getenv("JADX_VERSION") ?: "dev" }
println("jadx version: $jadxVersion")
version = jadxVersion

val jadxBuildJavaVersion by extra { getBuildJavaVersion() }

fun getBuildJavaVersion(): Int? {
	val envVarName = "JADX_BUILD_JAVA_VERSION"
	val buildJavaVer = System.getenv(envVarName)?.toInt() ?: return null
	if (buildJavaVer < 11) {
		throw GradleException("'$envVarName' can't be set to lower than 11")
	}
	println("Set Java toolchain for jadx build to version '$buildJavaVer'")
	return buildJavaVer
}

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

val distWinConfiguration: Configuration by configurations.creating {
	isCanBeConsumed = false
}
val distWinWithJreConfiguration: Configuration by configurations.creating {
	isCanBeConsumed = false
}
dependencies {
	distWinConfiguration(project(":jadx-gui", "distWinConfiguration"))
	distWinWithJreConfiguration(project(":jadx-gui", "distWinWithJreConfiguration"))
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
	from(layout.projectDirectory) {
		include("README.md")
		include("LICENSE")
	}
	into(layout.buildDirectory.dir("jadx"))
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val pack by tasks.registering(Zip::class) {
	from(copyArtifacts)
	archiveFileName.set("jadx-$jadxVersion.zip")
	destinationDirectory.set(layout.buildDirectory)
}

val distWin by tasks.registering(Zip::class) {
	group = "jadx"
	description = "Build Windows bundle"

	from(distWinConfiguration)

	destinationDirectory.set(layout.buildDirectory.dir("distWin"))
	archiveFileName.set("jadx-gui-$jadxVersion-win.zip")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val distWinWithJre by tasks.registering(Zip::class) {
	description = "Build Windows with JRE bundle"

	from(distWinWithJreConfiguration)

	destinationDirectory.set(layout.buildDirectory.dir("distWinWithJre"))
	archiveFileName.set("jadx-gui-$jadxVersion-with-jre-win.zip")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val dist by tasks.registering {
	group = "jadx"
	description = "Build jadx distribution zip bundles"

	dependsOn(pack)

	val os = DefaultNativePlatform.getCurrentOperatingSystem()
	if (os.isWindows) {
		if (project.hasProperty("bundleJRE")) {
			println("Build win bundle with JRE")
			dependsOn(distWinWithJre)
		} else {
			dependsOn(distWin)
		}
	}
}

val cleanBuildDir by tasks.registering(Delete::class) {
	delete(layout.buildDirectory)
}
tasks.getByName("clean").dependsOn(cleanBuildDir)
