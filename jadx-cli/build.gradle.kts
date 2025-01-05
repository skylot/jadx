plugins {
	id("jadx-java")
	id("jadx-library")
	id("application")

	// use shadow only for application scripts, jar will be copied from jadx-gui
	id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
	implementation(project(":jadx-core"))
	implementation(project(":jadx-plugins-tools"))
	implementation(project(":jadx-commons:jadx-app-commons"))

	runtimeOnly(project(":jadx-plugins:jadx-dex-input"))
	runtimeOnly(project(":jadx-plugins:jadx-java-input"))
	runtimeOnly(project(":jadx-plugins:jadx-java-convert"))
	runtimeOnly(project(":jadx-plugins:jadx-smali-input"))
	runtimeOnly(project(":jadx-plugins:jadx-rename-mappings"))
	runtimeOnly(project(":jadx-plugins:jadx-kotlin-metadata"))
	runtimeOnly(project(":jadx-plugins:jadx-kotlin-source-debug-extension"))
	runtimeOnly(project(":jadx-plugins:jadx-script:jadx-script-plugin"))
	runtimeOnly(project(":jadx-plugins:jadx-xapk-input"))
	runtimeOnly(project(":jadx-plugins:jadx-aab-input"))
	runtimeOnly(project(":jadx-plugins:jadx-apkm-input"))

	implementation("org.jcommander:jcommander:2.0")
	implementation("ch.qos.logback:logback-classic:1.5.13")
}

application {
	applicationName = "jadx"
	mainClass.set("jadx.cli.JadxCLI")
	applicationDefaultJvmArgs =
		listOf(
			"-Xms256M",
			"-XX:MaxRAMPercentage=70.0",
			// disable zip checks (#1962)
			"-Djdk.util.zip.disableZip64ExtraFieldValidation=true",
		)
	applicationDistribution.from("$rootDir") {
		include("README.md")
		include("NOTICE")
		include("LICENSE")
	}
}

tasks.shadowJar {
	// shadow jar not needed
	configurations = listOf()
}
