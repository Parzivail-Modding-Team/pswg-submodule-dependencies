plugins {
	`kotlin-dsl`
	`maven-publish`
	id("com.gradleup.nmcp") version "1.4.4"
	id("com.gradleup.nmcp.aggregation") version "1.4.4"
	id("org.jetbrains.dokka") version "2.2.0-Beta"
	signing
}

group = "com.parzivail.internal"
version = "0.3"

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
	from(tasks.named("dokkaGenerate"))
}

tasks.withType<Jar> {
	from(file("LICENSE")) {
		rename { "LICENSE_${project.name}" }
	}
}

gradlePlugin {
	plugins {
		all {
			displayName = "Fabric Loom subproject dependencies"
			description = "Gradle plugin to forward dependencies of Fabric Loom subprojects to their dependents"
		}
	}
}

publishing {
	publications {
		withType<MavenPublication> {
			pom {
				name.set("Fabric Loom subproject dependencies")
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
					developer {
						id.set("parzivail")
						name.set("parzivail")
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
	implementation("net.fabricmc:fabric-loom:1.15.5")
	nmcpAggregation(project(path))
}

signing {
	useGpgCmd()
	sign(publishing.publications)
}

nmcpAggregation {
	centralPortal {
		username = (findProperty("ossrhUsername") as String?) ?: ""
		password = (findProperty("ossrhPassword") as String?) ?: ""
		publishingType = "AUTOMATIC"
		publicationName = "${project.group}:${project.name}:${project.version}"
	}
}
