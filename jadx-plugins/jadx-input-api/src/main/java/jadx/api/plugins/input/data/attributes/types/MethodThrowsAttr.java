package jadx.api.plugins.input.data.attributes.types;

import java.util.Set;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class MethodThrowsAttr extends PinnedAttribute {
	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return JadxAttrType.METHOD_THROWS;
	}

	private final Set<String> list;

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	private boolean visited;

	public MethodThrowsAttr(Set<String> list) {
		this.list = list;
	}

	public Set<String> getList() {
		return list;
	}

	@Override
	public String toString() {
		return "EXCEPTIONS:" + list;
	}

}
