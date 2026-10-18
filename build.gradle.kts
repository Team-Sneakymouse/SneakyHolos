import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm") version "2.4.10"
	id("io.papermc.paperweight.userdev") version "2.0.0-beta.22"
	`maven-publish`
}

group = "io.github.team-sneakymouse"

version = providers.exec {
	workingDir(projectDir)
	commandLine("git", "show", "-s", "--format=%ct:%h", "--abbrev=12", "HEAD")
}.standardOutput.asText.map { commit ->
	val (timestamp, hash) = commit.trim().split(":", limit = 2)
	val date = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC)
		.format(Instant.ofEpochSecond(timestamp.toLong()))
	"$date-$hash"
}.get()

base {
	archivesName.set("SneakyHolos")
}

repositories {
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/")
	maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
	maven("https://maven.sneakyrp.com/releases")
}

dependencies {
	paperweight.paperDevBundle("26.2.build.+")
	compileOnly("me.clip:placeholderapi:2.11.6")
	compileOnly("io.github.team-sneakymouse:sneakyutils:2026.10.18-4b5831b0b997")
	implementation(kotlin("stdlib"))
	implementation("com.google.code.gson:gson:2.10.1")
}

kotlin {
	jvmToolchain(25)
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_25)
	}
}

tasks.processResources {
	inputs.property("version", project.version.toString())
	filesMatching(listOf("paper-plugin.yml", "plugin.yml")) {
		expand("version" to project.version.toString())
	}
}

tasks.jar {
	archiveBaseName.set("SneakyHolos")
	archiveFileName.set("SneakyHolos-${project.version}.jar")
}

// Only publish when this repo is the Gradle root (not when included as a sibling dependency).
if (project == rootProject) {
	publishing {
		publications {
			create<MavenPublication>("maven") {
				artifactId = "sneakyholos"
				from(components["java"])
				pom {
					name.set("SneakyHolos")
					description.set("High-performance holographic UI library for Paper (NMS displays and zero-raycast click detection).")
					url.set("https://github.com/Team-Sneakymouse/SneakyHolos")
					scm {
						url.set("https://github.com/Team-Sneakymouse/SneakyHolos")
						connection.set("scm:git:https://github.com/Team-Sneakymouse/SneakyHolos.git")
					}
				}
			}
		}
		repositories {
			maven {
				name = "sneakyrp"
				url = uri("https://maven.sneakyrp.com/releases")
				credentials(PasswordCredentials::class)
				authentication {
					create<org.gradle.authentication.http.BasicAuthentication>("basic")
				}
			}
		}
	}

	val checkMavenCoordinates by tasks.registering {
		group = "verification"
		description = "Checks that the Maven artifact ID matches the library name."
		doLast {
			val publication = publishing.publications.getByName("maven") as MavenPublication
			check(publication.artifactId == "sneakyholos") {
				"Expected Maven artifact ID sneakyholos, got ${publication.artifactId}"
			}
		}
	}

	tasks.check {
		dependsOn(checkMavenCoordinates)
	}

	tasks.withType<PublishToMavenRepository>().configureEach {
		dependsOn(tasks.check)
	}
}
