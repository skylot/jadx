package jadx.api;

import org.jetbrains.annotations.Nullable;

public interface ICodeCache {

	void add(String clsFullName, ICodeInfo codeInfo);

	@Nullable
	ICodeInfo get(String clsFullName);

	void remove(String clsFullName);
}
