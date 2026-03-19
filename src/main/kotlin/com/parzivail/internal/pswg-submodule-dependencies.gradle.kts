package com.parzivail.internal

import java.util.Collections
import java.util.EnumMap
import java.util.IdentityHashMap
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

plugins {
	`java-library`
}

enum class ConfigurationType(val loomConfig: String, val gradleConfig: String) {
	API("modApi", "api"),
	COMPILE_ONLY("modCompileOnly", "compileOnly"),
	COMPILE_ONLY_API("modCompileOnlyApi", "compileOnlyApi"),
	IMPLEMENTATION("modImplementation", "implementation"),
	RUNTIME_ONLY("modRuntimeOnly", "runtimeOnly"),
}

fun computeType(consumerType: ConfigurationType, producerType: ConfigurationType) =
	when (producerType) {
		ConfigurationType.API -> consumerType
		ConfigurationType.IMPLEMENTATION, ConfigurationType.RUNTIME_ONLY -> when (consumerType) {
			ConfigurationType.COMPILE_ONLY, ConfigurationType.COMPILE_ONLY_API -> null
			else -> ConfigurationType.RUNTIME_ONLY
		}

		ConfigurationType.COMPILE_ONLY_API -> when (consumerType) {
			ConfigurationType.API, ConfigurationType.COMPILE_ONLY_API -> ConfigurationType.COMPILE_ONLY_API
			ConfigurationType.COMPILE_ONLY, ConfigurationType.IMPLEMENTATION -> ConfigurationType.COMPILE_ONLY
			else -> null
		}

		else -> null
	}

val visitedProjects: Map<ConfigurationType, MutableSet<Project>> = ConfigurationType.values()
	.associateWithTo(EnumMap(ConfigurationType::class.java)) { Collections.newSetFromMap(IdentityHashMap()) }

fun resolveProject(dependency: ProjectDependency): Project =
	project.rootProject.findProject(dependency.path)
		?: error("Could not resolve project dependency path ${dependency.path}")

fun Project.findClientSourceSet(): SourceSet? =
	extensions.findByType(SourceSetContainer::class.java)?.findByName("client")

fun isSubmoduleProjectDependency(dependency: ProjectDependency): Boolean =
	dependency.targetConfiguration == null || dependency.targetConfiguration == "namedElements"

fun wireClientSourceSet(consumerProject: Project, producerProject: Project) {
	val consumerClient = consumerProject.findClientSourceSet() ?: return
	val producerClient = producerProject.findClientSourceSet() ?: return

	consumerClient.compileClasspath += producerClient.output
	consumerClient.runtimeClasspath += producerClient.output

	consumerProject.tasks.named(consumerClient.classesTaskName) {
		dependsOn(producerProject.tasks.named(producerClient.classesTaskName))
	}
}

fun importFrom(dependencyProject: Project, relation: ConfigurationType): Boolean {
	if (dependencyProject in visitedProjects[relation]!! && dependencyProject !== project) return false
	visitedProjects[relation]!!.add(dependencyProject)
	for (configType in ConfigurationType.values()) {
		val innerRelation = computeType(relation, configType) ?: continue

		for (dependency in dependencyProject.configurations.findByName(configType.gradleConfig)?.dependencies ?: setOf()) {
			if (dependency is ProjectDependency && isSubmoduleProjectDependency(dependency)) {
				val resolvedProject = resolveProject(dependency)
				if (!importFrom(resolvedProject, innerRelation)) {
					continue
				}
			}

			if (project.configurations.findByName(innerRelation.gradleConfig) != null) {
				project.dependencies.add(innerRelation.gradleConfig, dependency.copy())
			}
		}

		for (dependency in dependencyProject.configurations.findByName(configType.loomConfig)?.dependencies ?: setOf()) {
			if (project.configurations.findByName(innerRelation.loomConfig) != null) {
				project.dependencies.add(innerRelation.loomConfig, dependency.copy())
			}
		}
	}

	return true
}

afterEvaluate {
	if (project == project.rootProject) {
		return@afterEvaluate
	}

	for ((configType, dependencies) in ConfigurationType.values()
		.associateWithTo(EnumMap(ConfigurationType::class.java)) {
			project.configurations[it.gradleConfig]?.dependencies?.toSet() ?: setOf()
		}) {
		for (dependency in dependencies) {
			if (dependency is ProjectDependency && isSubmoduleProjectDependency(dependency)) {
				val dependencyProject = resolveProject(dependency)
				val processDependency = {
					wireClientSourceSet(project, dependencyProject)
					importFrom(dependencyProject, configType)
				}

				if (!dependencyProject.state.executed) {
					project.evaluationDependsOn(dependency.path)
				}

				processDependency()
			}
		}
	}

	tasks.classes {
		for (configType in ConfigurationType.values()) {
			dependsOn((project.configurations[configType.gradleConfig] ?: continue).getTaskDependencyFromProjectDependency(true, "classes"))
		}
	}
}

tasks.assemble {
	this.dependsOn()
}
