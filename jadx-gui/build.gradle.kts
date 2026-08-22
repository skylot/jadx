import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
	id("jadx-kotlin")
	id("application")
	id("jadx-library")
	id("com.gradleup.shadow") version "8.3.8"
	id("edu.sc.seis.launch4j") version "4.0.0"
	id("org.beryx.runtime") version "2.0.1"
}

dependencies {
	implementation(project(":jadx-core"))
	implementation(project(":jadx-cli"))
	implementation(project(":jadx-plugins-tools"))
	implementation(project(":jadx-commons:jadx-app-commons"))

	// import mappings
	implementation(project(":jadx-plugins:jadx-rename-mappings"))

	implementation("org.jcommander:jcommander:2.0")
	implementation("ch.qos.logback:logback-classic:1.5.21")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

	implementation("com.fifesoft:rsyntaxtextarea:3.6.1")
	implementation("com.fifesoft:autocomplete:3.3.2")
	implementation("org.drjekyll:fontchooser:3.1.0")
	implementation("hu.kazocsaba:image-viewer:1.2.3")
	implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0") // WebP support for image viewer

	implementation("com.formdev:flatlaf:3.7")
	implementation("com.formdev:flatlaf-intellij-themes:3.7")
	implementation("com.formdev:flatlaf-extras:3.7")
	implementation("com.formdev:flatlaf-fonts-inter:4.1")
	implementation("com.formdev:flatlaf-fonts-jetbrains-mono:2.304")

	implementation("com.google.code.gson:gson:2.13.2")
	implementation("org.apache.commons:commons-lang3:3.20.0")
	implementation("org.apache.commons:commons-text:1.15.0")
	implementation("commons-io:commons-io:2.21.0")

	implementation("io.reactivex.rxjava3:rxjava:3.1.12")
	implementation("com.github.akarnokd:rxjava3-swing:3.1.1")
	implementation("com.android.tools.build:apksig:8.13.1")
	implementation("io.github.skylot:jdwp:2.0.0")

	// Library for hex viewing data
	val bined = "0.2.2"
	implementation("org.exbin.bined:bined-swing:$bined")
	implementation("org.exbin.bined:bined-highlight-swing:$bined")
	implementation("org.exbin.bined:bined-swing-section:$bined")
	implementation("org.exbin.auxiliary:binary_data:$bined")
	implementation("org.exbin.auxiliary:binary_data-array:$bined")

	// Library for rendering GraphViz DOT files
	implementation("guru.nidi:graphviz-java:0.18.1")
	implementation("com.eclipsesource.j2v8:j2v8_linux_x86_64:4.6.0")
	implementation("com.eclipsesource.j2v8:j2v8_win32_x86_64:4.6.0")

	testImplementation(
		project
			.project(":jadx-core")
			.sourceSets
			.getByName("test")
			.output,
	)
}

val jadxVersion = rootProject.extra["jadxVersion"] as String

tasks.test {
	exclude("**/tmp/*")
}

application {
	applicationName = ("jadx-gui")
	mainClass.set("jadx.gui.JadxGUI")
	applicationDefaultJvmArgs =
		listOf(
			"-Xms128M",
			"-XX:MaxRAMPercentage=70.0",
			"-Dawt.useSystemAAFontSettings=lcd",
			"-Dswing.aatext=true",
			"-Djava.util.Arrays.useLegacyMergeSort=true",
			// disable zip checks (#1962)
			"-Djdk.util.zip.disableZip64ExtraFieldValidation=true",
			// needed for ktlint formatter
			"-XX:+IgnoreUnrecognizedVMOptions",
			"--add-opens=java.base/java.lang=ALL-UNNAMED",
			// Foreign API access for 'directories' library (Windows only)
			"--enable-native-access=ALL-UNNAMED",
			// flags to fix UI ghosting (#2225)
			"-Dsun.java2d.noddraw=true",
			"-Dsun.java2d.d3d=false",
			"-Dsun.java2d.ddforcevram=true",
			"-Dsun.java2d.ddblit=false",
			"-Dswing.useflipBufferStrategy=true",
		)
	applicationDistribution.from("$rootDir") {
		include("README.md")
		include("NOTICE")
		include("LICENSE")
	}
}

