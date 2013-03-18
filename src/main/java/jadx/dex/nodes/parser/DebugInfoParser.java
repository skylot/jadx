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

	private final static int DBG_END_SEQUENCE = 0x00;
	private final static int DBG_ADVANCE_PC = 0x01;
	private final static int DBG_ADVANCE_LINE = 0x02;
	private final static int DBG_START_LOCAL = 0x03;
	private final static int DBG_START_LOCAL_EXTENDED = 0x04;
	private final static int DBG_END_LOCAL = 0x05;
	private final static int DBG_RESTART_LOCAL = 0x06;
	private final static int DBG_SET_PROLOGUE_END = 0x07;
	private final static int DBG_SET_EPILOGUE_BEGIN = 0x08;
	private final static int DBG_SET_FILE = 0x09;

	private final static int DBG_FIRST_SPECIAL = 0x0a; // the smallest special opcode
	private final static int DBG_LINE_BASE = -4; // the smallest line number increment
	private final static int DBG_LINE_RANGE = 15; // the number of line increments represented

	private final MethodNode mth;
	private final Section section;
	private final DexNode dex;

	public DebugInfoParser(MethodNode mth, Section section) {
		this.mth = mth;
		this.section = section;
		this.dex = mth.dex();
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
					locals[regNum] = new LocalVarInfo(dex, regNum, nameId, type, DexNode.NO_INDEX);
					locals[regNum].start(addr, line);
					break;
				}
				case DBG_START_LOCAL_EXTENDED: {
					int regNum = section.readUleb128();
					int nameId = section.readUleb128() - 1;
					int type = section.readUleb128() - 1;
					int sign = section.readUleb128() - 1;
					locals[regNum] = new LocalVarInfo(dex, regNum, nameId, type, sign);
					locals[regNum].start(addr, line);
					break;
				}
				case DBG_RESTART_LOCAL: {
					int regNum = section.readUleb128();
					if (locals[regNum] != null)
						locals[regNum].start(addr, line);
					break;
				}
				case DBG_END_LOCAL: {
					int regNum = section.readUleb128();
					if (locals[regNum] != null)
						locals[regNum].end(addr, line);
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

						fillLocals(insnByOffset[addr], locals);
					} else {
						throw new DecodeException("Unknown debug insn code: " + c);
					}
					break;
			}

			c = section.readByte() & 0xFF;
		}
	}

	private void fillLocals(InsnNode insn, LocalVarInfo[] locals) {
		if (insn == null)
			return;

		if (insn.getResult() != null)
			merge(insn.getResult(), locals);

		for (InsnArg arg : insn.getArguments())
			merge(arg, locals);
	}

	private void merge(InsnArg arg, LocalVarInfo[] locals) {
		if (arg.isRegister()) {
			int rn = ((RegisterArg) arg).getRegNum();

			for (LocalVarInfo var : locals) {
				if (var != null && !var.isEnd()) {
					if (var.getRegNum() == rn)
						arg.replace(var);
				}
			}
		}
	}
}
