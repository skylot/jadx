package jadx.plugins.kotlin.metadata.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.LOG: Logger get() = LoggerFactory.getLogger(T::class.java)

inline fun <reified T : Any, R> T.runCatchingLog(msg: String? = null, block: () -> R) =
	runCatching(block)
		.onFailure { LOG.error(msg.orEmpty(), it) }
