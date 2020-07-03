package jadx.api.plugins.input.data;

public interface ITry {
	ICatch getCatch();

	int getStartAddress();

	int getInstructionCount();
}
