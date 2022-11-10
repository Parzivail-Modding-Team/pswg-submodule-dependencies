plugins {
	`kotlin-dsl`
	`maven-publish`
	id("org.jetbrains.dokka") version "1.7.20"
}

group = "com.parzivail.internal"
version = "0.1"

repositories {
	// Use Maven Central for resolving dependencies.
	mavenCentral()
	maven(url = "https://maven.fabricmc.net") {
		name = "FabricMC"
	}
}

java {
	withSourcesJar()
	withJavadocJar()
}

tasks.named<Jar>("javadocJar") {
	from(tasks.named("dokkaJavadoc"))
}

tasks.withType<Jar> {
	from(file("LICENSE")) {
		rename { "LICENSE_${project.name}" }
	}
}

publishing {
	publications {
		withType<MavenPublication> {
			pom {
				description.set("Gradle plugin to forward dependencies of Fabric Loom subprojects to their dependents")
				url.set("https://github.com/Parzivail-Modding-Team/pswg-submodule-dependencies")
				licenses {
					license {
						name.set("Mozilla Public License 2.0")
						url.set("https://www.mozilla.org/en-US/MPL/2.0/")
					}
				}
				developers {
					developer {
						id.set("kb1000")
						name.set("kb1000")
					}
				}
				scm {
					url.set("https://github.com/Parzivail-Modding-Team/pswg-submodule-dependencies")
					connection.set("scm:git:https://github.com/Parzivail-Modding-Team/pswg-submodule-dependencies.git")
					developerConnection.set("scm:git:git@github.com:Parzivail-Modding-Team/pswg-submodule-dependencies.git")
				}
				withXml {
					asNode().appendNode("").withGroovyBuilder {
						"replaceNode" {
							"repositories" {
								"repository" {
									"url"("https://maven.fabricmc.net")
									"name"("FabricMC")
									"id"("fabricmc")
								}
							}
						}
					}
				}
			}
		}
	}
}

dependencies {
	implementation("net.fabricmc:fabric-loom:1.0-SNAPSHOT")
}
