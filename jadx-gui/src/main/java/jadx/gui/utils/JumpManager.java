package jadx.gui.utils;

import java.util.ArrayList;
import java.util.List;

public class JumpManager {

	private List<Position> list = new ArrayList<>();
	private int currentPos = 0;

	public void addPosition(Position pos) {
		if (pos.equals(getCurrent())) {
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

	private Position getCurrent() {
		if (currentPos >= 0 && currentPos < list.size()) {
			return list.get(currentPos);
		}
		return null;
	}

	public Position getPrev() {
		if (currentPos == 0) {
			return null;
		}
		currentPos--;
		return list.get(currentPos);
	}

	public Position getNext() {
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
		Position position = list.get(newPos);
		if (position == null) {
			return null;
		}
		currentPos = newPos;
		return position;
	}
}
