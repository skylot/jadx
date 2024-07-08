plugins {
	id("jadx-kotlin")
	id("application")
	id("edu.sc.seis.launch4j") version "3.0.5"
	id("com.github.johnrengelman.shadow") version "8.1.1"
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
	implementation("com.pinterest.ktlint:ktlint-rule-engine:1.2.1")
	implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.2.1")

	implementation("org.jcommander:jcommander:1.83")
	implementation("ch.qos.logback:logback-classic:1.5.6")

	implementation("com.fifesoft:rsyntaxtextarea:3.4.0")
	implementation(files("libs/jfontchooser-1.0.5.jar"))
	implementation("hu.kazocsaba:image-viewer:1.2.3")

	implementation("com.formdev:flatlaf:3.4.1")
	implementation("com.formdev:flatlaf-intellij-themes:3.4.1")
	implementation("com.formdev:flatlaf-extras:3.4.1")

	implementation("com.google.code.gson:gson:2.10.1")
	implementation("org.apache.commons:commons-lang3:3.14.0")
	implementation("org.apache.commons:commons-text:1.12.0")
	implementation("commons-io:commons-io:2.16.1")

	implementation("io.reactivex.rxjava2:rxjava:2.2.21")
	implementation("com.github.akarnokd:rxjava2-swing:0.3.7")
	implementation("com.android.tools.build:apksig:8.3.2")
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

tasks.existing(CreateStartScripts::class) {
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
	initialHeapPercent.set(5)
	maxHeapSize.set(4096)
	maxHeapPercent.set(70)
	downloadUrl.set("https://www.oracle.com/java/technologies/downloads/#jdk21-windows")
	bundledJrePath.set(if (project.hasProperty("bundleJRE")) "%EXEDIR%/jre" else "%JAVA_HOME%")
	classpath.set(tasks.getByName("shadowJar").outputs.files.map { "%EXEDIR%/lib/${it.name}" }.toSortedSet())
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

val copyDistWin by tasks.registering(Copy::class) {
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
	into(layout.buildDirectory.dir("jadx-gui-with-jre-win"))
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val addNewNLSLines by tasks.registering(JavaExec::class) {
	group = "jadx-dev"
	description = "Utility task to add new/missing translation lines"

	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("jadx.gui.utils.tools.NLSAddNewLines")
}
