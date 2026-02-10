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

	public CentralityState(final SameInstructionsStrategy sameInstructionsStrategy, final boolean allowsNonStartingNode) {
		this.sameInstructionsStrategy = sameInstructionsStrategy;
		this.allowsNonStartingNode = allowsNonStartingNode;
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder("CentralityState - ");
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

	public final SameInstructionsStrategy getSameInstructionsStrategy() {
		return sameInstructionsStrategy;
	}

	public final boolean getAllowsCentral() {
		return allowsCentral;
	}

	public final void setAllowsCentral(final boolean allowsCentral) {
		this.allowsCentral = allowsCentral;
	}

	public final boolean getAllowsNonStartingNode() {
		return allowsNonStartingNode;
	}

	public final void setAllowsNonStartingNode(final boolean allowsNonStartingNode) {
		this.allowsNonStartingNode = allowsNonStartingNode;
	}

	public final void addAllowableOutput(final RegisterArg allowableOutput) {
		allowableOutputArguments.add(allowableOutput);
	}

	public final void addAllowableOutputs(final Collection<RegisterArg> allowableOutputs) {
		allowableOutputArguments.addAll(allowableOutputs);
	}

	/**
	 * Adds all inputs register arguments from an instruction as allowable output arguments.
	 *
	 * @param allowableOutputInsn The instruction to retrieve the list of inputs from.
	 */
	public final void addAllowableOutputs(final InsnNode allowableOutputInsn) {
		final List<RegisterArg> registerArgs = new LinkedList<>();
		for (final InsnArg arg : allowableOutputInsn.getArgList()) {
			if (!(arg instanceof RegisterArg)) {
				continue;
			}

			registerArgs.add((RegisterArg) arg);
		}

		registerArgs.forEach(this::addAllowableOutput);
	}

	public final boolean hasAllowableOutput(final InsnNode insn) {
		if (allowableOutputArguments.isEmpty()) {
			return false;
		}

		final RegisterArg registerArg;
		if (insn.getResult() != null) {
			registerArg = insn.getResult();
		} else {
			registerArg = null;
		}

		if (registerArg == null) {
			return false;
		}

		for (final RegisterArg allowableOutput : allowableOutputArguments) {
			if (allowableOutput.equals(registerArg)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	public final boolean hasAllowableInputs(final InsnNode insn) {
		if (allowableOutputArguments.isEmpty()) {
			return false;
		}

		final List<RegisterArg> registerArgs = new ArrayList<>();

		for (final InsnArg arg : insn.getArgList()) {
			if (arg instanceof RegisterArg) {
				registerArgs.add((RegisterArg) arg);
			}
		}

		if (registerArgs.isEmpty() || allowableOutputArguments.isEmpty()) {
			return false;
		}

		for (final RegisterArg regArg : registerArgs) {
			boolean foundMatch = false;
			for (final RegisterArg allowableOutput : allowableOutputArguments) {
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

	public final CentralityState duplicate() {
		final CentralityState state = new CentralityState(sameInstructionsStrategy, allowsNonStartingNode);
		state.allowsCentral = allowsCentral;
		state.allowableOutputArguments.addAll(allowableOutputArguments);
		return state;
	}

	public final Set<RegisterArg> getAllowableOutputArguments() {
		return allowableOutputArguments;
	}
}
