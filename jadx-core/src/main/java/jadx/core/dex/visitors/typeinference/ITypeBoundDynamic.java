package jadx.core.dex.visitors.typeinference;

import jadx.core.dex.instructions.args.ArgType;

/**
 * 'Dynamic' type bound allows to use requested and not yet applied types
 * from {@link TypeUpdateInfo} for more precise restrictions
 */
public interface ITypeBoundDynamic extends ITypeBound {

	/**
	 * This method will be executed instead of {@link ITypeBound#getType()}
	 * if {@link TypeUpdateInfo} is available.
	 */
	ArgType getType(TypeUpdateInfo updateInfo);
}
