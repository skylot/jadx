package jadx.api.plugins.input.insns.custom;

public interface ISwitchPayload extends ICustomPayload {
	int getSize();

	int[] getKeys();

	int[] getTargets();
}
