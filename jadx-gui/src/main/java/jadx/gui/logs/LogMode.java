package jadx.gui.logs;

import org.apache.commons.lang3.StringUtils;

import jadx.gui.utils.NLS;

public enum LogMode {
	ALL,
	ALL_SCRIPTS,
	CURRENT_SCRIPT;

	private static final String[] NLS_STRINGS = StringUtils.split(NLS.str("log_viewer.modes"), '|');

	public String getLocalizedName() {
		return NLS_STRINGS[this.ordinal()];
	}

	@Override
	public String toString() {
		return getLocalizedName();
	}
}
