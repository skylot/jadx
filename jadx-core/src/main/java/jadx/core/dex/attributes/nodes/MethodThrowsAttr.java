package jadx.core.dex.attributes.nodes;

import java.util.Set;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;
import jadx.core.dex.attributes.AType;

public class MethodThrowsAttr extends PinnedAttribute {
	private final Set<String> list;

	private boolean visited;

	public MethodThrowsAttr(Set<String> list) {
		this.list = list;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public Set<String> getList() {
		return list;
	}

	@Override
	public IJadxAttrType<MethodThrowsAttr> getAttrType() {
		return AType.METHOD_THROWS;
	}

	@Override
	public String toString() {
		return "THROWS:" + list;
	}

}
