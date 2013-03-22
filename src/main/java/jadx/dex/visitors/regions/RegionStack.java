package jadx.dex.visitors.regions;

import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.MethodNode;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionStack {
	private static final Logger LOG = LoggerFactory.getLogger(RegionStack.class);
	private static final boolean DEBUG = false;

	static {
		if (DEBUG)
			LOG.debug("Debug enabled for " + RegionStack.class);
	}

	private static class State {
		final Set<BlockNode> exits;
		IRegion region;

		public State() {
			exits = new HashSet<BlockNode>();
		}

		public State copy() {
			State c = new State();
			c.exits.addAll(exits);
			return c;
		}

		@Override
		public String toString() {
			return "Exits: " + exits;
		}
	}

	private final Stack<State> stack;
	private State curState;

	public RegionStack(MethodNode mth) {
		if (DEBUG)
			LOG.debug("New RegionStack: {}", mth);
		this.stack = new Stack<RegionStack.State>();
		this.curState = new State();
	}

	public void push(IRegion region) {
		stack.push(curState);
		if (stack.size() > 1000)
			throw new StackOverflowError("Deep code hierarchy");

		curState = curState.copy();
		curState.region = region;
		if (DEBUG)
			LOG.debug("Stack push: {} = {}", region, curState);
	}

	public void pop() {
		curState = stack.pop();
		if (DEBUG)
			LOG.debug("Stack pop : {}", curState);
	}

	/**
	 * Add boundary(exit) node for current stack frame
	 * 
	 * @param exit
	 *            boundary node, null will be ignored
	 */
	public void addExit(BlockNode exit) {
		if (exit != null)
			curState.exits.add(exit);
	}

	public boolean containsExit(BlockNode exit) {
		return curState.exits.contains(exit);
	}

	public IRegion peekRegion() {
		return curState.region;
	}

	public int size() {
		return stack.size();
	}
}
