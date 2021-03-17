package jadx.gui.device.debugger.smali;

import java.util.*;

import jadx.api.plugins.input.data.IDebugInfo;
import jadx.api.plugins.input.data.ILocalVar;
import jadx.core.dex.nodes.InsnNode;
import jadx.gui.device.debugger.smali.Smali.SmaliRegister;

class SmaliMethodNode {
	private Map<Long, InsnNode> nodes; // codeOffset: InsnNode
	private SmaliRegister[] regList;
	private int[] insnPos;
	private int defPos;
	private Map<Integer, Integer> lineMapping = Collections.emptyMap(); // line: codeOffset
	private int paramRegStart;
	private int regCount;

	public SmaliMethodNode() {
	}

	public void setParamRegStart(int paramRegStart) {
		this.paramRegStart = paramRegStart;
	}

	public int getParamRegStart() {
		return this.paramRegStart;
	}

	public void setRegCount(int regCount) {
		this.regCount = regCount;
	}

	public int getRegCount() {
		return this.regCount;
	}

	public void setInsnNodes(Map<Long, InsnNode> nodes, int insnCount) {
		this.nodes = nodes;
		insnPos = new int[insnCount];
	}

	public Map<Integer, Integer> getLineMapping() {
		return lineMapping;
	}

	public void attachLine(int line, int codeOffset) {
		if (lineMapping.isEmpty()) {
			lineMapping = new HashMap<>();
		}
		lineMapping.put(line, codeOffset);
	}

	public void initRegInfoList(int regCount) {
		regList = new SmaliRegister[regCount];
		for (int i = 0; i < regCount; i++) {
			regList[i] = new SmaliRegister(i);
		}
	}

	public void setInsnPos(int codeOffset, int pos) {
		if (insnPos != null && codeOffset < insnPos.length) {
			insnPos[codeOffset] = pos;
		}
	}

	public int getInsnPos(long codeOffset) {
		if (insnPos != null && codeOffset < insnPos.length) {
			return insnPos[(int) codeOffset];
		}
		return -1;
	}

	public void setDefPos(int pos) {
		defPos = pos;
	}

	public int getDefPos() {
		return defPos;
	}

	public InsnNode getInsnNode(long codeOffset) {
		return nodes.get(codeOffset);
	}

	protected void setParamReg(int regNum, String name) {
		regList[regNum].setParam(name);
	}

	public void setDebugInfo(IDebugInfo info) {
		for (ILocalVar localVar : info.getLocalVars()) {
			regList[localVar.getRegNum()].addDbgInfo(localVar);
		}
	}

	public SmaliRegister getSmaliReg(int regNum) {
		return regList[regNum];
	}
}
