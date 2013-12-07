package jadx.core.dex.nodes;

import java.util.List;

public interface IBlock extends IContainer {

	List<InsnNode> getInstructions();
}
