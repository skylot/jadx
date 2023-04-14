package jadx.plugins.mappings.load;

import java.nio.file.Path;
import java.util.Collections;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import jadx.api.JadxArgs;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxPreparePass;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.plugins.mappings.RenameMappingsData;
import jadx.plugins.mappings.RenameMappingsOptions;

public class LoadMappingsPass implements JadxPreparePass {

	private final RenameMappingsOptions options;

	public LoadMappingsPass(RenameMappingsOptions options) {
		this.options = options;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new SimpleJadxPassInfo("LoadMappings", "Load mappings file");
	}

	@Override
	public void init(RootNode root) {
		MappingTree mappings = loadMapping(root.getArgs());
		root.getAttributes().add(new RenameMappingsData(mappings));
	}

	private MappingTree loadMapping(JadxArgs args) {
		try {
			Path mappingsPath = args.getUserRenamesMappingsPath();
			MemoryMappingTree mappingTree = new MemoryMappingTree();
			MappingReader.read(mappingsPath, options.getFormat(), mappingTree);
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
			if (options.isInvert()) {
				MemoryMappingTree invertedMappingTree = new MemoryMappingTree();
				String dstNamespace = mappingTree.getDstNamespaces().get(0);
				mappingTree.accept(new MappingSourceNsSwitch(invertedMappingTree, dstNamespace));
				return invertedMappingTree;
			}
			return mappingTree;
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load mappings", e);
		}
	}
}
