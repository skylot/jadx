package jadx.api.plugins.input;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public interface JadxCodeInput {
	@NotNull
	ICodeLoader loadFiles(@NotNull List<Path> input);
}
