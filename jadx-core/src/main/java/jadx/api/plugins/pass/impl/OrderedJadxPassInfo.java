package jadx.api.plugins.pass.impl;

import java.util.ArrayList;
import java.util.List;

import jadx.api.plugins.pass.JadxPassInfo;

public class OrderedJadxPassInfo implements JadxPassInfo {

	private final String name;
	private final String desc;
	private final List<String> runAfter;
	private final List<String> runBefore;

	public OrderedJadxPassInfo(String name, String desc) {
		this(name, desc, new ArrayList<>(), new ArrayList<>());
	}

	public OrderedJadxPassInfo(String name, String desc, List<String> runAfter, List<String> runBefore) {
		this.name = name;
		this.desc = desc;
		this.runAfter = runAfter;
		this.runBefore = runBefore;
	}

	public OrderedJadxPassInfo after(String pass) {
		runAfter.add(pass);
		return this;
	}

	public OrderedJadxPassInfo before(String pass) {
		runBefore.add(pass);
		return this;
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
		return runAfter;
	}

	@Override
	public List<String> runBefore() {
		return runBefore;
	}
}
