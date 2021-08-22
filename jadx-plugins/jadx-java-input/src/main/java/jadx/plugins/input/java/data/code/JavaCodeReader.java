package jadx.plugins.input.java.data.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IDebugInfo;
import jadx.api.plugins.input.data.ILocalVar;
import jadx.api.plugins.input.data.ITry;
import jadx.api.plugins.input.data.impl.CatchData;
import jadx.api.plugins.input.data.impl.DebugInfo;
import jadx.api.plugins.input.insns.InsnData;
import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;
import jadx.plugins.input.java.data.attributes.JavaAttrStorage;
import jadx.plugins.input.java.data.attributes.JavaAttrType;
import jadx.plugins.input.java.data.attributes.debuginfo.JavaLocalVar;
import jadx.plugins.input.java.data.attributes.debuginfo.LineNumberTableAttr;
import jadx.plugins.input.java.data.attributes.debuginfo.LocalVarTypesAttr;
import jadx.plugins.input.java.data.attributes.debuginfo.LocalVarsAttr;
import jadx.plugins.input.java.data.code.trycatch.JavaSingleCatch;
import jadx.plugins.input.java.data.code.trycatch.JavaTryData;
import jadx.plugins.input.java.utils.JavaClassParseException;

public class JavaCodeReader implements ICodeReader {

	private final JavaClassData clsData;
	private final DataReader reader;
	private final int codeOffset;

	public JavaCodeReader(JavaClassData clsData, int offset) {
		this.clsData = clsData;
		this.reader = clsData.getData();
		this.codeOffset = offset;
	}

	@Override
	public ICodeReader copy() {
		return this;
	}

	@Override
	public void visitInstructions(Consumer<InsnData> insnConsumer) {
		Set<Integer> excHandlers = getExcHandlers();
		int maxStack = readMaxStack();
		reader.skip(2);
		int codeSize = reader.readU4();

		CodeDecodeState state = new CodeDecodeState(clsData, reader, maxStack, excHandlers);
		JavaInsnData insn = new JavaInsnData(state);
		state.setInsn(insn);
		int offset = 0;
		while (offset < codeSize) {
			insn.setDecoded(false);
			insn.setOffset(offset);
			insn.setInsnStart(reader.getOffset());

			int opcode = reader.readU1();
			JavaInsnInfo insnInfo = JavaInsnsRegister.get(opcode);
			if (insnInfo == null) {
				throw new JavaClassParseException("Unknown opcode: 0x" + Integer.toHexString(opcode));
			}
			insn.setInsnInfo(insnInfo);
			insn.setInsnInfo(insnInfo);
			insn.setRegsCount(insnInfo.getRegsCount());
			insn.setOpcode(insnInfo.getApiOpcode());
			insn.setPayloadSize(insnInfo.getPayloadSize());
			insn.setOpcodeUnit(opcode);
			insn.setPayload(null);

			state.onInsn(offset);
			insnConsumer.accept(insn);

			int payloadSize = insn.getPayloadSize();
			if (!insn.isDecoded()) {
				if (payloadSize == -1) {
					insn.skip();
					payloadSize = insn.getPayloadSize();
				} else {
					reader.skip(payloadSize);
				}
			}
			offset += 1 + payloadSize;
		}
	}

	@Override
	public int getRegistersCount() {
		int maxStack = readMaxStack();
		int maxLocals = reader.readU2();
		return maxStack + maxLocals;
	}

	@Override
	public int getArgsStartReg() {
		return readMaxStack();
	}

	private int readMaxStack() {
		reader.absPos(codeOffset);
		int maxStack = reader.readU2();
		return maxStack + 1; // add one temporary register (for `swap` opcode)
	}

	@Override
	public int getUnitsCount() {
		return reader.absPos(codeOffset + 4).readU4();
	}

