package jadx.api.plugins.input.data;

public interface ITry {
	ICatch getCatch();

	int getStartOffset();

	int getEndOffset();
}
