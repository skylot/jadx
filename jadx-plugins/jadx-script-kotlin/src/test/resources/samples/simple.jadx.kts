val jadx = getJadxInstance()

jadx.afterLoad {
	jadx.log.info { "Hello" }
}
