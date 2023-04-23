import jadx.api.plugins.events.JadxEvents

/**
 * Log events
 */

val jadx = getJadxInstance()

jadx.events.addListener(JadxEvents.NODE_RENAMED_BY_USER) { rename ->
	log.info { "Rename from '${rename.oldName}' to '${rename.newName}' for node ${rename.node}" }
}
