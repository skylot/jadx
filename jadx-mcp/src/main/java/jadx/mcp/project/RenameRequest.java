package jadx.mcp.project;

import jadx.api.JavaNode;

/**
 * Validated rename request. Node can be: class, method, field, package or variable.
 */
public final class RenameRequest {
	private final JavaNode node;
	private final String newName;
	private final boolean reset;

	public RenameRequest(JavaNode node, String newName, boolean reset) {
		this.node = node;
		this.newName = newName;
		this.reset = reset;
	}

	public JavaNode getNode() {
		return node;
	}

	/**
	 * New alias, empty for reset
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 * Remove user rename and restore original (or deobfuscated) name
	 */
	public boolean isReset() {
		return reset;
	}
}
