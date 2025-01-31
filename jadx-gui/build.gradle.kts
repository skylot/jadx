plugins {
	id("jadx-kotlin")
	id("application")
	id("jadx-library")
	id("edu.sc.seis.launch4j") version "3.0.6"
	id("org.beryx.runtime") version "1.13.1"
}

dependencies {
	implementation(project(":jadx-core"))
	implementation(project(":jadx-cli"))
	implementation(project(":jadx-plugins-tools"))
	implementation(project(":jadx-commons:jadx-app-commons"))

	// import mappings
	implementation(project(":jadx-plugins:jadx-rename-mappings"))

	// jadx-script autocomplete support
	implementation(project(":jadx-plugins:jadx-script:jadx-script-ide"))
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))
	implementation(kotlin("scripting-common"))
	implementation("com.fifesoft:autocomplete:3.3.1")

	// use KtLint for format and check jadx scripts
	implementation("com.pinterest.ktlint:ktlint-rule-engine:1.5.0")
	implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.5.0")

	implementation("org.jcommander:jcommander:2.0")
	implementation("ch.qos.logback:logback-classic:1.5.16")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

	implementation("com.fifesoft:rsyntaxtextarea:3.5.3")
	implementation("org.drjekyll:fontchooser:3.1.0")
	implementation("hu.kazocsaba:image-viewer:1.2.3")
	implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0") // WebP support for image viewer

	implementation("com.formdev:flatlaf:3.5.4")
	implementation("com.formdev:flatlaf-intellij-themes:3.5.4")
	implementation("com.formdev:flatlaf-extras:3.5.4")

	implementation("com.google.code.gson:gson:2.11.0")
	implementation("org.apache.commons:commons-lang3:3.17.0")
	implementation("org.apache.commons:commons-text:1.13.0")
	implementation("commons-io:commons-io:2.18.0")

	implementation("io.reactivex.rxjava2:rxjava:2.2.21")
	implementation("com.github.akarnokd:rxjava2-swing:0.3.7")
	implementation("com.android.tools.build:apksig:8.8.0")
	implementation("io.github.skylot:jdwp:2.0.0")

	testImplementation(project.project(":jadx-core").sourceSets.getByName("test").output)
}

val jadxVersion: String by rootProject.extra

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
			// flags to fix UI ghosting (#2225)
			"-Dsun.java2d.noddraw=true",
			"-Dsun.java2d.d3d=false",
			"-Dsun.java2d.ddforcevram=true",
			"-Dsun.java2d.ddblit=false",
			"-Dswing.useflipBufferStrategy=True",
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

tasks.startScripts {
	classpath = files("lib/*")
	doLast {
		val newContent =
			windowsScript.readText()
				.replace("java.exe", "javaw.exe")
				.replace("\"%JAVA_EXE%\" %DEFAULT_JVM_OPTS%", "start \"jadx-gui\" /B \"%JAVA_EXE%\" %DEFAULT_JVM_OPTS%")
		windowsScript.writeText(newContent)
	}
}

launch4j {
	mainClassName.set(application.mainClass.get())
	copyConfigurable.set(listOf<Any>())
	dontWrapJar.set(true)
	icon.set("$projectDir/src/main/resources/logos/jadx-logo.ico")
	outfile.set("jadx-gui-$jadxVersion.exe")
	copyright.set("Skylot")
	windowTitle.set("jadx")
	companyName.set("jadx")
	jreMinVersion.set("11")
	chdir.set("")
	jvmOptions.set(application.applicationDefaultJvmArgs.toSet())
	requires64Bit.set(true)
	downloadUrl.set("https://www.oracle.com/java/technologies/downloads/#jdk21-windows")
	bundledJrePath.set(if (project.hasProperty("bundleJRE")) "%EXEDIR%/jre" else "%JAVA_HOME%")
	classpath.set(listOf("%EXEDIR%/lib/*"))
}

runtime {
	addOptions("--strip-debug", "--compress", "zip-9", "--no-header-files", "--no-man-pages")
	addModules(
		"java.desktop",
		"java.naming",
		"java.xml",
		// needed for "https" protocol to download plugins and updates
		"jdk.crypto.cryptoki",
	)
	jpackage {
		imageOptions = listOf("--icon", "$projectDir/src/main/resources/logos/jadx-logo.ico")
		skipInstaller = true
		targetPlatformName = "win"
	}
	launcher {
		noConsole = true
	}
}

val copyDistWin by tasks.registering(Copy::class) {
	description = "Copy files for Windows bundle"

	val distTask = tasks.getByName("installDist")
	dependsOn(distTask)
	from(distTask.outputs) {
		include("**/lib/*.jar")
		includeEmptyDirs = false
	}
	val exeTask = tasks.getByName("createExe")
	dependsOn(exeTask)
	from(exeTask.outputs) {
		include("*.exe")
	}
	into(layout.buildDirectory.dir("jadx-gui-win"))
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val copyDistWinWithJre by tasks.registering(Copy::class) {
	description = "Copy files for Windows with JRE bundle"

	val jreTask = tasks.runtime.get()
	dependsOn(jreTask)
	from(jreTask.jreDir) {
		include("**/*")
		into("jre")
	}
	val distTask = tasks.getByName("installDist")
	dependsOn(distTask)
	from(distTask.outputs) {
		include("**/lib/*.jar")
		includeEmptyDirs = false
	}
	val exeTask = tasks.getByName("createExe")
	dependsOn(exeTask)
	from(exeTask.outputs) {
		include("*.exe")
	}
	into(layout.buildDirectory.dir("jadx-gui-with-jre-win"))
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val addNewNLSLines by tasks.registering(JavaExec::class) {
	group = "jadx-dev"
	description = "Utility task to add new/missing translation lines"

	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("jadx.gui.utils.tools.NLSAddNewLines")
}
