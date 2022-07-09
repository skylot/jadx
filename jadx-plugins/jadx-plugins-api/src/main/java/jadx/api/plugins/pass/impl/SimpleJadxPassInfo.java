package jadx.api.plugins.pass.impl;

import java.util.Collections;
import java.util.List;

import jadx.api.plugins.pass.JadxPassInfo;

public class SimpleJadxPassInfo implements JadxPassInfo {

	private final String name;
	private final String desc;

	public SimpleJadxPassInfo(String name) {
		this(name, name);
	}

	public SimpleJadxPassInfo(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return desc;
	}

	@Override
	public List<String> runAfter() {
		return Collections.emptyList();
	}

	@Override
	public List<String> runBefore() {
		return Collections.emptyList();
	}
}
