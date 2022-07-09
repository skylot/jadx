/**
 * Utils for use in scripts.
 * Located in default package to reduce imports.
 */

import java.io.File

fun String.asFile(): File = File(this)

fun Int.hex(): String = Integer.toHexString(this)
