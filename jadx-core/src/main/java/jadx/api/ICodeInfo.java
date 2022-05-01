package jadx.api;

import jadx.api.impl.SimpleCodeInfo;
import jadx.api.metadata.ICodeMetadata;

public interface ICodeInfo {

	ICodeInfo EMPTY = new SimpleCodeInfo("");

	String getCodeStr();

	ICodeMetadata getCodeMetadata();

	boolean hasMetadata();
}