tasks.jar {
	manifest {
		attributes(mapOf("Main-Class" to application.mainClass.get()))
	}
}

tasks.shadowJar {
	isZip64 = true
	mergeServiceFiles()
	manifest {
		from(tasks.jar.get().manifest)
	}
}

// workaround to exclude shadowJar 'all' artifact from publishing to maven
project.components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
	c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
		skip()
	}
}

tasks.startShadowScripts {
	doLast {
		val newWindowsScriptContent =
			windowsScript
				.readText()
				.replace("java.exe", "javaw.exe")
				.replace("\"%JAVA_EXE%\" %DEFAULT_JVM_OPTS%", "start \"jadx-gui\" /B \"%JAVA_EXE%\" %DEFAULT_JVM_OPTS%")
				// Prefer 8.3 short path when available so non-ASCII install paths still work (#2926)
				.replace(
					"set APP_HOME=%DIRNAME%..",
					"set APP_HOME=%DIRNAME%..\r\nfor %%i in (\"%APP_HOME%\") do set APP_HOME=%%~si",
				).replace(
					"for %%i in (\"%APP_HOME%\") do set APP_HOME=%%~fi",
					"for %%i in (\"%APP_HOME%\") do set APP_HOME=%%~si",
				)
		// Add launch script path as a property
		val newUnixScriptContent =
			unixScript
				.readText()
				.replace(
					Regex("DEFAULT_JVM_OPTS=.+", RegexOption.MULTILINE),
					{ result -> result.value + "\" \\\"-Djadx.launchScript.path=\$(realpath $0)\\\"\"" },
				)
		windowsScript.writeText(newWindowsScriptContent)
		unixScript.writeText(newUnixScriptContent)
	}
}

// Launch4j uses ANSI Windows APIs and fails when the install path contains non-ASCII
// characters (emoji, CJK, accented letters, …) — see #2926. These messages surface when
// the launcher itself can report an error; the JVM "JNI error" dialog cannot be customized.
val nonAsciiPathHint =
	"If the install path contains non-ASCII characters, use jadx-gui.cmd next to this exe, " +
		"the MSI installer, or move jadx to a path with only ASCII characters. See https://github.com/skylot/jadx/issues/2926"

launch4j {
	mainClassName.set(application.mainClass.get())
	copyConfigurable.set(listOf<Any>())
	dontWrapJar.set(true)
	icon.set("$projectDir/dist/windows/jadx-logo.ico")
	outfile.set("jadx-gui-$jadxVersion.exe")
	version.set(jadxVersion)
	copyright.set("Skylot")
	windowTitle.set("jadx")
	companyName.set("jadx")
	jreMinVersion.set("11")
	jvmOptions.set(escapeJVMOptions())
	requires64Bit.set(true)
	downloadUrl.set("https://www.oracle.com/java/technologies/downloads/#jdk21-windows")
	supportUrl.set("https://github.com/skylot/jadx")
	errTitle.set("jadx-gui")
	messagesStartupError.set("Failed to start jadx-gui. $nonAsciiPathHint")
	messagesLauncherError.set("jadx-gui launcher error. $nonAsciiPathHint")
	messagesJreNotFoundError.set(
		"Java 11+ (64-bit) was not found. Install a JDK/JRE or set JAVA_HOME. $nonAsciiPathHint",
	)

	bundledJrePath.set(if (project.hasProperty("bundleJRE")) "%EXEDIR%/jre" else "%JAVA_HOME%")
	classpath.set(
		tasks
			.getByName("shadowJar")
			.outputs.files
			.map { "%EXEDIR%/lib/${it.name}" }
			.sorted()
			.toList(),
	)

	chdir.set("") // don't change current dir
	libraryDir.set("") // don't add any libs
}

fun escapeJVMOptions(): List<String> =
	application.applicationDefaultJvmArgs
		.toList()
		.map { if (it.startsWith("-D")) "\"$it\"" else it }

