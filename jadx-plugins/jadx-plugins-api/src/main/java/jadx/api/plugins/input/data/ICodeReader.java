package jadx.api.plugins.input.data;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.insns.InsnData;

public interface ICodeReader {
	ICodeReader copy();

	void visitInstructions(Consumer<InsnData> insnConsumer);

	int getRegistersCount();

	int getArgsStartReg();

	int getUnitsCount();

	@Nullable
	IDebugInfo getDebugInfo();

	int getCodeOffset();

	List<ITry> getTries();
}
