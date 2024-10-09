package jadx.api.security;

import java.util.EnumSet;
import java.util.Set;

public enum JadxSecurityFlag {

	VERIFY_APP_PACKAGE,
	SECURE_XML_PARSER;

	public static Set<JadxSecurityFlag> all() {
		return EnumSet.allOf(JadxSecurityFlag.class);
	}

	public static Set<JadxSecurityFlag> none() {
		return EnumSet.noneOf(JadxSecurityFlag.class);
	}
}
