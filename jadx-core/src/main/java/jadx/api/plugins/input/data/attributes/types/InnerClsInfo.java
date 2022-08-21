package jadx.api.plugins.input.data.attributes.types;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.AccessFlagsScope;

public class InnerClsInfo {
	private final String innerCls;
	private final @Nullable String outerCls;
	private final @Nullable String name;
	private final int accessFlags;

	public InnerClsInfo(String innerCls, @Nullable String outerCls, @Nullable String name, int accessFlags) {
		this.innerCls = innerCls;
		this.outerCls = outerCls;
		this.name = name;
		this.accessFlags = accessFlags;
	}

	public String getInnerCls() {
		return innerCls;
	}

	public @Nullable String getOuterCls() {
		return outerCls;
	}

	public @Nullable String getName() {
		return name;
	}

	public int getAccessFlags() {
		return accessFlags;
	}

	@Override
	public String toString() {
		return "InnerCls{" + innerCls
				+ ", outerCls=" + outerCls
				+ ", name=" + name
				+ ", accessFlags=" + AccessFlags.format(accessFlags, AccessFlagsScope.CLASS)
				+ '}';
	}
}
