package jadx.core.dex.nodes;

import jadx.api.ICodeWriter;
import jadx.core.codegen.RegionGen;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.utils.exceptions.CodegenException;

public interface IContainer extends IAttributeNode {

	/**
	 * Unique id for use in 'toString()' method
	 */
	String baseString();

	/**
	 * Dispatch to needed generate method in RegionGen
	 */
	default void generate(RegionGen regionGen, ICodeWriter code) throws CodegenException {
		throw new CodegenException("Code generate not implemented for container: " + getClass().getSimpleName());
	}
}
