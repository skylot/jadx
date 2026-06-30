package jadx.core.dex.visitors.finaly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;

/**
 * A centrality state is an object which helps track how instructions can be skipped.
 * When looking for a finally, one of the things we have to do is make sure that instructions
 * are not part of the return and are actually part of the "finally" block.
 * This object helps keep track of registers, instructions etc to see if instructions can be
 * skipped.
 */
public final class CentralityState {

	private final Set<RegisterArg> allowableOutputArguments = new HashSet<>();
	private final SameInstructionsStrategy sameInstructionsStrategy;
	private boolean allowsCentral = true;
	private boolean allowsNonStartingNode;

	public CentralityState(SameInstructionsStrategy sameInstructionsStrategy, boolean allowsNonStartingNode) {
		this.sameInstructionsStrategy = sameInstructionsStrategy;
		this.allowsNonStartingNode = allowsNonStartingNode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("CentralityState - ");
		if (allowsCentral) {
			sb.append("allows central");
		} else {
			sb.append("disallows central");
		}
		sb.append(" | ");
		for (RegisterArg registerArg : allowableOutputArguments) {
			sb.append(registerArg.getName());
			sb.append(" ");
		}
		return sb.toString();
	}

	public SameInstructionsStrategy getSameInstructionsStrategy() {
		return sameInstructionsStrategy;
	}

	public boolean getAllowsCentral() {
		return allowsCentral;
	}

	public void setAllowsCentral(boolean allowsCentral) {
		this.allowsCentral = allowsCentral;
	}

	public boolean getAllowsNonStartingNode() {
		return allowsNonStartingNode;
	}

	public void setAllowsNonStartingNode(boolean allowsNonStartingNode) {
		this.allowsNonStartingNode = allowsNonStartingNode;
	}

	public void addAllowableOutput(RegisterArg allowableOutput) {
		allowableOutputArguments.add(allowableOutput);
	}

	public void addAllowableOutputs(Collection<RegisterArg> allowableOutputs) {
		allowableOutputArguments.addAll(allowableOutputs);
	}

	/**
	 * Adds all inputs register arguments from an instruction as allowable output arguments.
	 *
	 * @param allowableOutputInsn The instruction to retrieve the list of inputs from.
	 */
	public void addAllowableOutputs(InsnNode allowableOutputInsn) {
		List<RegisterArg> registerArgs = new LinkedList<>();
		for (InsnArg arg : allowableOutputInsn.getArgList()) {
			if (!(arg instanceof RegisterArg)) {
				continue;
			}
			registerArgs.add((RegisterArg) arg);
		}
		registerArgs.forEach(this::addAllowableOutput);
	}

	public boolean hasAllowableOutput(InsnNode insn) {
		if (allowableOutputArguments.isEmpty()) {
			return false;
		}
		RegisterArg registerArg;
		if (insn.getResult() != null) {
			registerArg = insn.getResult();
		} else {
			registerArg = null;
		}
		if (registerArg == null) {
			return false;
		}
		for (RegisterArg allowableOutput : allowableOutputArguments) {
			if (allowableOutput.equals(registerArg)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	public boolean hasAllowableInputs(InsnNode insn) {
		if (allowableOutputArguments.isEmpty()) {
			return false;
		}
		List<RegisterArg> registerArgs = new ArrayList<>();
		for (InsnArg arg : insn.getArgList()) {
			if (arg instanceof RegisterArg) {
				registerArgs.add((RegisterArg) arg);
			}
		}
		if (registerArgs.isEmpty() || allowableOutputArguments.isEmpty()) {
			return false;
		}
		for (RegisterArg regArg : registerArgs) {
			boolean foundMatch = false;
			for (RegisterArg allowableOutput : allowableOutputArguments) {
				if (regArg.equals(allowableOutput)) {
					foundMatch = true;
					break;
				}
			}
			if (!foundMatch) {
				return false;
			}
		}
		return true;
	}

	public CentralityState duplicate() {
		CentralityState state = new CentralityState(sameInstructionsStrategy, allowsNonStartingNode);
		state.allowsCentral = allowsCentral;
		state.allowableOutputArguments.addAll(allowableOutputArguments);
		return state;
	}

	public Set<RegisterArg> getAllowableOutputArguments() {
		return allowableOutputArguments;
	}
}
