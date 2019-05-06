package jadx.core.dex.visitors.debuginfo;

import java.util.ArrayList;
import java.util.List;

import com.android.dex.Dex.Section;

import jadx.core.dex.attributes.nodes.SourceFileAttr;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.DecodeException;

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

	private final MethodNode mth;
	private final Section section;
	private final DexNode dex;

	private final LocalVar[] locals;
	private final InsnNode[] insnByOffset;

	private List<LocalVar> resultList;

	public DebugInfoParser(MethodNode mth, int debugOffset, InsnNode[] insnByOffset) {
		this.mth = mth;
		this.dex = mth.dex();
		this.section = dex.openSection(debugOffset);

		int regsCount = mth.getRegsCount();
		this.locals = new LocalVar[regsCount];
		this.insnByOffset = insnByOffset;
	}

	public List<LocalVar> process() throws DecodeException {
		boolean varsInfoFound = false;
		resultList = new ArrayList<>();

		int addr = 0;
		int line = section.readUleb128();

		int paramsCount = section.readUleb128();
		List<RegisterArg> mthArgs = mth.getArguments(false);

		for (int i = 0; i < paramsCount; i++) {
			int nameId = section.readUleb128() - 1;
			if (nameId != DexNode.NO_INDEX) {
				String name = dex.getString(nameId);
				if (i < mthArgs.size() && name != null) {
					RegisterArg arg = mthArgs.get(i);
					int regNum = arg.getRegNum();
					LocalVar lVar = new LocalVar(regNum, name, arg.getInitType());
					startVar(lVar, -1);
					varsInfoFound = true;
				}
			}
		}

		// process '0' instruction
		addrChange(-1, 1, line);
		setLine(addr, line);

		int c = section.readByte() & 0xFF;
		while (c != DBG_END_SEQUENCE) {
			switch (c) {
				case DBG_ADVANCE_PC: {
					int addrInc = section.readUleb128();
					addr = addrChange(addr, addrInc, line);
					setLine(addr, line);
					break;
				}
				case DBG_ADVANCE_LINE: {
					line += section.readSleb128();
					break;
				}

				case DBG_START_LOCAL: {
					int regNum = section.readUleb128();
					int nameId = section.readUleb128() - 1;
					int type = section.readUleb128() - 1;
					LocalVar var = new LocalVar(dex, regNum, nameId, type, DexNode.NO_INDEX);
					startVar(var, addr);
					varsInfoFound = true;
					break;
				}
				case DBG_START_LOCAL_EXTENDED: {
					int regNum = section.readUleb128();
					int nameId = section.readUleb128() - 1;
					int type = section.readUleb128() - 1;
					int sign = section.readUleb128() - 1;
					LocalVar var = new LocalVar(dex, regNum, nameId, type, sign);
					startVar(var, addr);
					varsInfoFound = true;
					break;
				}
				case DBG_RESTART_LOCAL: {
					int regNum = section.readUleb128();
					restartVar(regNum, addr);
					varsInfoFound = true;
					break;
				}
				case DBG_END_LOCAL: {
					int regNum = section.readUleb128();
					LocalVar var = locals[regNum];
					if (var != null) {
						endVar(var, addr);
					}
					varsInfoFound = true;
					break;
				}

				case DBG_SET_PROLOGUE_END:
				case DBG_SET_EPILOGUE_BEGIN:
					// do nothing
					break;

				case DBG_SET_FILE: {
					int idx = section.readUleb128() - 1;
					if (idx != DexNode.NO_INDEX) {
						String sourceFile = dex.getString(idx);
						mth.addAttr(new SourceFileAttr(sourceFile));
					}
					break;
				}

				default: {
					if (c >= DBG_FIRST_SPECIAL) {
						int adjustedOpCode = c - DBG_FIRST_SPECIAL;
						int addrInc = adjustedOpCode / DBG_LINE_RANGE;
						addr = addrChange(addr, addrInc, line);
						line += DBG_LINE_BASE + adjustedOpCode % DBG_LINE_RANGE;
						setLine(addr, line);
					} else {
						throw new DecodeException("Unknown debug insn code: " + c);
					}
					break;
				}
			}
			c = section.readByte() & 0xFF;
		}

		if (varsInfoFound) {
			for (LocalVar var : locals) {
				if (var != null && !var.isEnd()) {
					endVar(var, mth.getCodeSize() - 1);
				}
			}
		}
		setSourceLines(addr, insnByOffset.length, line);

		return resultList;
	}

	private int addrChange(int addr, int addrInc, int line) {
		int newAddr = addr + addrInc;
		int maxAddr = insnByOffset.length - 1;
		newAddr = Math.min(newAddr, maxAddr);
		setSourceLines(addr, newAddr, line);
		return newAddr;
	}

	private void setSourceLines(int start, int end, int line) {
		for (int offset = start + 1; offset < end; offset++) {
			setLine(offset, line);
		}
	}

	private void setLine(int offset, int line) {
		InsnNode insn = insnByOffset[offset];
		if (insn != null) {
			insn.setSourceLine(line);
		}
	}

	private void restartVar(int regNum, int addr) {
		LocalVar prev = locals[regNum];
		if (prev != null) {
			endVar(prev, addr);
			LocalVar newVar = new LocalVar(regNum, prev.getName(), prev.getType());
			startVar(newVar, addr);
		} else {
			mth.addComment("Debug info: failed to restart local var, previous not found, register: " + regNum);
		}
	}

	private void startVar(LocalVar newVar, int addr) {
		int regNum = newVar.getRegNum();
		LocalVar prev = locals[regNum];
		if (prev != null) {
			endVar(prev, addr);
		}
		newVar.start(addr);
		locals[regNum] = newVar;
	}

	private void endVar(LocalVar var, int addr) {
		if (var.end(addr)) {
			resultList.add(var);
		}
	}
}
