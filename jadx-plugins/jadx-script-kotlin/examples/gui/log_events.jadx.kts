/**
 * Log events
 */

import jadx.api.plugins.events.JadxEvents

val jadx = getJadxInstance()

jadx.gui.ifAvailable {
	// GUI only events

	jadx.events.addListener(JadxEvents.NODE_RENAMED_BY_USER) { rename ->
		log.info { "Rename from '${rename.oldName}' to '${rename.newName}' for node ${rename.node}" }
	}

	jadx.events.addListener(JadxEvents.RELOAD_PROJECT) {
		log.info { "Project reloaded" }
	}

	jadx.events.addListener(JadxEvents.RELOAD_SETTINGS_WINDOW) {
		log.info { "Settings window reloaded" }
	}
}
