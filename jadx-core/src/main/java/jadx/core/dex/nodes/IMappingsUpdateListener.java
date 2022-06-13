package jadx.core.dex.nodes;

import net.fabricmc.mappingio.tree.MemoryMappingTree;

public interface IMappingsUpdateListener {

	void updated(MemoryMappingTree mappingTree);
}
