plugins {
	id("jadx-kotlin")
	id("application")
	id("edu.sc.seis.launch4j") version "3.0.3"
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("org.beryx.runtime") version "1.13.0"
}

dependencies {
	implementation(project(":jadx-core"))
	implementation(project(":jadx-cli"))
	implementation(project(":jadx-plugins-tools"))

	// import mappings
	implementation(project(":jadx-plugins:jadx-rename-mappings"))

	// jadx-script autocomplete support
	implementation(project(":jadx-plugins:jadx-script:jadx-script-ide"))
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))
	implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.0")
	implementation("com.fifesoft:autocomplete:3.3.1")

	// use KtLint for format and check jadx scripts
	implementation("com.pinterest.ktlint:ktlint-rule-engine:0.50.0")
	implementation("com.pinterest.ktlint:ktlint-ruleset-standard:0.50.0")

	implementation("com.beust:jcommander:1.82")
	implementation("ch.qos.logback:logback-classic:1.4.8")
	implementation("dev.dirs:directories:26")

	implementation("com.fifesoft:rsyntaxtextarea:3.3.3")
	implementation(files("libs/jfontchooser-1.0.5.jar"))
	implementation("hu.kazocsaba:image-viewer:1.2.3")

	implementation("com.formdev:flatlaf:3.1.1")
	implementation("com.formdev:flatlaf-intellij-themes:3.1.1")
	implementation("com.formdev:flatlaf-extras:3.1.1")
	implementation("com.formdev:svgSalamander:1.1.4")

	implementation("com.google.code.gson:gson:2.10.1")
	implementation("org.apache.commons:commons-lang3:3.12.0")
	implementation("org.apache.commons:commons-text:1.10.0")
	implementation("commons-io:commons-io:2.13.0")

	implementation("io.reactivex.rxjava2:rxjava:2.2.21")
	implementation("com.github.akarnokd:rxjava2-swing:0.3.7")
	implementation("com.android.tools.build:apksig:8.1.0")
	implementation("io.github.skylot:jdwp:2.0.0")

	testImplementation(project(":jadx-core").dependencyProject.sourceSets.getByName("test").output)
}

val jadxVersion: String by rootProject.extra

tasks.test {
	exclude("**/tmp/*")
}

application {
	applicationName = ("jadx-gui")
	mainClass.set("jadx.gui.JadxGUI")
	applicationDefaultJvmArgs = listOf(
		"-Xms128M",
		"-XX:MaxRAMPercentage=70.0",
		"-Dawt.useSystemAAFontSettings=lcd",
		"-Dswing.aatext=true",
		"-Djava.util.Arrays.useLegacyMergeSort=true",
		"-Djdk.util.zip.disableZip64ExtraFieldValidation=true", // disable zip checks (#1962)
		"-XX:+IgnoreUnrecognizedVMOptions",
		"--add-opens=java.base/java.lang=ALL-UNNAMED", // for ktlint formatter
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
	mergeServiceFiles()
	manifest {
		from(project.tasks.jar.get().manifest)
	}
}

tasks.existing(CreateStartScripts::class) {
	doLast {
		val newContent = windowsScript.readText()
			.replace("java.exe", "javaw.exe")
			.replace("\"%JAVA_EXE%\" %DEFAULT_JVM_OPTS%", "start \"jadx-gui\" /B \"%JAVA_EXE%\" %DEFAULT_JVM_OPTS%")
		windowsScript.writeText(newContent)
	}
}

launch4j {
	mainClassName = application.mainClass.get()
	copyConfigurable.set(listOf<Any>())
	setJarTask(tasks.shadowJar.get())
	icon = "$projectDir/src/main/resources/logos/jadx-logo.ico"
	outfile = "jadx-gui-$jadxVersion.exe"
	copyright = "Skylot"
	windowTitle = "jadx"
	companyName = "jadx"
	jreMinVersion = "11"
	jvmOptions = application.applicationDefaultJvmArgs.toSet()
	requires64Bit = true
	initialHeapPercent = 5
	maxHeapSize = 4096
	maxHeapPercent = 70
	downloadUrl = "https://www.oracle.com/java/technologies/downloads/#jdk17-windows"
	bundledJrePath = if (project.hasProperty("bundleJRE")) "%EXEDIR%/jre" else "%JAVA_HOME%"
}

runtime {
	addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
	addModules(
		"java.desktop",
		"java.naming",
		"java.xml",
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

val copyDistWinWithJre by tasks.registering(Copy::class) {
	group = "jadx"
	dependsOn(tasks.named("runtime"), tasks.named("createExe"))
	from(runtime.jreDir) {
		include("**/*")
		into("jre")
	}
	from(tasks.named("createExe").get().outputs) {
		include("*.exe")
	}
	into(File(buildDir, "jadx-gui-$jadxVersion-with-jre-win"))
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val distWinWithJre by tasks.registering(Zip::class) {
	group = "jadx"
	dependsOn(copyDistWinWithJre)
	archiveFileName.set("jadx-gui-$jadxVersion-with-jre-win.zip")
	from(copyDistWinWithJre.get().outputs) {
		include("**/*")
	}
	into(buildDir)
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val addNewNLSLines by tasks.registering(JavaExec::class) {
	group = "jadx"
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("jadx.gui.utils.tools.NLSAddNewLines")
}
