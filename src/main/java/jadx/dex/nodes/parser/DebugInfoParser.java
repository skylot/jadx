package jadx.dex.nodes.parser;

import jadx.dex.info.LocalVarInfo;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.DexNode;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.exceptions.DecodeException;

import java.util.List;

import com.android.dx.io.DexBuffer.Section;

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

	private static final int DBG_FIRST_SPECIAL = 0x0a; // the smallest special opcode
	private static final int DBG_LINE_BASE = -4; // the smallest line number increment
	private static final int DBG_LINE_RANGE = 15; // the number of line increments represented

	private final MethodNode mth;
	private final Section section;
	private final DexNode dex;

	public DebugInfoParser(MethodNode mth, int debugOffset) {
		this.mth = mth;
		this.dex = mth.dex();
		this.section = dex.openSection(debugOffset);
	}

	public void process(InsnNode[] insnByOffset) throws DecodeException {
		int addr = 0;
		int line;
		// String source_file;

		line = section.readUleb128();
		int param_size = section.readUleb128(); // exclude 'this'
		List<RegisterArg> mthArgs = mth.getArguments(false);
		assert param_size == mthArgs.size();

		for (int i = 0; i < param_size; i++) {
			int id = section.readUleb128() - 1;
			if (id != DexNode.NO_INDEX) {
				String name = dex.getString(id);
				mthArgs.get(i).getTypedVar().setName(name);
			}
		}

		LocalVarInfo[] locals = new LocalVarInfo[mth.getRegsCount()];
		for (RegisterArg arg : mthArgs) {
			locals[arg.getRegNum()] = new LocalVarInfo(dex, arg.getRegNum(),
					arg.getTypedVar().getName(), arg.getType(), null);
		}

		int c = section.readByte() & 0xFF;
		while (c != DBG_END_SEQUENCE) {
			switch (c) {
				case DBG_ADVANCE_PC:
					addr += section.readUleb128();
					break;
				case DBG_ADVANCE_LINE:
					line += section.readSleb128();
					break;

				case DBG_START_LOCAL: {
					int regNum = section.readUleb128();
					int nameId = section.readUleb128() - 1;
					int type = section.readUleb128() - 1;
					LocalVarInfo var = new LocalVarInfo(dex, regNum, nameId, type, DexNode.NO_INDEX);
					startVar(var, locals, insnByOffset, addr, line);
					break;
				}
				case DBG_START_LOCAL_EXTENDED: {
					int regNum = section.readUleb128();
					int nameId = section.readUleb128() - 1;
					int type = section.readUleb128() - 1;
					int sign = section.readUleb128() - 1;
					LocalVarInfo var = new LocalVarInfo(dex, regNum, nameId, type, sign);
					startVar(var, locals, insnByOffset, addr, line);
					break;
				}
				case DBG_RESTART_LOCAL: {
					int regNum = section.readUleb128();
					LocalVarInfo var = locals[regNum];
					if (var != null) {
						var.end(addr, line);
						setVar(var, insnByOffset);
						var.start(addr, line);
					}
					break;
				}
				case DBG_END_LOCAL: {
					int regNum = section.readUleb128();
					LocalVarInfo var = locals[regNum];
					if (var != null) {
						var.end(addr, line);
						setVar(var, insnByOffset);
					}
					break;
				}

				case DBG_SET_PROLOGUE_END:
					break;
				case DBG_SET_EPILOGUE_BEGIN:
					break;

				case DBG_SET_FILE:
					section.readUleb128();
					// source_file = dex.getString(idx);
					break;

				default:
					if (c >= DBG_FIRST_SPECIAL) {
						int adjusted_opcode = c - DBG_FIRST_SPECIAL;
						line += DBG_LINE_BASE + (adjusted_opcode % DBG_LINE_RANGE);
						addr += (adjusted_opcode / DBG_LINE_RANGE);
					} else {
						throw new DecodeException("Unknown debug insn code: " + c);
					}
					break;
			}

			c = section.readByte() & 0xFF;
		}

		for (LocalVarInfo var : locals) {
			if (var != null && !var.isEnd()) {
				var.end(addr, line);
				setVar(var, insnByOffset);
			}
		}
	}

	private void startVar(LocalVarInfo var, LocalVarInfo[] locals, InsnNode[] insnByOffset, int addr, int line) {
		int regNum = var.getRegNum();
		LocalVarInfo prev = locals[regNum];
		if (prev != null && !prev.isEnd()) {
			prev.end(addr, line);
			setVar(prev, insnByOffset);
		}
		var.start(addr, line);
		locals[regNum] = var;
	}

	private void setVar(LocalVarInfo var, InsnNode[] insnByOffset) {
		int start = var.getStartAddr();
		int end = var.getEndAddr();

		for (int i = start; i <= end; i++) {
			InsnNode insn = insnByOffset[i];
			fillLocals(insn, var);
		}
	}

	private void fillLocals(InsnNode insn, LocalVarInfo var) {
		if (insn == null)
			return;

		if (insn.getResult() != null)
			merge(insn.getResult(), var);

		for (InsnArg arg : insn.getArguments())
			merge(arg, var);
	}

	private void merge(InsnArg arg, LocalVarInfo var) {
		if (arg.isRegister()) {
			int rn = ((RegisterArg) arg).getRegNum();
			if (var.getRegNum() == rn)
				arg.setTypedVar(var.getTypedVar());
		}
	}
}
