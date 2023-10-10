plugins {
	id("jadx-java")
	id("application")

	// use shadow only for application scripts, jar will be copied from jadx-gui
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
	implementation(project(":jadx-core"))
	implementation(project(":jadx-plugins-tools"))

	runtimeOnly(project(":jadx-plugins:jadx-dex-input"))
	runtimeOnly(project(":jadx-plugins:jadx-java-input"))
	runtimeOnly(project(":jadx-plugins:jadx-java-convert"))
	runtimeOnly(project(":jadx-plugins:jadx-smali-input"))
	runtimeOnly(project(":jadx-plugins:jadx-rename-mappings"))
	runtimeOnly(project(":jadx-plugins:jadx-kotlin-metadata"))
	runtimeOnly(project(":jadx-plugins:jadx-script:jadx-script-plugin"))

	implementation("com.beust:jcommander:1.82")
	implementation("ch.qos.logback:logback-classic:1.4.11")
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
