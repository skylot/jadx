package jadx.core.dex.attributes;

import jadx.core.utils.InsnUtils;

public class JumpAttribute implements IAttribute {

	private final int src;
	private final int dest;

	public JumpAttribute(int src, int dest) {
		this.src = src;
		this.dest = dest;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.JUMP;
	}

	public int getSrc() {
		return src;
	}

	public int getDest() {
		return dest;
	}

	@Override
	public String toString() {
		return "JUMP: " + InsnUtils.formatOffset(src) + " -> " + InsnUtils.formatOffset(dest);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dest;
		result = prime * result + src;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JumpAttribute other = (JumpAttribute) obj;
		return dest == other.dest && src == other.src;
	}
}
