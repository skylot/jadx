package jadx.core.dex.attributes.nodes;

import jadx.core.utils.InsnUtils;

public class JumpInfo {

	private final int src;
	private final int dest;

	public JumpInfo(int src, int dest) {
		this.src = src;
		this.dest = dest;
	}

	public int getSrc() {
		return src;
	}

	public int getDest() {
		return dest;
	}

	@Override
	public int hashCode() {
		return 31 * dest + src;
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
		JumpInfo other = (JumpInfo) obj;
		return dest == other.dest && src == other.src;
	}

	@Override
	public String toString() {
		return "JUMP: " + InsnUtils.formatOffset(src) + " -> " + InsnUtils.formatOffset(dest);
	}
}
