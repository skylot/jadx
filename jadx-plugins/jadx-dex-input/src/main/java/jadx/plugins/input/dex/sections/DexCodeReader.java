package jadx.plugins.input.dex.sections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IDebugInfo;
import jadx.api.plugins.input.data.ITry;
import jadx.api.plugins.input.data.impl.CatchData;
import jadx.api.plugins.input.data.impl.TryData;
import jadx.api.plugins.input.insns.InsnData;
import jadx.plugins.input.dex.DexException;
import jadx.plugins.input.dex.insns.DexInsnData;
import jadx.plugins.input.dex.insns.DexInsnFormat;
import jadx.plugins.input.dex.insns.DexInsnInfo;
import jadx.plugins.input.dex.sections.debuginfo.DebugInfoParser;

public class DexCodeReader implements ICodeReader {

	private final SectionReader in;
	private int mthId;

	public DexCodeReader(SectionReader in) {
		this.in = in;
	}

	@Override
	public DexCodeReader copy() {
		DexCodeReader copy = new DexCodeReader(in.copy());
		copy.setMthId(this.getMthId());
		return copy;
	}

	public void setOffset(int offset) {
		this.in.setOffset(offset);
	}

	@Override
	public int getRegistersCount() {
		return in.pos(0).readUShort();
	}

	@Override
	public int getArgsStartReg() {
		return -1;
	}

	@Override
	public int getUnitsCount() {
		return in.pos(12).readInt();
	}

	@Override
	public void visitInstructions(Consumer<InsnData> insnConsumer) {
		DexInsnData insnData = new DexInsnData(this, in.copy());
		in.pos(12);
		int size = in.readInt();
		int offset = 0; // in code units (2 byte)
		while (offset < size) {
			int insnStart = in.getAbsPos();
			int opcodeUnit = in.readUShort();
			DexInsnInfo insnInfo = DexInsnInfo.get(opcodeUnit);
			insnData.setInsnStart(insnStart);
			insnData.setOffset(offset);
			insnData.setInsnInfo(insnInfo);
			insnData.setOpcodeUnit(opcodeUnit);
			insnData.setPayload(null);
			insnData.setDecoded(false);
			if (insnInfo != null) {
				DexInsnFormat format = insnInfo.getFormat();
				insnData.setRegsCount(format.getRegsCount());
				insnData.setLength(format.getLength());
			} else {
				insnData.setRegsCount(0);
				insnData.setLength(1);
			}

			insnConsumer.accept(insnData);

			if (!insnData.isDecoded()) {
				skip(insnData);
			}
			offset += insnData.getLength();
		}
	}

	public void decode(DexInsnData insn) {
		DexInsnFormat format = insn.getInsnInfo().getFormat();
		format.decode(insn, insn.getOpcodeUnit(), insn.getCodeData().in);
		insn.setDecoded(true);
	}

	public void skip(DexInsnData insn) {
		DexInsnInfo insnInfo = insn.getInsnInfo();
		if (insnInfo != null) {
			DexCodeReader codeReader = insn.getCodeData();
			insnInfo.getFormat().skip(insn, codeReader.in);
		}
	}

	@Nullable
	@Override
	public IDebugInfo getDebugInfo() {
		int debugOff = in.pos(8).readInt();
		if (debugOff == 0) {
			return null;
		}
		int regsCount = getRegistersCount();
		DebugInfoParser debugInfoParser = new DebugInfoParser(in, regsCount, getUnitsCount());
		debugInfoParser.initMthArgs(regsCount, in.getMethodParamTypes(mthId));
		return debugInfoParser.process(debugOff);
	}

	private int getTriesCount() {
		return in.pos(6).readUShort();
	}

	private int getTriesOffset() {
		int triesCount = getTriesCount();
		if (triesCount == 0) {
			return -1;
		}
		int insnsCount = getUnitsCount();
		int padding = insnsCount % 2 == 1 ? 2 : 0;
		return 4 * 4 + insnsCount * 2 + padding;
	}

	@Override
	public List<ITry> getTries() {
		int triesOffset = getTriesOffset();
		if (triesOffset == -1) {
			return Collections.emptyList();
		}
		int triesCount = getTriesCount();
		Map<Integer, ICatch> catchHandlers = getCatchHandlers(triesOffset + 8 * triesCount, in.copy());
		in.pos(triesOffset);
		List<ITry> triesList = new ArrayList<>(triesCount);
		for (int i = 0; i < triesCount; i++) {
			int startAddr = in.readInt();
			int insnsCount = in.readUShort();
			int handlerOff = in.readUShort();
			ICatch catchHandler = catchHandlers.get(handlerOff);
			if (catchHandler == null) {
				throw new DexException("Catch handler not found by byte offset: " + handlerOff);
			}
			triesList.add(new TryData(startAddr, startAddr + insnsCount - 1, catchHandler));
		}
		return triesList;
	}

	private Map<Integer, ICatch> getCatchHandlers(int offset, SectionReader ext) {
		in.pos(offset);
		int byteOffsetStart = in.getAbsPos();
		int size = in.readUleb128();
		Map<Integer, ICatch> map = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			int byteIndex = in.getAbsPos() - byteOffsetStart;
			int sizeAndType = in.readSleb128();
			int handlersLen = Math.abs(sizeAndType);
			int[] addr = new int[handlersLen];
			String[] types = new String[handlersLen];
			for (int h = 0; h < handlersLen; h++) {
				types[h] = ext.getType(in.readUleb128());
				addr[h] = in.readUleb128();
			}
			int catchAllAddr;
			if (sizeAndType <= 0) {
				catchAllAddr = in.readUleb128();
			} else {
				catchAllAddr = -1;
			}
			map.put(byteIndex, new CatchData(addr, types, catchAllAddr));
		}
		return map;
	}

	@Override
	public int getCodeOffset() {
		return in.getOffset();
	}

	public void setMthId(int mthId) {
		this.mthId = mthId;
	}

	public int getMthId() {
		return mthId;
	}
}
