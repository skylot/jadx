package jadx.core.dex.attributes.nodes;

import java.util.Objects;

public class JadxWarn {

	private final String warn;

	public JadxWarn(String warn) {
		this.warn = Objects.requireNonNull(warn);
	}

	public String getWarn() {
		return warn;
	}

	@Override
	public String toString() {
		return "JadxWarn: " + warn;
	}
}
