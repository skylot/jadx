import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("java-library")

	kotlin("jvm")
}

version = System.getenv("JADX_SCRIPT_KOTLIN_PLUGIN_VERSION") ?: "dev"

dependencies {
	compileOnly(project(":jadx-core"))
	compileOnly(project(":jadx-commons:jadx-app-commons"))

	compileOnly(project(":jadx-gui"))

	implementation(kotlin("scripting-common"))
	implementation(kotlin("scripting-jvm"))
	implementation(kotlin("scripting-jvm-host"))
	implementation(kotlin("scripting-ide-services"))
	implementation(kotlin("scripting-compiler-embeddable"))
	implementation(kotlin("compiler-embeddable"))

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

	// allow to use maven dependencies in scripts
	implementation(kotlin("scripting-dependencies"))
	implementation(kotlin("scripting-dependencies-maven"))

	// autocomplete support in editor
	compileOnly("com.fifesoft:autocomplete:3.3.2")
	compileOnly("com.fifesoft:rsyntaxtextarea:3.6.0")

	// use KtLint for format and check jadx scripts
	implementation("com.pinterest.ktlint:ktlint-rule-engine:1.8.0")
	implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.8.0")

	compileOnly("io.github.oshai:kotlin-logging-jvm:7.0.13")
	compileOnly("org.slf4j:slf4j-api:2.0.17")

	// register jadx script for IDE support (don't work now)
	// kotlinScriptDef(project(":jadx-plugins:jadx-script-kotlin"))

	testImplementation(project(":jadx-core"))
	testRuntimeOnly(project(":jadx-plugins:jadx-dex-input"))
	testRuntimeOnly(project(":jadx-plugins:jadx-smali-input"))

	testImplementation("ch.qos.logback:logback-classic:1.5.22")
	testImplementation("org.assertj:assertj-core:3.27.6")

	testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

repositories {
	mavenLocal()
	mavenCentral()
	google()
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_11)
	}
}

tasks {
	register<Zip>("dist") {
		group = "jadx-plugin"
		dependsOn(jar)

		from(jar)
		from(project.configurations.runtimeClasspath)

		archiveBaseName = project.name
		destinationDirectory = layout.buildDirectory.dir("dist")
	}

	withType(Test::class) {
		useJUnitPlatform()
	}
}
