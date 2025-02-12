package jadx.gui.utils;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class JumpManager {
	/**
	 * Maximum number of elements to store in a jump list
	 */
	private static final int MAX_JUMPS = 100;

	/**
	 * This number of elements will be removed from the start if a list becomes bigger than MAX_JUMPS.
	 * List grow most of the time, so removing should be done in big batches to not run very often.
	 * Because of this, an effective jump history size will vary
	 * from (MAX_JUMPS - LIST_SHRINK_COUNT) to MAX_JUMPS over time.
	 */
	private static final int LIST_SHRINK_COUNT = 50;

	private final List<JumpPosition> list = new ArrayList<>(MAX_JUMPS);
	private int currentPos = 0;

	public void addPosition(@Nullable JumpPosition pos) {
		if (pos == null || ignoreJump(pos)) {
			return;
		}
		currentPos++;
		if (currentPos >= list.size()) {
			list.add(pos);
			if (list.size() >= MAX_JUMPS) {
				list.subList(0, LIST_SHRINK_COUNT).clear();
			}
			currentPos = list.size() - 1;
		} else {
			// discard forward history after navigating back and jumping to a new place
			list.set(currentPos, pos);
			list.subList(currentPos + 1, list.size()).clear();
		}
	}

	public int size() {
		return list.size();
	}

	private boolean ignoreJump(JumpPosition pos) {
		JumpPosition current = getCurrent();
		if (current == null) {
			return false;
		}
		return pos.equals(current);
	}

	public @Nullable JumpPosition getCurrent() {
		if (currentPos >= 0 && currentPos < list.size()) {
			return list.get(currentPos);
		}
		return null;
	}

	@Nullable
	public JumpPosition getPrev() {
		if (currentPos == 0) {
			return null;
		}
		currentPos--;
		return list.get(currentPos);
	}

	@Nullable
	public JumpPosition getNext() {
		int size = list.size();
		if (size == 0) {
			currentPos = 0;
			return null;
		}
		int newPos = currentPos + 1;
		if (newPos >= size) {
			currentPos = size - 1;
			return null;
		}
		JumpPosition position = list.get(newPos);
		if (position == null) {
			return null;
		}
		currentPos = newPos;
		return position;
	}

	public void reset() {
		list.clear();
		currentPos = 0;
	}
}
