/**
 *  Add menu action (into 'Plugins' section)
 */

val jadx = getJadxInstance()

jadx.gui.ifAvailable {
	addMenuAction("Decompile All") {
		jadx.decompile.allThreaded()
	}
}
