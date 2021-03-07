package jadx.core.dex.nodes;

import java.util.List;

import jadx.api.ICodeWriter;
import jadx.core.codegen.RegionGen;
import jadx.core.utils.exceptions.CodegenException;

public interface IBlock extends IContainer {

	List<InsnNode> getInstructions();

	@Override
	default void generate(RegionGen regionGen, ICodeWriter code) throws CodegenException {
		regionGen.makeSimpleBlock(this, code);
	}
}
