plugins {
	id("jadx-kotlin")
}

dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	implementation(kotlin("stdlib-common"))
	implementation(kotlin("script-runtime"))

	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

	// script context support in IDE is poor, use stubs and manual imports for now
	// kotlinScriptDef(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	// manual imports (IDE can't import dependencies by scripts annotations)
	implementation("com.github.javafaker:javafaker:1.0.2")
	implementation("org.apache.commons:commons-text:1.13.0")
}

sourceSets {
	main {
		kotlin.srcDirs(
			"scripts",
			"scripts/deobf",
			"scripts/gui",
			"context",
		)
	}
}
