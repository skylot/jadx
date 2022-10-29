dependencies {
	implementation(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
	implementation("org.jetbrains.kotlin:kotlin-script-runtime")

	implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")

	// script context support in IDE is poor, use stubs and manual imports for now
	// kotlinScriptDef(project(":jadx-plugins:jadx-script:jadx-script-runtime"))

	// manual imports (IDE can't import dependencies by scripts annotations)
	implementation("com.github.javafaker:javafaker:1.0.2")
}

sourceSets {
	main {
		kotlin.srcDirs(
			"scripts",
			"context"
		)
	}
}
