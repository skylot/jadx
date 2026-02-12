@file:DependsOn("org.apache.commons:commons-text:1.10.0")

import org.apache.commons.text.StringEscapeUtils

val jadx = getJadxInstance()

jadx.afterLoad {
	jadx.classes.forEach {
		jadx.log.info { "Escaped name: ${StringEscapeUtils.escapeJava(it.fullName)}" }
	}
}
