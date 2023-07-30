@file:Suppress("MayBeConstant", "unused")

import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.plugins.script.runtime.JadxScriptInstance

/**
 * Stubs for JadxScriptBaseClass script super class
 */

val log = KotlinLogging.logger("JadxScript")
val scriptName = "script"

fun getJadxInstance(): JadxScriptInstance {
	throw IllegalStateException("Stub method!")
}

/**
 * Annotations for maven imports
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOn(vararg val artifactsCoordinates: String, val options: Array<String> = [])

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Repository(vararg val repositoriesCoordinates: String, val options: Array<String> = [])
