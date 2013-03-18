package jadx.dex.nodes;

import java.util.List;

public interface IBlock extends IContainer {

	public List<InsnNode> getInstructions();
}
