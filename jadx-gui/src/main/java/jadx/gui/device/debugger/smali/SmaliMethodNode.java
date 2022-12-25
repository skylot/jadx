package jadx.gui.device.debugger.smali;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;

class SmaliMethodNode {
	private Map<Long, InsnNode> nodes; // codeOffset: InsnNode
	private List<SmaliRegister> regList;
	private int[] insnPos;
	private int defPos;
	private Map<Integer, Integer> lineMapping = Collections.emptyMap(); // line: codeOffset
	private int paramRegStart;
	private int regCount;

	public int getParamRegStart() {
		return this.paramRegStart;
	}

	public int getRegCount() {
		return this.regCount;
	}

	public Map<Integer, Integer> getLineMapping() {
		return lineMapping;
	}

	public void initRegInfoList(int regCount, int insnCount) {
		regList = new ArrayList<>(regCount);
		for (int i = 0; i < regCount; i++) {
			regList.add(new SmaliRegister(i, insnCount));
		}
	}

	public int getInsnPos(long codeOffset) {
		if (insnPos != null && codeOffset < insnPos.length) {
			return insnPos[(int) codeOffset];
		}
		return -1;
	}

	public int getDefPos() {
		return defPos;
	}

	public InsnNode getInsnNode(long codeOffset) {
		return nodes.get(codeOffset);
	}

	public List<SmaliRegister> getRegList() {
		return regList;
	}

	protected SmaliMethodNode() {
	}

	protected void setRegCount(int regCount) {
		this.regCount = regCount;
	}

	protected void attachLine(int line, int codeOffset) {
		if (lineMapping.isEmpty()) {
			lineMapping = new HashMap<>();
		}
		lineMapping.put(line, codeOffset);
	}

	protected void setInsnInfo(int codeOffset, int pos) {
		if (insnPos != null && codeOffset < insnPos.length) {
			insnPos[codeOffset] = pos;
		}
		InsnNode insn = getInsnNode(codeOffset);
		RegisterArg r = insn.getResult();
		if (r != null) {
			regList.get(r.getRegNum()).setStartOffset(codeOffset);
		}
		for (InsnArg arg : insn.getArguments()) {
			if (arg instanceof RegisterArg) {
				regList.get(((RegisterArg) arg).getRegNum()).setStartOffset(codeOffset);
			}
		}
	}

	protected void setDefPos(int pos) {
		defPos = pos;
	}

	protected void setParamReg(int regNum, String name) {
		SmaliRegister r = regList.get(regNum);
		r.setParam(name);
	}

	protected void setParamRegStart(int paramRegStart) {
		this.paramRegStart = paramRegStart;
	}

	protected void setInsnNodes(Map<Long, InsnNode> nodes, int insnCount) {
		this.nodes = nodes;
		insnPos = new int[insnCount];
	}
}
