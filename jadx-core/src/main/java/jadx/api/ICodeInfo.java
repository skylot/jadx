package jadx.api;

import java.util.Map;

import jadx.api.impl.SimpleCodeInfo;
import jadx.api.metadata.ICodeMetadata;

public interface ICodeInfo {

	ICodeInfo EMPTY = new SimpleCodeInfo("");

	String getCodeStr();

	/**
	 * Replaced by {@link ICodeInfo#getCodeMetadata()}
	 */
	@Deprecated
	Map<Integer, Integer> getLineMapping();

	/**
	 * Replaced by {@link ICodeInfo#getCodeMetadata()}
	 */
	@Deprecated
	Map<CodePosition, Object> getAnnotations();

	ICodeMetadata getCodeMetadata();

	boolean hasMetadata();
}
