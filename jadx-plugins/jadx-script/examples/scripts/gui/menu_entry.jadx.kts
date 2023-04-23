// customize jadx-gui

val jadx = getJadxInstance()

jadx.gui.ifAvailable {
	addMenuAction("Decompile All") {
		jadx.decompile.all()
	}
}
