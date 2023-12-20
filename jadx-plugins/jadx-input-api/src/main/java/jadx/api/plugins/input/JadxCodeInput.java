package jadx.api.plugins.input;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public interface JadxCodeInput {
	@NotNull ICodeLoader loadFiles(@NotNull List<Path> input);
}
