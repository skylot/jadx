package jadx.core.dex.attributes.nodes;

import java.util.Objects;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;

public class RegDebugInfoAttr implements IJadxAttribute {

	private final ArgType type;
	private final String name;

	public RegDebugInfoAttr(ArgType type, String name) {
		this.type = type;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public ArgType getRegType() {
		return type;
	}

	@Override
	public AType<RegDebugInfoAttr> getAttrType() {
		return AType.REG_DEBUG_INFO;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RegDebugInfoAttr that = (RegDebugInfoAttr) o;
		return Objects.equals(type, that.type) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, name);
	}

	@Override
	public String toString() {
		return "D('" + name + "' " + type + ')';
	}
}
