package jadx.core.dex.visitors.typeinference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;

@FunctionalInterface
public interface ITypeListener {

	/**
	 * Listener function - triggered on type update
	 *
	 * @param updateInfo    store all allowed type updates
	 * @param arg           apply suggested type for this arg
	 * @param candidateType suggest new type
	 */
	@Nullable
	TypeUpdateResult update(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, @NotNull ArgType candidateType);
}
