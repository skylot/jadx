plugins {
	id("jadx-java")
	id("java-library")
	id("maven-publish")
	id("signing")
}

val jadxVersion: String by rootProject.extra

group = "io.github.skylot"
version = jadxVersion

java {
	withJavadocJar()
	withSourcesJar()
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = project.name
			from(components["java"])
			versionMapping {
				usage("java-api") {
					fromResolutionOf("runtimeClasspath")
				}
				usage("java-runtime") {
					fromResolutionResult()
				}
			}
			pom {
				name.set(project.name)
				description.set("Dex to Java decompiler")
				url.set("https://github.com/skylot/jadx")
				licenses {
					license {
						name.set("The Apache License, Version 2.0")
						url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
					}
				}
				developers {
					developer {
						id.set("skylot")
						name.set("Skylot")
						email.set(project.properties["libEmail"].toString())
						url.set("https://github.com/skylot")
					}
				}
				scm {
					connection .set("scm:git:git://github.com/skylot/jadx.git")
					developerConnection.set("scm:git:ssh://github.com:skylot/jadx.git")
					url .set("https://github.com/skylot/jadx")
				}
			}
		}
	}
	repositories {
		maven {
			val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
			val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
			url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
			credentials {
				username = project.properties["ossrhUser"].toString()
				password = project.properties["ossrhPassword"].toString()
			}
		}
	}
}

signing {
	isRequired = gradle.taskGraph.hasTask("publish")
	sign(publishing.publications["mavenJava"])
}


tasks.javadoc {
	val stdOptions = options as StandardJavadocDocletOptions
	stdOptions.addBooleanOption("html5", true)
	// disable 'missing' warnings
	stdOptions.addStringOption("Xdoclint:all,-missing", "-quiet")
}
