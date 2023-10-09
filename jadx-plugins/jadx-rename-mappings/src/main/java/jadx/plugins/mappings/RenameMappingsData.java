package jadx.plugins.mappings;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.tree.MappingTreeView;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.nodes.RootNode;

public class RenameMappingsData implements IJadxAttribute {

	private static final IJadxAttrType<RenameMappingsData> DATA = IJadxAttrType.create();

	public static @Nullable RenameMappingsData getData(RootNode root) {
		return root.getAttributes().get(DATA);
	}

	public static @Nullable MappingTreeView getTree(RootNode root) {
		RenameMappingsData data = getData(root);
		return data == null ? null : data.getMappings();
	}

	private final MappingTreeView mappings;

	public RenameMappingsData(MappingTreeView mappings) {
		this.mappings = mappings;
	}

	public MappingTreeView getMappings() {
		return mappings;
	}

	@Override
	public IJadxAttrType<RenameMappingsData> getAttrType() {
		return DATA;
	}
}
