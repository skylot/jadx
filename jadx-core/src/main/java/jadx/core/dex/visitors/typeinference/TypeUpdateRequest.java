package jadx.core.dex.visitors.typeinference;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;

public class TypeUpdateRequest {
	private final InsnArg arg;
	private final ArgType candidateType;
	private final boolean direct;
	private final @Nullable ITypeUpdateCallback callback;

	public TypeUpdateRequest(InsnArg arg, ArgType candidateType, boolean direct, @Nullable ITypeUpdateCallback callback) {
		this.arg = arg;
		this.candidateType = candidateType;
		this.direct = direct;
		this.callback = callback;
	}

	public InsnArg getArg() {
		return arg;
	}

	public ArgType getCandidateType() {
		return candidateType;
	}

	public boolean isDirect() {
		return direct;
	}

	public @Nullable ITypeUpdateCallback getCallback() {
		return callback;
	}

	@Override
	public String toString() {
		return "TypeUpdateRequest{arg=" + arg + ", candidateType=" + candidateType + '}';
	}
}