/**
 * Build a Unicode-friendly Windows .cmd launcher for portable bundles.
 * Launch4j's .exe uses ANSI path APIs and cannot start from non-ASCII paths (#2926).
 * This script prefers the 8.3 short path when available, then starts javaw directly.
 */
fun windowsCmdLauncher(bundleJre: Boolean): String {
	val jvmOpts =
		application.applicationDefaultJvmArgs.joinToString(" ") { opt ->
			if (opt.startsWith("-D") || opt.contains(' ')) "\"$opt\"" else opt
		}
	val lines = mutableListOf<String>()
	lines += "@echo off"
	lines += "setlocal EnableExtensions"
	lines += "rem Unicode-safe launcher for paths with non-ASCII characters (issue #2926)."
	lines += "rem Prefer this over the .exe when the install folder name is not pure ASCII."
	lines += "set \"DIR=%~dp0\""
	lines += "if \"%DIR:~-1%\"==\"\\\" set \"DIR=%DIR:~0,-1%\""
	lines += "rem 8.3 short path keeps the JVM classpath ASCII when short names are enabled"
	lines += "for %%I in (\"%DIR%\") do set \"DIR=%%~sI\""
	lines += ""
	if (bundleJre) {
		lines += "set \"JAVA_EXE=%DIR%\\jre\\bin\\javaw.exe\""
		lines += "if not exist \"%JAVA_EXE%\" ("
		lines += "  echo ERROR: Bundled JRE not found:"
		lines += "  echo   %JAVA_EXE%"
		lines += "  pause"
		lines += "  exit /b 1"
		lines += ")"
	} else {
		lines += "if defined JAVA_HOME ("
		lines += "  set \"JAVA_EXE=%JAVA_HOME%\\bin\\javaw.exe\""
		lines += ") else ("
		lines += "  set \"JAVA_EXE=javaw.exe\""
		lines += ")"
	}
	lines += ""
	lines += "set \"CP=\""
	lines += "for %%J in (\"%DIR%\\lib\\*.jar\") do set \"CP=%%~fJ\""
	lines += "if not defined CP ("
	lines += "  echo ERROR: No jar found in \"%DIR%\\lib\""
	lines += "  echo If this path contains non-ASCII characters, move jadx to an ASCII-only folder"
	lines += "  echo or use the MSI installer from the GitHub releases / nightly builds."
	lines += "  pause"
	lines += "  exit /b 1"
	lines += ")"
	lines += ""
	lines += "start \"jadx-gui\" /B \"%JAVA_EXE%\" $jvmOpts -cp \"%CP%\" jadx.gui.JadxGUI %*"
	lines += ""
	return lines.joinToString("\n")
}

val writeWinCmdLauncher =
	tasks.register("writeWinCmdLauncher") {
		description = "Write Unicode-safe jadx-gui.cmd for Windows portable bundles"
		val outDir = layout.buildDirectory.dir("win-launcher")
		val outFile = outDir.map { it.file("jadx-gui.cmd") }
		val bundleJre = project.hasProperty("bundleJRE")
		outputs.file(outFile)
		doLast {
			val file = outFile.get().asFile
			file.parentFile.mkdirs()
			file.writeText(windowsCmdLauncher(bundleJre).replace("\n", "\r\n"))
		}
	}

