import jadx.api.metadata.ICodeNodeRef

val jadx = getJadxInstance()

jadx.gui.ifAvailable {
	addPopupMenuAction(
		"Print enclosing symbols under caret or mouse",
		enabled = { true },
		keyBinding = "G",
		action = ::runAction,
	)
}

fun runAction(node: ICodeNodeRef) {
	log.info { "Node under caret: ${jadx.gui.nodeUnderCaret}" }
	log.info { "Enclosing node under caret: ${jadx.gui.enclosingNodeUnderCaret}" }
	log.info { "Node under mouse: ${jadx.gui.nodeUnderMouse}" }
	log.info { "Enclosing Node under mouse: ${jadx.gui.enclosingNodeUnderMouse}" }
}
