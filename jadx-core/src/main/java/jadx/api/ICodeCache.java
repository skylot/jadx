package jadx.api;

import org.jetbrains.annotations.Nullable;

public interface ICodeCache {

	void add(String clsFullName, ICodeInfo codeInfo);

	void remove(String clsFullName);

	@Nullable
	ICodeInfo get(String clsFullName);
}