	@Override
	@Nullable
	public IDebugInfo getDebugInfo() {
		int maxStack = readMaxStack();
		reader.skip(2);
		reader.skip(reader.readU4());
		reader.skip(reader.readU2() * 8);

		JavaAttrStorage attrs = clsData.getAttributesReader().load(reader);
		LineNumberTableAttr linesAttr = attrs.get(JavaAttrType.LINE_NUMBER_TABLE);
		LocalVarsAttr varsAttr = attrs.get(JavaAttrType.LOCAL_VAR_TABLE);
		if (linesAttr == null && varsAttr == null) {
			return null;
		}
		Map<Integer, Integer> linesMap = linesAttr != null ? linesAttr.getLineMap() : Collections.emptyMap();

		List<ILocalVar> vars;
		if (varsAttr == null) {
			vars = Collections.emptyList();
		} else {
			List<JavaLocalVar> javaVars = varsAttr.getVars();
			LocalVarTypesAttr typedVars = attrs.get(JavaAttrType.LOCAL_VAR_TYPE_TABLE);
			if (typedVars != null && !typedVars.getVars().isEmpty()) {
				// merge signature from typedVars into javaVars
				Map<JavaLocalVar, JavaLocalVar> varsMap = new HashMap<>(javaVars.size());
				javaVars.forEach(v -> varsMap.put(v, v));
				for (JavaLocalVar typedVar : typedVars.getVars()) {
					JavaLocalVar jv = varsMap.get(typedVar);
					if (jv != null) {
						jv.setSignature(typedVar.getSignature());
					}
				}
			}
			javaVars.forEach(v -> v.shiftRegNum(maxStack));
			vars = Collections.unmodifiableList(javaVars);
		}
		return new DebugInfo(linesMap, vars);
	}

	@Override
	public int getCodeOffset() {
		return codeOffset;
	}

	@Override
	public List<ITry> getTries() {
		skipToTries();
		int excTableLen = reader.readU2();
		if (excTableLen == 0) {
			return Collections.emptyList();
		}
		ConstPoolReader constPool = clsData.getConstPoolReader();
		Map<JavaTryData, List<JavaSingleCatch>> tries = new HashMap<>(excTableLen);
		for (int i = 0; i < excTableLen; i++) {
			int start = reader.readU2();
			int end = reader.readU2();
			int handler = reader.readU2();
			int type = reader.readU2();
			JavaTryData tryData = new JavaTryData(start, end);
			List<JavaSingleCatch> catches = tries.computeIfAbsent(tryData, k -> new ArrayList<>());
			if (type == 0) {
				catches.add(new JavaSingleCatch(handler, null));
			} else {
				catches.add(new JavaSingleCatch(handler, constPool.getClass(type)));
			}
		}
		return tries.entrySet().stream()
				.map(e -> {
					JavaTryData tryData = e.getKey();
					tryData.setCatch(convertSingleCatches(e.getValue()));
					return tryData;
				})
				.collect(Collectors.toList());
	}

	private static CatchData convertSingleCatches(List<JavaSingleCatch> list) {
		int allHandler = -1;
		for (JavaSingleCatch singleCatch : list) {
			if (singleCatch.getType() == null) {
				allHandler = singleCatch.getHandler();
				list.remove(singleCatch);
				break;
			}
		}
		int len = list.size();
		int[] handlers = new int[len];
		String[] types = new String[len];
		for (int i = 0; i < len; i++) {
			JavaSingleCatch singleCatch = list.get(i);
			handlers[i] = singleCatch.getHandler();
			types[i] = singleCatch.getType();
		}
		return new CatchData(handlers, types, allHandler);
	}

	private Set<Integer> getExcHandlers() {
		skipToTries();
		int excTableLen = reader.readU2();
		if (excTableLen == 0) {
			return Collections.emptySet();
		}
		Set<Integer> set = new HashSet<>(excTableLen);
		for (int i = 0; i < excTableLen; i++) {
			reader.skip(4);
			int handler = reader.readU2();
			reader.skip(2);
			set.add(handler);
		}
		return set;
	}

	private void skipToTries() {
		reader.absPos(codeOffset + 4);
		int codeSize = reader.readU4();
		reader.skip(codeSize);
	}
}
