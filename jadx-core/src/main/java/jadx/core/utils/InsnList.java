package jadx.core.utils;

import java.util.Iterator;
import java.util.List;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;

public final class InsnList implements Iterable<InsnNode> {

	private final List<InsnNode> list;

	public InsnList(List<InsnNode> list) {
		this.list = list;
	}

	public static void remove(List<InsnNode> list, InsnNode insn) {
		for (Iterator<InsnNode> iterator = list.iterator(); iterator.hasNext();) {
			InsnNode next = iterator.next();
			if (next == insn) {
				iterator.remove();
				return;
			}
		}
	}

	public static void remove(BlockNode block, InsnNode insn) {
		remove(block.getInstructions(), insn);
	}

	public static int getIndex(List<InsnNode> list, InsnNode insn) {
		return getIndex(list, insn, 0);
	}

	public static int getIndex(List<InsnNode> list, InsnNode insn, int startOffset) {
		int size = list.size();
		for (int i = startOffset; i < size; i++) {
			if (list.get(i) == insn) {
				return i;
			}
		}
		return -1;
	}

	public static boolean contains(List<InsnNode> list, InsnNode insn) {
		return getIndex(list, insn, 0) != -1;
	}

	public static boolean contains(List<InsnNode> list, InsnNode insn, int startOffset) {
		return getIndex(list, insn, startOffset) != -1;
	}

	public int getIndex(InsnNode insn) {
		return getIndex(list, insn);
	}

	public boolean contains(InsnNode insn) {
		return getIndex(insn) != -1;
	}

	public void remove(InsnNode insn) {
		remove(list, insn);
	}

	public Iterator<InsnNode> iterator() {
		return list.iterator();
	}

	public InsnNode get(int index) {
		return list.get(index);
	}

	public int size() {
		return list.size();
	}
}