runtime {
	addOptions("--strip-debug", "--no-header-files", "--no-man-pages")
	addModules(
		"java.desktop",
		"java.naming",
		"java.xml",
		// needed for "https" protocol to download plugins and updates
		"jdk.crypto.cryptoki",
		"jdk.accessibility",
	)
	jpackage {
		val os = DefaultNativePlatform.getCurrentOperatingSystem()
		if (os.isMacOsX) {
			imageName = "jadx-gui"
			val fileAssociations =
				fileTree("$projectDir/dist/macos/jpackage-file-associations") { include("*.properties") }
					.files
					.sortedBy { it.name }
					.flatMap { listOf("--file-associations", it.absolutePath) }
			imageOptions =
				listOf(
					"--icon",
					"$projectDir/dist/macos/jadx-logo.icns",
					"--mac-package-identifier",
					"io.github.skylot.jadx",
				) + fileAssociations
			// jpackage on macOS requires version as up to three integers separated by dots
			appVersion = if (jadxVersion.matches(Regex("\\d+(\\.\\d+){0,2}"))) jadxVersion else "1.0.0"
			installerType = "dmg"
			installerName = "jadx-gui"
			skipInstaller = false
		} else if (os.isWindows) {
			// WiX Toolset required
			appVersion = if (jadxVersion.matches(Regex("\\d+(\\.\\d+){0,2}"))) jadxVersion else "0.0.0"
			imageOptions = listOf("--icon", "$projectDir/dist/windows/jadx-logo.ico")
			skipInstaller = false
			installerType = "msi"
			installerOptions =
				listOf(
					"--win-menu",
					"--win-shortcut",
					"--win-dir-chooser",
					"--win-upgrade-uuid",
					"3d479468-383f-49fc-b374-53f64559dd9b",
				)
		} else if (os.isLinux) {
			appVersion = if (jadxVersion.matches(Regex("\\d+(\\.\\d+){0,2}"))) jadxVersion else "0.0.0"
			// TODO: setup linux packages, need to include jadx cli
		} else {
			throw RuntimeException("Unexpected OS: $os")
		}
	}
	launcher {
		noConsole = true
	}
}

val copyDistWin =
	tasks.register<Copy>("copyDistWin") {
		description = "Copy files for Windows bundle"

		val libTask = tasks.getByName("shadowJar")
		dependsOn(libTask)
		from(libTask.outputs) {
			include("*.jar")
			into("lib")
		}
		val exeTask = tasks.getByName("createExe")
		dependsOn(exeTask)
		from(exeTask.outputs) {
			include("*.exe")
		}
		dependsOn(writeWinCmdLauncher)
		from(writeWinCmdLauncher)
		into(layout.buildDirectory.dir("jadx-gui-win"))
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

val copyDistWinWithJre =
	tasks.register<Copy>("copyDistWinWithJre") {
		description = "Copy files for Windows with JRE bundle"

		val jreTask = tasks.runtime.get()
		dependsOn(jreTask)
		from(jreTask.jreDir) {
			include("**/*")
			into("jre")
		}
		val libTask = tasks.getByName("shadowJar")
		dependsOn(libTask)
		from(libTask.outputs) {
			include("*.jar")
			into("lib")
		}
		val exeTask = tasks.getByName("createExe")
		dependsOn(exeTask)
		from(exeTask.outputs) {
			include("*.exe")
		}
		dependsOn(writeWinCmdLauncher)
		from(writeWinCmdLauncher)
		into(layout.buildDirectory.dir("jadx-gui-with-jre-win"))
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

val copyDistMac =
	tasks.register<Copy>("copyDistMac") {
		description = "Copy dmg file for macOS bundle"

		val jpackageTask = tasks.getByName("jpackage")
		dependsOn(jpackageTask)
		from(layout.buildDirectory.dir("jpackage")) {
			include("*.dmg")
		}
		rename(
			"(.*)\\.dmg",
			"jadx-gui-$jadxVersion-mac-${System.getProperty("os.arch")}.dmg",
		)
		into(layout.buildDirectory.dir("jadx-gui-mac"))
	}

/**
 * Register and expose distribution artifacts to use in top level packaging tasks
 */
val distWinConfiguration =
	configurations.create("distWinConfiguration") {
		isCanBeResolved = false
	}
val distWinWithJreConfiguration =
	configurations.create("distWinWithJreConfiguration") {
		isCanBeResolved = false
	}
val distMacConfiguration =
	configurations.create("distMacConfiguration") {
		isCanBeResolved = false
	}
artifacts {
	add(distWinConfiguration.name, copyDistWin)
	add(distWinWithJreConfiguration.name, copyDistWinWithJre)
	add(distMacConfiguration.name, copyDistMac)
}

val syncNLSLines =
	tasks.register<JavaExec>("syncNLSLines") {
		group = "jadx-dev"
		description = "Utility task to sync new/missing translation using EN as a reference"

		classpath = sourceSets.main.get().runtimeClasspath
		mainClass.set("jadx.gui.utils.tools.SyncNLSLines")
	}
