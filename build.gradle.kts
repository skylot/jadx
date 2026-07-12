import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.Locale

plugins {
	id("com.github.ben-manes.versions") version "0.54.0"
	id("se.patrikerdes.use-latest-versions") version "0.2.19"
	id("com.diffplug.spotless") version "8.8.0"
}

val jadxEnv = loadEnv(file("$rootDir/.env"))

val jadxVersion = jadxEnv["JADX_VERSION"] ?: "dev"
extra.set("jadxVersion", jadxVersion)
println("jadx version: $jadxVersion")
version = jadxVersion

val jadxBuildJavaVersion = getBuildJavaVersion()
extra.set("jadxBuildJavaVersion", jadxBuildJavaVersion)

fun getBuildJavaVersion(): Int? {
	val envVarName = "JADX_BUILD_JAVA_VERSION"
	val buildJavaVer = jadxEnv[envVarName]?.toInt() ?: return null
	if (buildJavaVer < 11) {
		throw GradleException("'$envVarName' can't be set to lower than 11")
	}
	println("Set Java toolchain for jadx build to version '$buildJavaVer'")
	return buildJavaVer
}

// control ErrorProne checks level, can be: off, warn, error
val jadxBuildChecksMode = getBuildChecksMode()
extra.set("jadxBuildChecksMode", jadxBuildChecksMode)

fun getBuildChecksMode(): String {
	val buildChecksMode = jadxEnv["JADX_BUILD_CHECKS_MODE"]?.lowercase() ?: "off"
	val expectedValues = listOf("off", "warn", "error")
	if (!expectedValues.contains(buildChecksMode)) {
		throw GradleException("Unknown check mode: '$buildChecksMode', should be one of $expectedValues")
	}
	if (buildChecksMode != "off") {
		val javaVersion = jadxBuildJavaVersion?.let { JavaVersion.toVersion(it) } ?: JavaVersion.current()
		if (!javaVersion.isCompatibleWith(JavaVersion.VERSION_21)) {
			throw GradleException("Error Prone requires Java 21")
		}
	}
	return buildChecksMode
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

fun loadEnv(file: File): Map<String, String> {
	val envMap = HashMap<String, String>()
	System
		.getenv()
		.filter { it.key.startsWith("JADX_") }
		.forEach { envMap[it.key] = it.value }
	if (file.exists()) {
		file
			.readLines()
			.map { it.trim() }
			.filter { it.isNotEmpty() && !it.startsWith("#") }
			.forEach {
				val (k, v) = it.split("=", limit = 2)
				envMap[k.trim()] = v.trim()
			}
	}
	println(
		"Loaded env vars (${envMap.size}):\n${
			envMap.toList().sortedBy { it.first }.joinToString(separator = "\n") { "${it.first}=${it.second}" }
		}\n",
	)
	return envMap
}

val distWinConfiguration =
	configurations.create("distWinConfiguration") {
		isCanBeConsumed = false
	}
val distWinWithJreConfiguration =
	configurations.create("distWinWithJreConfiguration") {
		isCanBeConsumed = false
	}
val distMacConfiguration =
	configurations.create("distMacConfiguration") {
		isCanBeConsumed = false
	}
dependencies {
	distWinConfiguration(project(":jadx-gui", "distWinConfiguration"))
	distWinWithJreConfiguration(project(":jadx-gui", "distWinWithJreConfiguration"))
	distMacConfiguration(project(":jadx-gui", "distMacConfiguration"))
}

val copyArtifacts =
	tasks.register<Copy>("copyArtifacts") {
		val jarCliPattern = "jadx-cli-(.*)-all.jar".toPattern()
		from(tasks.getByPath(":jadx-cli:installShadowDist")) {
			exclude("**/*.jar")
			filter { line ->
				jarCliPattern
					.matcher(line)
					.replaceAll("jadx-$1-all.jar")
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

val pack =
	tasks.register<Zip>("pack") {
		from(copyArtifacts)
		archiveFileName.set("jadx-$jadxVersion.zip")
		destinationDirectory.set(layout.buildDirectory)
		eachFile {
			if (path == "bin/jadx" || path == "bin/jadx-gui") {
				permissions {
					unix("rwxr-xr-x")
				}
			}
		}
	}

val distWin =
	tasks.register<Zip>("distWin") {
		group = "jadx"
		description = "Build Windows bundle"

		from(distWinConfiguration)

		destinationDirectory.set(layout.buildDirectory.dir("distWin"))
		archiveFileName.set("jadx-gui-$jadxVersion-win.zip")
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

val distWinWithJre =
	tasks.register<Zip>("distWinWithJre") {
		description = "Build Windows with JRE bundle"

		from(distWinWithJreConfiguration)

		destinationDirectory.set(layout.buildDirectory.dir("distWinWithJre"))
		archiveFileName.set("jadx-gui-$jadxVersion-with-jre-win.zip")
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

val distMac =
	tasks.register<Copy>("distMac") {
		group = "jadx"
		description = "Build macOS DMG bundle (with bundled JRE)"

		from(distMacConfiguration)

		into(layout.buildDirectory.dir("distMac"))
	}

val dist =
	tasks.register("dist") {
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
		} else if (os.isMacOsX) {
			dependsOn(distMac)
		}
	}

val cleanBuildDir =
	tasks.register<Delete>("cleanBuildDir") {
		delete(layout.buildDirectory)
	}
tasks.getByName("clean").dependsOn(cleanBuildDir)
