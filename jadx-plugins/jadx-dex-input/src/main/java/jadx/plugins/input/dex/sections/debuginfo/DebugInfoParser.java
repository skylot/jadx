package jadx.plugins.input.dex.sections.debuginfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ILocalVar;
import jadx.api.plugins.input.data.impl.DebugInfo;
import jadx.plugins.input.dex.sections.DexConsts;
import jadx.plugins.input.dex.sections.SectionReader;

public class DebugInfoParser {
	private static final int DBG_END_SEQUENCE = 0x00;
	private static final int DBG_ADVANCE_PC = 0x01;
	private static final int DBG_ADVANCE_LINE = 0x02;
	private static final int DBG_START_LOCAL = 0x03;
	private static final int DBG_START_LOCAL_EXTENDED = 0x04;
	private static final int DBG_END_LOCAL = 0x05;
	private static final int DBG_RESTART_LOCAL = 0x06;
	private static final int DBG_SET_PROLOGUE_END = 0x07;
	private static final int DBG_SET_EPILOGUE_BEGIN = 0x08;
	private static final int DBG_SET_FILE = 0x09;

	// the smallest special opcode
	private static final int DBG_FIRST_SPECIAL = 0x0a;
	// the smallest line number increment
	private static final int DBG_LINE_BASE = -4;
	// the number of line increments represented
	private static final int DBG_LINE_RANGE = 15;

	private final SectionReader in;
	private final SectionReader ext;

	private final DexLocalVar[] locals;
	private final int codeSize;

	private List<ILocalVar> resultList;
	private Map<Integer, Integer> linesMap;
	@Nullable
	private String sourceFile;

	private List<String> argTypes;
	private int[] argRegs;

	public DebugInfoParser(SectionReader in, int regsCount, int codeSize) {
		this.in = in;
		this.ext = in.copy();
		this.locals = new DexLocalVar[regsCount];
		this.codeSize = codeSize;
	}

	public void initMthArgs(int regsCount, List<String> argTypes) {
		if (argTypes.isEmpty()) {
			this.argTypes = Collections.emptyList();
			return;
		}

		int argsCount = argTypes.size();
		int[] argRegsArr = new int[argsCount];
		int regNum = regsCount;
		for (int i = argsCount - 1; i >= 0; i--) {
			regNum -= getTypeLen(argTypes.get(i));
			argRegsArr[i] = regNum;
		}
		this.argRegs = argRegsArr;
		this.argTypes = argTypes;
	}

	public static int getTypeLen(String type) {
		switch (type.charAt(0)) {
			case 'J':
			case 'D':
				return 2;
			default:
				return 1;
		}
	}

	public DebugInfo process(int debugOff) {
		in.absPos(debugOff);

		boolean varsInfoFound = false;
		resultList = new ArrayList<>();
		linesMap = new HashMap<>();

		int addr = 0;
		int line = in.readUleb128();
		int paramsCount = in.readUleb128();
		int argsCount = argTypes.size();

		for (int i = 0; i < paramsCount; i++) {
			int nameId = in.readUleb128p1();
			String name = ext.getString(nameId);
			if (name != null && i < argsCount) {
				DexLocalVar paramVar = new DexLocalVar(argRegs[i], name, argTypes.get(i));
				startVar(paramVar, addr);
				paramVar.markAsParameter();
				varsInfoFound = true;
			}
		}
		while (true) {
			int c = in.readUByte();
			if (c == DBG_END_SEQUENCE) {
				break;
			}
			switch (c) {
				case DBG_ADVANCE_PC: {
					int addrInc = in.readUleb128();
					addr = addrChange(addr, addrInc);
					break;
				}
				case DBG_ADVANCE_LINE: {
					line += in.readSleb128();
					break;
				}
				case DBG_START_LOCAL: {
					int regNum = in.readUleb128();
					int nameId = in.readUleb128() - 1;
					int type = in.readUleb128() - 1;
					DexLocalVar var = new DexLocalVar(ext, regNum, nameId, type, DexConsts.NO_INDEX);
					startVar(var, addr);
					varsInfoFound = true;
					break;
				}
				case DBG_START_LOCAL_EXTENDED: {
					int regNum = in.readUleb128();
					int nameId = in.readUleb128p1();
					int type = in.readUleb128p1();
					int sign = in.readUleb128p1();
					DexLocalVar var = new DexLocalVar(ext, regNum, nameId, type, sign);
					startVar(var, addr);
					varsInfoFound = true;
					break;
				}
				case DBG_RESTART_LOCAL: {
					int regNum = in.readUleb128();
					restartVar(regNum, addr);
					varsInfoFound = true;
					break;
				}
				case DBG_END_LOCAL: {
					int regNum = in.readUleb128();
					DexLocalVar var = locals[regNum];
					if (var != null) {
						endVar(var, addr);
					}
					varsInfoFound = true;
					break;
				}
				case DBG_SET_PROLOGUE_END:
				case DBG_SET_EPILOGUE_BEGIN: {
					// do nothing
					break;
				}
				case DBG_SET_FILE: {
					int idx = in.readUleb128() - 1;
					this.sourceFile = ext.getString(idx);
					break;
				}
				default: {
					int adjustedOpCode = c - DBG_FIRST_SPECIAL;
					int addrInc = adjustedOpCode / DBG_LINE_RANGE;
					addr = addrChange(addr, addrInc);
					line += DBG_LINE_BASE + adjustedOpCode % DBG_LINE_RANGE;
					setLine(addr, line);
					break;
				}
			}
		}
		if (varsInfoFound) {
			for (DexLocalVar var : locals) {
				if (var != null && !var.isEnd()) {
					endVar(var, codeSize - 1);
				}
			}
		}
		return new DebugInfo(linesMap, resultList);
	}

	private int addrChange(int addr, int addrInc) {
		return Math.min(addr + addrInc, codeSize - 1);
	}

	private void setLine(int offset, int line) {
		linesMap.put(offset, line);
	}

	private void restartVar(int regNum, int addr) {
		DexLocalVar prev = locals[regNum];
		if (prev != null) {
			endVar(prev, addr);
			DexLocalVar newVar = new DexLocalVar(regNum, prev.getName(), prev.getType(), prev.getSignature());
			startVar(newVar, addr);
		}
	}

	private void startVar(DexLocalVar newVar, int addr) {
		int regNum = newVar.getRegNum();
		DexLocalVar prev = locals[regNum];
		if (prev != null) {
			endVar(prev, addr);
		}
		newVar.start(addr);
		locals[regNum] = newVar;
	}

	private void endVar(DexLocalVar var, int addr) {
		if (var.end(addr)) {
			resultList.add(var);
		}
	}
}
