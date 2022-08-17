package jadx.plugins.mappings;

import java.util.Collections;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.args.UserRenamesMappingsMode;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.pass.JadxPassContext;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.plugins.mappings.load.CodeMappingsVisitor;
import jadx.plugins.mappings.load.MappingsVisitor;

public class RenameMappingsPlugin implements JadxPlugin {

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("jadx-rename-mappings", "Rename Mappings", "various mappings support");
	}

	@Override
	public void init(JadxPluginContext context) {
		JadxArgs args = ((JadxDecompiler) context.getDecompiler()).getArgs();
		MappingTree mappingTree = openMapping(args);
		if (mappingTree != null) {
			JadxPassContext passContext = context.getPassContext();
			passContext.addPass(new MappingsVisitor(mappingTree));
			passContext.addPass(new CodeMappingsVisitor(mappingTree));
		}
	}

	public MappingTree openMapping(JadxArgs args) {
		if (args.getUserRenamesMappingsMode() != UserRenamesMappingsMode.IGNORE
				&& args.getUserRenamesMappingsPath() != null) {
			try {
				MemoryMappingTree mappingTree = new MemoryMappingTree();
				MappingReader.read(args.getUserRenamesMappingsPath(), mappingTree);
				if (mappingTree.getSrcNamespace() == null) {
					mappingTree.setSrcNamespace(MappingUtil.NS_SOURCE_FALLBACK);
				}
				if (mappingTree.getDstNamespaces() == null || mappingTree.getDstNamespaces().isEmpty()) {
					mappingTree.setDstNamespaces(Collections.singletonList(MappingUtil.NS_TARGET_FALLBACK));
				} else if (mappingTree.getDstNamespaces().size() > 1) {
					throw new JadxRuntimeException(
							String.format("JADX only supports mappings with just one destination namespace! The provided ones have %s.",
									mappingTree.getDstNamespaces().size()));
				}
				return mappingTree;
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to load mappings", e);
			}
		}
		return null;
	}
}
