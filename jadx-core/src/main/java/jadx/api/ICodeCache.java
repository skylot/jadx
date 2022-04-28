package jadx.api;

import java.io.Closeable;

import org.jetbrains.annotations.NotNull;

public interface ICodeCache extends Closeable {

	void add(String clsFullName, ICodeInfo codeInfo);

	void remove(String clsFullName);

	@NotNull
	ICodeInfo get(String clsFullName);
}
