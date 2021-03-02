package jadx.plugins.input.dex.smali;

import java.util.*;
import java.util.Map.Entry;

import jadx.api.plugins.input.data.*;
import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.InsnIndexType;
import jadx.plugins.input.dex.insns.DexOpcodes;
import jadx.plugins.input.dex.insns.payloads.DexSwitchPayload;
import jadx.plugins.input.dex.sections.DexFieldData;
import jadx.plugins.input.dex.sections.DexMethodData;
import jadx.plugins.input.dex.sections.DexMethodRef;

import static jadx.api.plugins.input.data.AccessFlagsScope.FIELD;
import static jadx.api.plugins.input.data.AccessFlagsScope.METHOD;

// TODO: not finished
public class SmaliPrinter {

	public static String printMethod(DexMethodData mth) {
		SmaliCodeWriter codeWriter = new SmaliCodeWriter();
		codeWriter.startLine(".method ");
		codeWriter.add(AccessFlags.format(mth.getAccessFlags(), METHOD));

		DexMethodRef methodRef = mth.getMethodRef();
		methodRef.load();
		codeWriter.add(methodRef.getName());
		codeWriter.add('(').addArgs(methodRef.getArgTypes()).add(')');
		codeWriter.add(methodRef.getReturnType());
		codeWriter.incIndent();

		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader != null) {
			codeWriter.startLine(".registers ").add(codeReader.getRegistersCount());
			SmaliInsnFormat insnFormat = SmaliInsnFormat.getInstance();
			InsnFormatterInfo formatterInfo = new InsnFormatterInfo(codeWriter, mth);
			codeReader.visitInstructions(insn -> {
				codeWriter.startLine();
				formatterInfo.setInsn(insn);
				insnFormat.format(formatterInfo);
			});
			codeWriter.decIndent();
		}
		codeWriter.startLine(".end method");
		return codeWriter.getCode();
	}

	public static String printClass(IClassData cls) {
		SmaliCodeWriter smali = new SmaliCodeWriter();
		smali.startLine("Class: " + cls.getType())
				.startLine("AccessFlags: " + AccessFlags.format(cls.getAccessFlags(), AccessFlagsScope.CLASS))
				.startLine("SuperType: " + cls.getSuperType())
				.startLine("Interfaces: " + cls.getInterfacesTypes())
				.startLine("SourceFile: " + cls.getSourceFile());

		if (cls.getAnnotations().size() > 0) {
			smali.startLine().startLine("# annotations");
			printAnnotations(smali, cls.getAnnotations());
		}
		List<Entry<DexFieldData, List<IAnnotation>>> flds = new ArrayList<>();
		cls.visitFieldsAndMethods(
				f -> {
					DexFieldData fld = new DexFieldData(null);
					fld.setParentClassType(f.getParentClassType());
					fld.setAccessFlags(f.getAccessFlags());
					fld.setName(f.getName());
					fld.setType(f.getType());
					flds.add(new AbstractMap.SimpleEntry<>(fld, f.getAnnotations()));
				},
				m -> {
					if (!flds.isEmpty()) {
						printField(smali, flds, cls.getStaticFieldInitValues());
						flds.clear();
						smali.startLine("# methods");
					}
					printMethod(smali, m);
				});
		if (!flds.isEmpty()) { // in case there are no methods.
			printField(smali, flds, cls.getStaticFieldInitValues());
			flds.clear();
		}
		return smali.getCode();
	}

	private static void printField(SmaliCodeWriter smali,
			List<Entry<DexFieldData, List<IAnnotation>>> flds,
			List<EncodedValue> staticFieldInitValues) {
		int staticIdx = 0;
		int accessColWidth = 0;
		int nameColWidth = 0;
		List<String> accesses = new ArrayList<>(flds.size());
		for (Entry<DexFieldData, List<IAnnotation>> fld : flds) { // calc width of cols
			String temp = fld.getKey().getName();
			if (temp.length() > nameColWidth) {
				nameColWidth = temp.length();
			}
			temp = AccessFlags.format(fld.getKey().getAccessFlags(), FIELD);
			accesses.add(temp);
			if (temp.length() > accessColWidth) {
				accessColWidth = temp.length();
			}
		}
		smali.startLine().startLine("# fields");
		String whites = new String(new byte[Math.max(accessColWidth, nameColWidth)]).replace("\0", " ");
		for (int i = 0; i < flds.size(); i++) {
			smali.startLine();
			Entry<DexFieldData, List<IAnnotation>> fld = flds.get(i);
			String access = accesses.get(i);
			int pad = accessColWidth - access.length();
			if (pad > 0) {
				access += whites.substring(0, pad);
			}
			smali.add(".field ").add(access);
			String name = fld.getKey().getName();
			pad = nameColWidth - name.length();
			if (pad > 0) {
				name += whites.substring(0, pad);
			}
			smali.add(name).add(" ");
			smali.add(": ").add(fld.getKey().getType());
			if ((fld.getKey().getAccessFlags() & AccessFlags.STATIC) != 0) { // static field
				if (staticIdx < staticFieldInitValues.size()) {
					smali.add(" # init val = ");
					printEncodedValue(smali, staticFieldInitValues.get(staticIdx++), false);
				}
			}
			smali.incIndent();
			printAnnotations(smali, fld.getValue());
			smali.decIndent();
		}
		smali.startLine();
	}

	private static void printMethod(SmaliCodeWriter smali, IMethodData mth) {
		smali.startLine()
				.startLine(mth.isDirect() ? "# direct method" : " # virtual method")
				.startLine(".method ");
		printMethodDef(smali, mth);
		smali.incIndent();
		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader != null) {
			smali.startLine(".registers ")
					.add(codeReader.getRegistersCount())
					.startLine();
			Map<Integer, String> paramMap = formatMthParamInfo(mth, smali, codeReader);
			if (paramMap.size() > 0) {
				smali.startLine();
			}
			SmaliGen smaliGen = new SmaliGen(paramMap, codeReader.getDebugInfo(), true, true);
			codeReader.visitInstructions(insn -> {
				insn.decode();
				smaliGen.format(insn);
			});
			smaliGen.gen(smali);
		}
		smali.decIndent();
		smali.startLine(".end method");
	}

	private static void printMethodDef(SmaliCodeWriter smali, IMethodData mth) {
		smali.add(AccessFlags.format(mth.getAccessFlags(), METHOD));

		IMethodRef methodRef = mth.getMethodRef();
		methodRef.load();
		smali.add(methodRef.getName());
		smali.add('(').addArgs(methodRef.getArgTypes()).add(')');
		smali.add(methodRef.getReturnType());
		if (mth.getAnnotations().size() > 0) {
			smali.incIndent();
			printAnnotations(smali, mth.getAnnotations());
			smali.decIndent();
			smali.startLine();
		}
	}

	private static Map<Integer, String> formatMthParamInfo(IMethodData mth, SmaliCodeWriter smali, ICodeReader codeReader) {
		List<String> types = mth.getMethodRef().getArgTypes();
		if (types.size() == 0) {
			return Collections.emptyMap();
		}
		int i = 0;
		int paramCount = 0;
		int paramStart = isStaticMethod(mth) ? 0 : 1;
		int regNum = getParamStartRegNum(mth);
		Map<Integer, String> paramMap = new HashMap<>(types.size());
		IDebugInfo dbgInfo = codeReader.getDebugInfo();
		if (dbgInfo != null) {
			for (ILocalVar var : dbgInfo.getLocalVars()) {
				if (var.getStartOffset() == -1) {
					smali.startLine(String.format(".param p%d, \"%s\":%s",
							paramStart + i, var.getName(), var.getType()));
					paramMap.put(regNum + i, "p" + (paramStart + i));
					paramCount++;
					i += 1;
					if (isWideType(var.getType())) {
						paramMap.put(regNum + i, "p" + (paramStart + i));
						i += 1;
					}
				}
			}
			if (paramCount + 1 == types.size()) {
				return paramMap;
			}
		}
		for (; paramCount < types.size(); paramCount++) {
			String type = types.get(paramCount);
			smali.startLine(String.format(".param p%d, \"\":%s", paramStart + i, type));
			paramMap.put(regNum + i, "p" + (paramStart + i));
			i += 1;
			if (isWideType(type)) {
				paramMap.put(regNum + i, "p" + (paramStart + i));
				i += 1;
			}
		}
		return paramMap;
	}

	private static int getParamStartRegNum(IMethodData mth) {
		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader != null) {
			int startNum = codeReader.getRegistersCount();
			if (startNum > 0) {
				for (String argType : mth.getMethodRef().getArgTypes()) {
					if (isWideType(argType)) {
						startNum -= 2;
					} else {
						startNum -= 1;
					}
				}
				if (!isStaticMethod(mth)) {
					startNum--;
				}
				return startNum;
			}
		}
		return -1;
	}

	private static boolean isWideType(String type) {
		return type.equals("D") || type.equals("J");
	}

	private static boolean isStaticMethod(IMethodData mth) {
		return (mth.getAccessFlags() & AccessFlags.STATIC) != 0;
	}

	private static void printAnnotations(SmaliCodeWriter smali, List<IAnnotation> annoList) {
		if (annoList.size() > 0) {
			for (int i = 0; i < annoList.size(); i++) {
				smali.startLine();
				printAnnotation(smali, annoList.get(i));
				if (i != annoList.size() - 1) {
					smali.startLine();
				}
			}
		}
	}

	private static void printAnnotation(SmaliCodeWriter smali, IAnnotation anno) {
		smali.add(".annotation")
				.add(" ");
		AnnotationVisibility vby = anno.getVisibility();
		if (vby != null) {
			smali.add(vby.toString().toLowerCase()).add(" ");
		}
		smali.add(anno.getAnnotationClass());
		anno.getValues().forEach((k, v) -> {
			smali.incIndent();
			smali.startLine(k).add(" = ");
			printEncodedValue(smali, v, true);
			smali.decIndent();
		});
		smali.startLine(".end annotation");
	}

	private static void printEncodedValue(SmaliCodeWriter smali, EncodedValue value, boolean wrapArray) {
		switch (value.getType()) {
			case ENCODED_ARRAY:
				smali.add("{");
				if (wrapArray) {
					smali.incIndent();
					smali.startLine();
				}
				List<EncodedValue> values = (List<EncodedValue>) value.getValue();
				for (int i = 0; i < values.size(); i++) {
					printEncodedValue(smali, values.get(i), wrapArray);
					if (i != values.size() - 1) {
						smali.add(",");
						if (wrapArray) {
							smali.startLine();
						} else {
							smali.add(" ");
						}
					}
				}
				if (wrapArray) {
					smali.decIndent();
					smali.startLine("}");
				}
				break;
			case ENCODED_STRING:
				smali.add("\"").add(value.getValue()).add("\"");
				break;
			case ENCODED_NULL:
				smali.add("null");
				break;
			case ENCODED_ANNOTATION:
				printAnnotation(smali, (IAnnotation) value.getValue());
				break;
			default:
				smali.add(value.getValue());
		}
	}

	private static final int CODE_OFFSET_COLUMN_WIDTH = 4;
	private static final int BYTECODE_COLUMN_WIDTH = 20 + 3; // 3 for ellipses.
	private static final String FMT_BYTECODE_COL = "%-" + (BYTECODE_COLUMN_WIDTH - 3) + "s";

	private static final int INSN_COL_WIDTH = "const-method-handle".length();
	private static final String FMT_INSN_COL = "%-" + INSN_COL_WIDTH + "s";
	private static final String FMT_FILE_OFFSET = "%08x:";
	private static final String FMT_CODE_OFFSET = "%04x:";
	private static final String FMT_TARGET_OFFSET = "%04x";
	private static final String FMT_GOTO = ":goto_" + FMT_TARGET_OFFSET;
	private static final String FMT_COND = ":cond_" + FMT_TARGET_OFFSET;
	private static final String FMT_DATA = ":data_" + FMT_TARGET_OFFSET;
	private static final String FMT_P_SWITCH = ":p_switch_" + FMT_TARGET_OFFSET;
	private static final String FMT_S_SWITCH = ":s_switch_" + FMT_TARGET_OFFSET;
	private static final String FMT_P_SWITCH_CASE = ":p_case_" + FMT_TARGET_OFFSET;
	private static final String FMT_S_SWITCH_CASE = ":s_case_" + FMT_TARGET_OFFSET;

	private static final String FMT_GOTO_TAG = "goto_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_COND_TAG = "cond_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_DATA_TAG = "data_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_P_SWITCH_TAG = "p_switch_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_S_SWITCH_TAG = "s_switch_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_P_SWITCH_CASE_TAG = "p_case_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_S_SWITCH_CASE_TAG = "s_case_" + FMT_TARGET_OFFSET + ":";

	public static class SmaliGen {
		static class SmaliLine {
			Object line;
			List<Entry<String, String>> tips = Collections.emptyList();

			void setLine(String str) {
				line = str;
			}

			void addLine(String str) {
				if (!(line instanceof List)) {
					line = new ArrayList<String>();
				}
				((ArrayList<String>) this.line).add(str);
			}

			void addLineTip(String tip, String extra) {
				if (tips.isEmpty()) {
					tips = new ArrayList<>();
				}
				tips.add(new AbstractMap.SimpleEntry<>(tip, extra));
			}

			private void fmtLineTip(int lineOffset, SmaliCodeWriter smali) {
				for (Entry<String, String> tip : tips) {
					int start = Math.max(0, lineOffset - tip.getKey().length());
					if (start > 0) {
						smali.add(new String(new byte[start]).replace("\0", " "));
					}
					smali.add(tip.getKey() + tip.getValue()).startLine();
				}
			}

			private void gen(int lineOffset, SmaliCodeWriter smali) {
				fmtLineTip(lineOffset, smali);
				if (line instanceof List) {
					int size = ((List<String>) line).size();
					for (int i = 0; i < size; i++) {
						smali.add(((List<String>) line).get(i));
						if (i != size - 1) {
							smali.startLine();
						}
					}
				} else {
					smali.add(line);
				}
			}
		}

		StringBuilder lineWriter = new StringBuilder(50);
		Map<Integer, SmaliLine> targetMap = new HashMap<>();
		Map<Integer, Integer> payloadOffsetMap = new HashMap<>();
		Map<Integer, String> paramMap;
		List<SmaliLine> smaliList = new ArrayList<>();
		boolean fileOffset;
		boolean bytecode;
		boolean hasDbgInfo;

		/**
		 * @param fileOffset adds file offset column to smali output
		 * @param bytecode   adds bytecode column to smali output
		 */
		public SmaliGen(Map<Integer, String> paramMap, IDebugInfo dbgInfo,
				boolean fileOffset, boolean bytecode) {
			this.fileOffset = fileOffset;
			this.bytecode = bytecode;
			this.paramMap = paramMap;
			this.hasDbgInfo = dbgInfo != null;
			if (hasDbgInfo) {
				fmtDbgInfo(dbgInfo);
			}
		}

		private boolean isParamReg(int regNum) {
			return paramMap.containsKey(regNum);
		}

		private String getRegName(int regNum) {
			String text = paramMap.get(regNum);
			if (text == null || text.isEmpty()) {
				return "v" + regNum;
			}
			return text;
		}

		public void gen(SmaliCodeWriter smali) {
			removeDupTips();
			int lineOffset = getInsnColStart();
			for (SmaliLine smaliLine : smaliList) {
				smali.startLine();
				smaliLine.gen(lineOffset, smali);
			}
		}

		public void format(InsnData insnData) {
			SmaliLine line = targetMap.computeIfAbsent(insnData.getOffset(), k -> new SmaliLine());
			smaliList.add(line);
			fmt(insnData, line);
		}

		private void fmt(InsnData insn, SmaliLine line) {
			fmtCols(insn);
			if (!fmtPayloadInsn(insn, line)) {
				fmtInsn(insn);
				line.line = lineWriter.toString();
			}
			lineWriter.delete(0, lineWriter.length());
		}

		private void fmtDbgInfo(IDebugInfo dbgInfo) {
			dbgInfo.getSourceLineMapping().forEach((codeOffset, srcLine) -> {
				if (codeOffset > -1) {
					SmaliLine line = targetMap.computeIfAbsent(codeOffset, k -> new SmaliLine());
					line.addLineTip(String.format(".line %d", srcLine), "");
				}
			});
			for (ILocalVar localVar : dbgInfo.getLocalVars()) {
				if (localVar.getStartOffset() > -1) {
					SmaliLine line = targetMap.computeIfAbsent(localVar.getStartOffset(), k -> new SmaliLine());
					line.addLineTip(String.format(".local v%d", localVar.getRegNum()),
							String.format(", \"%s\":%s", localVar.getName(), localVar.getType()));
				}
				if (localVar.getEndOffset() > -1) {
					if (isParamReg(localVar.getRegNum())) {
						return; // no need to add .end local for parameters.
					}
					SmaliLine line = targetMap.computeIfAbsent(localVar.getEndOffset(), k -> new SmaliLine());
					line.addLineTip(String.format(".end local v%d", localVar.getRegNum()),
							String.format(" # \"%s\":%s", localVar.getName(), localVar.getType()));
				}
			}
		}

		private void fmtInsn(InsnData insn) {
			int opcode = insn.getRawOpcodeUnit();
			opcode = opcode & 0xff;
			String mne = DexOpcodes.MNEMONICS[opcode];
			lineWriter.append(String.format(FMT_INSN_COL, mne)).append(" ");
			fmtRegs(opcode, insn, lineWriter);
			if (hasTarget(opcode)) {
				if (isGotoIns(opcode)) {
					lineWriter.append(String.format(FMT_GOTO, insn.getTarget()));
					addTarget(FMT_GOTO_TAG, insn.getTarget());
					return;
				}
				lineWriter.append(", ");
				if (isConditionIns(opcode)) {
					lineWriter.append(String.format(FMT_COND, insn.getTarget()));
					addTarget(FMT_COND_TAG, insn.getTarget());

				} else if (opcode == DexOpcodes.PACKED_SWITCH) {
					payloadOffsetMap.put(insn.getTarget(), insn.getOffset());
					lineWriter.append(String.format(FMT_P_SWITCH, insn.getTarget()));
					addTarget(FMT_P_SWITCH_TAG, insn.getTarget());

				} else if (opcode == DexOpcodes.SPARSE_SWITCH) {
					payloadOffsetMap.put(insn.getTarget(), insn.getOffset());
					lineWriter.append(String.format(FMT_S_SWITCH, insn.getTarget()));
					addTarget(FMT_S_SWITCH_TAG, insn.getTarget());

				} else {
					lineWriter.append(String.format(FMT_DATA, insn.getTarget()));
					addTarget(FMT_DATA_TAG, insn.getTarget());
				}
				return;
			}
			if (isInvokeIns(opcode)) {
				lineWriter.append(", ").append(method(insn));
				return;
			}
			if (insn.getIndexType() == InsnIndexType.TYPE_REF) {
				lineWriter.append(", ").append(type(insn));
				return;
			}
			if (insn.getIndexType() == InsnIndexType.FIELD_REF) {
				lineWriter.append(", ").append(field(insn));
				return;
			}
			if (insn.getIndexType() == InsnIndexType.STRING_REF) {
				lineWriter.append(", ").append(str(insn));
				return;
			}
			if (hasLiteral(opcode)) {
				lineWriter.append(", ").append(literal(insn, opcode));
				return;
			}
			if (opcode == DexOpcodes.CONST_METHOD_HANDLE) {
				lineWriter.append(", ").append(methodHandle(insn));
				return;
			}
			if (opcode == DexOpcodes.CONST_METHOD_TYPE) {
				lineWriter.append(", ").append(proto(insn, insn.getIndex()));
				return;
			}
		}

		private void addTarget(String fmtTag, int target) {
			addTarget(fmtTag, target, "");
		}

		private void addTarget(String fmtTag, int target, String extraTip) {
			targetMap.computeIfAbsent(target, k -> new SmaliLine())
					.addLineTip(String.format(fmtTag, target), extraTip);
		}

		private void fmtRegs(int opcode, InsnData insn, StringBuilder smali) {
			boolean appendBrace = isRegList(opcode);
			if (appendBrace) {
				smali.append("{");
			}
			if (isRangeRegIns(opcode)) {
				smali.append(getRegName(insn.getReg(0)))
						.append(" .. ")
						.append(getRegName(insn.getReg(insn.getRegsCount() - 1)));

			} else if (insn.getRegsCount() > 0) {
				for (int i = 0; i < insn.getRegsCount(); i++) {
					if (i > 0) {
						smali.append(", ");
					}
					smali.append(getRegName(insn.getReg(i)));
				}
			}
			if (appendBrace) {
				smali.append("}");
			}
		}

		private boolean fmtPayloadInsn(InsnData insn, SmaliLine line) {
			int opcode = insn.getRawOpcodeUnit();
			if (opcode == DexOpcodes.PACKED_SWITCH_PAYLOAD) {
				lineWriter.append("packed-switch-payload");
				line.addLine(lineWriter.toString());
				DexSwitchPayload payload = (DexSwitchPayload) insn.getPayload();
				if (payload != null) {
					fmtSwitchPayload(FMT_P_SWITCH_CASE, FMT_P_SWITCH_CASE_TAG, line, payload, insn.getOffset());
				}
				return true;
			}
			if (opcode == DexOpcodes.SPARSE_SWITCH_PAYLOAD) {
				lineWriter.append("sparse-switch-payload");
				line.addLine(lineWriter.toString());
				DexSwitchPayload payload = (DexSwitchPayload) insn.getPayload();
				if (payload != null) {
					fmtSwitchPayload(FMT_S_SWITCH_CASE, FMT_S_SWITCH_CASE_TAG, line, payload, insn.getOffset());
				}
				return true;
			}
			if (opcode == DexOpcodes.FILL_ARRAY_DATA_PAYLOAD) {
				lineWriter.append("fill-array-data-payload");
				line.setLine(lineWriter.toString());
				return true;
			}
			return false;
		}

		private void fmtSwitchPayload(String fmtTarget, String fmtTag, SmaliLine line,
				DexSwitchPayload payload, int curOffset) {
			int lineStart = getInsnColStart();
			lineStart += CODE_OFFSET_COLUMN_WIDTH + 1 + 1; // plus 1s for space and the ':'
			String basicIndent = new String(new byte[lineStart]).replace("\0", " ");
			String indent = SmaliCodeWriter.INDENT_STR + basicIndent;
			int[] keys = payload.getKeys();
			int[] targets = payload.getTargets();
			int opcodeOffset = payloadOffsetMap.get(curOffset);
			for (int i = 0; i < keys.length; i++) {
				int target = opcodeOffset + targets[i];
				line.addLine(String.format("%scase %d: -> " + fmtTarget, indent, keys[i], target));
				addTarget(fmtTag, target, String.format(" # case %d", keys[i]));
			}
			line.addLine(basicIndent + ".end payload");
		}

		private void removeDupTips() {
			List<Entry<Integer, Entry<String, String>>> dbgLines = null; // line num: tip
			if (hasDbgInfo) {
				dbgLines = new ArrayList<>();
			}
			for (int i = 0; i < smaliList.size(); i++) {
				SmaliLine line = smaliList.get(i);
				Map<String, Integer> tipSet = Collections.emptyMap(); // tip: reference count
				for (Iterator<Entry<String, String>> it = line.tips.iterator(); it.hasNext();) {
					Entry<String, String> tip = it.next();
					if (hasDbgInfo && removeDupSourceLine(tip, i, dbgLines)) { // debug info source line.
						it.remove();
						continue;
					}
					if (tipSet.containsKey(tip.getKey())) { // remove dup tips like cond_:/goto_:.
						it.remove();
						tipSet.computeIfPresent(tip.getKey(), (k, v) -> v + 1);
					} else {
						if (tipSet.isEmpty()) {
							tipSet = new HashMap<>();
						}
						tipSet.computeIfAbsent(tip.getKey(), k -> 1);
					}
				}
				tipSet.forEach((k, v) -> {
					if (v > 1) {
						for (int j = 0; j < line.tips.size(); j++) {
							if (line.tips.get(j).getKey().equals(k)) {
								line.tips.set(j, new AbstractMap.SimpleEntry<>(k, " # " + v + " refs"));
							}
						}
					}
				});
			}
		}

		private boolean removeDupSourceLine(Entry<String, String> tip, int i,
				List<Entry<Integer, Entry<String, String>>> dbgLines) {
			boolean removeIt = false;
			if (tip.getKey().startsWith(".line ")) { // debug info source line.
				if (dbgLines.size() > 0) {
					Entry<Integer, Entry<String, String>> entry = dbgLines.get(dbgLines.size() - 1);
					if (i - entry.getKey() == 1 && entry.getValue().getKey().equals(tip.getKey())) {
						removeIt = true; // duplicated.
					}
				}
				dbgLines.add(new AbstractMap.SimpleEntry<>(i, tip));
			}
			return removeIt;
		}

		private int getInsnColStart() {
			int start = 0;
			if (fileOffset) {
				start += 8 + 1 + 1; // plus 1s for space and the ':'
			}
			if (bytecode) {
				start += BYTECODE_COLUMN_WIDTH + 1; // plus 1 for space
			}
			return start;
		}

		private void fmtCols(InsnData insn) {
			if (fileOffset) {
				lineWriter.append(String.format(FMT_FILE_OFFSET + " ", insn.getFileOffset()));
			}
			if (bytecode) {
				formatByteCode(lineWriter, insn.getByteCode());
				lineWriter.append(" ");
				lineWriter.append(String.format(FMT_CODE_OFFSET + " ", insn.getOffset()));
			}
		}

		private static void formatByteCode(StringBuilder smali, byte[] bytes) {
			int maxLen = Math.min(bytes.length, 4 * 2); // limit to 4 units
			StringBuilder inHex = new StringBuilder();
			for (int i = 0; i < maxLen; i++) {
				int temp = ((bytes[i++] & 0xff) << 8) | (bytes[i] & 0xff);
				inHex.append(String.format("%04x ", temp));
			}
			smali.append(String.format(FMT_BYTECODE_COL, inHex));
			if (maxLen < bytes.length) {
				smali.append("...");
			} else {
				smali.append("   ");
			}
		}

		private static String literal(InsnData insn, int opcode) {
			long it = insn.getLiteral();
			String tip = "";
			if (it > Integer.MAX_VALUE) {
				if (isWideIns(opcode)) {
					tip = " # double: " + Double.longBitsToDouble(it);
				} else if (opcode == DexOpcodes.CONST_HIGH16) {
					tip = " # float: " + Float.intBitsToFloat((int) it);
				}
			} else if (it <= 0) {
				return "" + it + tip;
			}
			return "0x" + Long.toHexString(it) + tip;
		}

		private static String str(InsnData insn) {
			return String.format("\"%s\" # string@%04x",
					insn.getIndexAsString()
							.replace("\n", "\\n")
							.replace("\t", "\\t"),
					insn.getIndex());
		}

		private static String type(InsnData insn) {
			return String.format("%s # type@%04x", insn.getIndexAsType(), insn.getIndex());
		}

		private static String field(InsnData insn) {
			return String.format("%s # field@%04x", insn.getIndexAsField().toString(), insn.getIndex());
		}

		private static String method(InsnData insn) {
			int rawOpcodeUnit = insn.getRawOpcodeUnit();
			int opcode = rawOpcodeUnit & 0xFF;
			if (opcode == DexOpcodes.INVOKE_CUSTOM || opcode == DexOpcodes.INVOKE_CUSTOM_RANGE) {
				insn.getIndexAsCallSite().load();
				return String.format("%s # call_site@%04x", insn.getIndexAsCallSite().toString(), insn.getIndex());
			}
			IMethodRef mthRef = insn.getIndexAsMethod();
			mthRef.load();
			if (opcode == DexOpcodes.INVOKE_POLYMORPHIC || opcode == DexOpcodes.INVOKE_POLYMORPHIC_RANGE) {
				return String.format("%s, %s # method@%04x, proto@%04x",
						mthRef.toString(), insn.getIndexAsProto(insn.getTarget()).toString(),
						insn.getIndex(), insn.getTarget());
			}
			return String.format("%s # method@%04x", mthRef.toString(), insn.getIndex());
		}

		private static String proto(InsnData insn, int protoIndex) {
			return String.format("%s # proto@%04x", insn.getIndexAsProto(protoIndex).toString(), protoIndex);
		}

		private static String methodHandle(InsnData insn) {
			return String.format("%s # method_handle@%04x",
					insn.getIndexAsMethodHandle().toString(), insn.getIndex());
		}

		private static boolean isGotoIns(int opcode) {
			return opcode >= DexOpcodes.GOTO && opcode <= DexOpcodes.GOTO_32;
		}

		private static boolean isInvokeIns(int opcode) {
			return (opcode >= DexOpcodes.INVOKE_VIRTUAL && opcode <= DexOpcodes.INVOKE_INTERFACE)
					|| (opcode >= DexOpcodes.INVOKE_VIRTUAL_RANGE && opcode <= DexOpcodes.INVOKE_INTERFACE_RANGE)
					|| (opcode >= DexOpcodes.INVOKE_POLYMORPHIC && opcode <= DexOpcodes.INVOKE_CUSTOM_RANGE);
		}

		private static boolean isRangeRegIns(int opcode) {
			if (opcode >= DexOpcodes.INVOKE_VIRTUAL_RANGE && opcode <= DexOpcodes.INVOKE_INTERFACE_RANGE) {
				return true;
			}
			switch (opcode) {
				case DexOpcodes.FILLED_NEW_ARRAY_RANGE:
				case DexOpcodes.INVOKE_CUSTOM_RANGE:
				case DexOpcodes.INVOKE_POLYMORPHIC_RANGE:
					return true;
			}
			return false;
		}

		private static boolean isWideIns(int opcode) {
			return (opcode >= DexOpcodes.CONST_WIDE_16 && opcode <= DexOpcodes.CONST_WIDE_HIGH16);
		}

		private static boolean hasLiteral(int opcode) {
			return (opcode >= DexOpcodes.CONST_4 && opcode <= DexOpcodes.CONST_WIDE_HIGH16)
					|| (opcode >= DexOpcodes.ADD_INT_LIT16 && opcode <= DexOpcodes.USHR_INT_LIT8);
		}

		private static boolean isConditionIns(int opcode) {
			return opcode >= DexOpcodes.IF_EQ && opcode <= DexOpcodes.IF_LEZ;
		}

		private static boolean hasTarget(int opcode) {
			return (opcode >= DexOpcodes.IF_EQ && opcode <= DexOpcodes.IF_LEZ)
					|| (opcode >= DexOpcodes.GOTO && opcode <= DexOpcodes.SPARSE_SWITCH)
					|| (opcode == DexOpcodes.FILL_ARRAY_DATA);
		}

		private static boolean isRegList(int opcode) {
			return isInvokeIns(opcode)
					|| (opcode >= DexOpcodes.FILLED_NEW_ARRAY && opcode <= DexOpcodes.FILLED_NEW_ARRAY_RANGE);
		}
	}
}
