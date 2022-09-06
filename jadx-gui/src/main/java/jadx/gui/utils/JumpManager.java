package jadx.gui.utils;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class JumpManager {

	private final List<JumpPosition> list = new ArrayList<>();
	private int currentPos = 0;

	public void addPosition(JumpPosition pos) {
		if (ignoreJump(pos)) {
			return;
		}
		currentPos++;
		if (currentPos >= list.size()) {
			list.add(pos);
			currentPos = list.size() - 1;
		} else {
			list.set(currentPos, pos);
			int size = list.size();
			for (int i = currentPos + 1; i < size; i++) {
				list.set(i, null);
			}
		}
	}

	public void updateCurPosition(JumpPosition pos) {
		list.set(currentPos, pos);
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

	@Nullable
	private JumpPosition getCurrent() {
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
