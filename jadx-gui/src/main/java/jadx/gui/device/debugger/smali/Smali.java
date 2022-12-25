package jadx.gui.device.debugger.smali;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.AccessFlagsScope;
import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IDebugInfo;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.ILocalVar;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.ITry;
import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.AnnotationsAttr;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.InsnIndexType;
import jadx.api.plugins.input.insns.Opcode;
import jadx.api.plugins.input.insns.custom.ISwitchPayload;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.attributes.AttributeStorage;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnDecoder;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.api.plugins.input.data.AccessFlagsScope.FIELD;
import static jadx.api.plugins.input.data.AccessFlagsScope.METHOD;
import static jadx.api.plugins.input.insns.Opcode.CONST;
import static jadx.api.plugins.input.insns.Opcode.CONST_METHOD_HANDLE;
import static jadx.api.plugins.input.insns.Opcode.CONST_METHOD_TYPE;
import static jadx.api.plugins.input.insns.Opcode.CONST_WIDE;
import static jadx.api.plugins.input.insns.Opcode.FILLED_NEW_ARRAY;
import static jadx.api.plugins.input.insns.Opcode.FILLED_NEW_ARRAY_RANGE;
import static jadx.api.plugins.input.insns.Opcode.FILL_ARRAY_DATA_PAYLOAD;
import static jadx.api.plugins.input.insns.Opcode.INVOKE_CUSTOM;
import static jadx.api.plugins.input.insns.Opcode.INVOKE_CUSTOM_RANGE;
import static jadx.api.plugins.input.insns.Opcode.INVOKE_POLYMORPHIC;
import static jadx.api.plugins.input.insns.Opcode.INVOKE_POLYMORPHIC_RANGE;
import static jadx.api.plugins.input.insns.Opcode.PACKED_SWITCH;
import static jadx.api.plugins.input.insns.Opcode.PACKED_SWITCH_PAYLOAD;
import static jadx.api.plugins.input.insns.Opcode.SPARSE_SWITCH;
import static jadx.api.plugins.input.insns.Opcode.SPARSE_SWITCH_PAYLOAD;

public class Smali {
	private static final Logger LOG = LoggerFactory.getLogger(Smali.class);

	private static SmaliInsnDecoder insnDecoder = null;

	private ICodeInfo codeInfo;
	private final Map<String, SmaliMethodNode> insnMap = new HashMap<>(); // fullRawId of method as key

	private final boolean printFileOffset = true;
	private final boolean printBytecode = true;

	private boolean isJavaBytecode;

	private Smali() {
	}

	public static Smali disassemble(ClassNode cls) {
		cls = cls.getTopParentClass();
		SmaliWriter code = new SmaliWriter(cls);
		Smali smali = new Smali();
		smali.isJavaBytecode = cls.getInputFileName().endsWith(".class"); // TODO: add flag to api
		smali.writeClass(code, cls);
		smali.codeInfo = code.finish();
		return smali;
	}

	public String getCode() {
		return codeInfo.getCodeStr();
	}

	public int getMethodDefPos(String mthFullRawID) {
		SmaliMethodNode info = insnMap.get(mthFullRawID);
		if (info != null) {
			return info.getDefPos();
		}
		return -1;
	}

	public int getRegCount(String mthFullRawID) {
		SmaliMethodNode info = insnMap.get(mthFullRawID);
		if (info != null) {
			return info.getRegCount();
		}
		return -1;
	}

	public int getParamRegStart(String mthFullRawID) {
		SmaliMethodNode info = insnMap.get(mthFullRawID);
		if (info != null) {
			return info.getParamRegStart();
		}
		return -1;
	}

	public int getInsnPosByCodeOffset(String mthFullRawID, long codeOffset) {
		SmaliMethodNode info = insnMap.get(mthFullRawID);
		if (info != null) {
			return info.getInsnPos(codeOffset);
		}
		return -1;
	}

	@Nullable
	public Entry<String, Integer> getMthFullIDAndCodeOffsetByLine(int line) {
		for (Entry<String, SmaliMethodNode> entry : insnMap.entrySet()) {
			Integer codeOffset = entry.getValue().getLineMapping().get(line);
			if (codeOffset != null) {
				return new SimpleEntry<>(entry.getKey(), codeOffset);
			}
		}
		return null;
	}

	public List<SmaliRegister> getRegisterList(String mthFullRawID) {
		SmaliMethodNode node = insnMap.get(mthFullRawID);
		if (node != null) {
			return node.getRegList();
		}
		return Collections.emptyList();
	}

	/**
	 * @return null for no result, FieldInfo for field, Integer for register.
	 */
	@Nullable
	public Object getResultRegOrField(String mthFullRawID, long codeOffset) {
		SmaliMethodNode info = insnMap.get(mthFullRawID);
		if (info != null) {
			InsnNode insn = info.getInsnNode(codeOffset);
			if (insn != null) {
				if (insn.getType() == InsnType.IPUT) {
					return ((IndexInsnNode) insn).getIndex();
				}
				if (insn.getType() == InsnType.INVOKE) {
					if (insn instanceof InvokeNode) {
						if (insn.getArgsCount() > 0) {
							return ((RegisterArg) insn.getArg(0)).getRegNum();
						}
					}
				}
				RegisterArg regArg = insn.getResult();
				if (regArg != null) {
					return regArg.getRegNum();
				}
			}
		}
		return null;
	}

	private void writeClass(SmaliWriter smali, ClassNode cls) {
		IClassData clsData = cls.getClsData();
		if (clsData == null) {
			smali.startLine(String.format("###### Class %s is created by jadx", cls.getFullName()));
			return;
		}
		AttributeStorage attributes = new AttributeStorage();
		attributes.add(clsData.getAttributes());

		smali.startLine("Class: " + clsData.getType())
				.startLine("AccessFlags: " + AccessFlags.format(clsData.getAccessFlags(), AccessFlagsScope.CLASS))
				.startLine("SuperType: " + clsData.getSuperType())
				.startLine("Interfaces: " + clsData.getInterfacesTypes())
				.startLine("SourceFile: " + attributes.get(JadxAttrType.SOURCE_FILE));

		AnnotationsAttr annotationsAttr = attributes.get(JadxAttrType.ANNOTATION_LIST);
		if (annotationsAttr != null) {
			Collection<IAnnotation> annos = annotationsAttr.getList();
			if (!annos.isEmpty()) {
				smali.startLine(String.format("# %d annotations", annos.size()));
				writeAnnotations(smali, new ArrayList<>(annos));
				smali.startLine();
			}
		}

		List<RawField> fields = new ArrayList<>();
		int[] colWidths = new int[] { 0, 0 }; // first is access flag, second is name
		int[] mthIndex = new int[] { 0 };
		LineInfo line = new LineInfo();
		clsData.visitFieldsAndMethods(
				f -> {
					RawField fld = RawField.make(f);
					fields.add(fld);
					if (fld.accessFlag.length() > colWidths[0]) {
						colWidths[0] = fld.accessFlag.length();
					}
					if (fld.name.length() > colWidths[1]) {
						colWidths[1] = fld.name.length();
					}
				},
				m -> {
					if (!fields.isEmpty()) {
						writeFields(smali, clsData, fields, colWidths);
						fields.clear();
					}
					try {
						writeMethod(smali, cls.getMethods().get(mthIndex[0]++), m, line);
					} catch (Throwable e) {
						IMethodRef methodRef = m.getMethodRef();
						String mthFullName = methodRef.getParentClassType() + "->" + methodRef.getName();
						smali.setIndent(0);
						smali.startLine("Failed to write method: " + mthFullName + "\n" + Utils.getStackTrace(e));
						LOG.error("Failed to write smali code for method: {}", mthFullName, e);
					}
					line.reset();
				});

		if (!fields.isEmpty()) { // in case there's no methods.
			writeFields(smali, clsData, fields, colWidths);
		}
		for (ClassNode innerClass : cls.getInnerClasses()) {
			writeClass(smali, innerClass);
		}
	}

	private void writeFields(SmaliWriter smali, IClassData classData, List<RawField> fields, int[] colWidths) {
		int staticIdx = 0;
		smali.startLine().startLine("# fields");
		String whites = new String(new byte[Math.max(colWidths[0], colWidths[1])]).replace("\0", " ");
		for (RawField fld : fields) {
			smali.startLine();
			int pad = colWidths[0] - fld.accessFlag.length();
			if (pad > 0) {
				fld.accessFlag += whites.substring(0, pad);
			}
			smali.add(".field ").add(fld.accessFlag);
			pad = colWidths[1] - fld.name.length();
			if (pad > 0) {
				fld.name += whites.substring(0, pad);
			}
			smali.add(fld.name).add(" ");
			smali.add(": ").add(fld.type);
			if (fld.isStatic) {
				EncodedValue constVal = fld.attributes.get(JadxAttrType.CONSTANT_VALUE);
				if (constVal != null) {
					smali.add(" # init val = ");
					writeEncodedValue(smali, constVal, false);
				}
			}
			AnnotationsAttr annotationsAttr = fld.attributes.get(JadxAttrType.ANNOTATION_LIST);
			if (annotationsAttr != null) {
				smali.incIndent();
				writeAnnotations(smali, annotationsAttr.getList());
				smali.decIndent();
			}
		}
		smali.startLine();
	}

	private void writeMethod(SmaliWriter smali, MethodNode methodNode, IMethodData mth, LineInfo line) {
		if (insnDecoder == null) {
			insnDecoder = new SmaliInsnDecoder(methodNode);
		}
		smali.startLine().startLine(".method ");
		writeMethodDef(smali, mth, line);
		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader != null) {
			int regsCount = codeReader.getRegistersCount();
			line.smaliMthNode.setParamRegStart(getParamStartRegNum(mth));
			line.smaliMthNode.setRegCount(regsCount);
			Map<Long, InsnNode> nodes = new HashMap<>(codeReader.getUnitsCount() / 2);
			line.smaliMthNode.setInsnNodes(nodes, codeReader.getUnitsCount());
			line.smaliMthNode.initRegInfoList(regsCount, codeReader.getUnitsCount());

			smali.incIndent();
			smali.startLine(".registers ").add(Integer.toString(regsCount));

			writeTries(codeReader, line);
			IDebugInfo debugInfo = codeReader.getDebugInfo();
			List<ILocalVar> localVars = debugInfo != null ? debugInfo.getLocalVars() : Collections.emptyList();
			formatMthParamInfo(mth, smali, line, regsCount, localVars);
			if (debugInfo != null) {
				formatDbgInfo(debugInfo, localVars, line);
			}
			smali.newLine();
			smali.startLine();
			// first pass to fill payload offsets for switch instructions
			codeReader.visitInstructions(insn -> {
				Opcode opcode = insn.getOpcode();
				if (opcode == PACKED_SWITCH || opcode == SPARSE_SWITCH) {
					insn.decode();
					line.addPayloadOffset(insn.getOffset(), insn.getTarget());
				}
			});
			codeReader.visitInstructions(insn -> {
				InsnNode node = decodeInsn(insn, line);
				nodes.put((long) insn.getOffset(), node);
			});
			line.write(smali);
			insnMap.put(methodNode.getMethodInfo().getRawFullId(), line.smaliMthNode);

			smali.decIndent();
		}
		smali.startLine(".end method");
	}

	private void writeTries(ICodeReader codeReader, LineInfo line) {
		List<ITry> tries = codeReader.getTries();
		for (ITry aTry : tries) {
			int end = aTry.getEndOffset();
			String tryEndTip = String.format(FMT_TRY_END_TAG, end);
			String tryStartTip = String.format(FMT_TRY_TAG, aTry.getStartOffset());
			String tryStartTipExtra = " # :" + tryStartTip.substring(0, tryStartTip.length() - 1);

			line.addTip(aTry.getStartOffset(), tryStartTip, " # :" + tryEndTip.substring(0, tryEndTip.length() - 1));
			line.addTip(end, tryEndTip, tryStartTipExtra);

			ICatch iCatch = aTry.getCatch();
			int[] addresses = iCatch.getHandlers();
			int addr;
			for (int i = 0; i < addresses.length; i++) {
				addr = addresses[i];
				String catchTip = String.format(FMT_CATCH_TAG, addr);
				line.addTip(addr, catchTip, " # " + iCatch.getTypes()[i]);
				line.addTip(addr, catchTip, tryStartTipExtra);
				line.addTip(aTry.getStartOffset(), tryStartTip, " # :" + catchTip.substring(0, catchTip.length() - 1));
			}
			addr = iCatch.getCatchAllHandler();
			if (addr > -1) {
				String catchAllTip = String.format(FMT_CATCH_ALL_TAG, addr);
				line.addTip(addr, catchAllTip, tryStartTipExtra);
				line.addTip(aTry.getStartOffset(), tryStartTip, " # :" + catchAllTip.substring(0, catchAllTip.length() - 1));
			}
		}
	}

	private InsnNode decodeInsn(InsnData insn, LineInfo lineInfo) {
		insn.decode();
		InsnNode node = insnDecoder.decode(insn);
		formatInsn(insn, node, lineInfo);
		return node;
	}

	private void formatInsn(InsnData insn, InsnNode node, LineInfo line) {
		StringBuilder lw = line.getLineWriter();
		lw.delete(0, lw.length());
		fmtCols(insn, line);
		if (fmtPayloadInsn(insn, line)) {
			return;
		}
		lw.append(formatInsnName(insn)).append(" ");
		fmtRegs(insn, node.getType(), line);
		if (!tryFormatTargetIns(insn, node.getType(), line)) {
			if (hasLiteral(insn)) {
				lw.append(", ").append(literal(insn));
			} else if (node.getType() == InsnType.INVOKE) {
				lw.append(", ").append(method(insn));
			} else if (insn.getIndexType() == InsnIndexType.FIELD_REF) {
				lw.append(", ").append(field(insn));
			} else if (insn.getIndexType() == InsnIndexType.STRING_REF) {
				lw.append(", ").append(str(insn));
			} else if (insn.getIndexType() == InsnIndexType.TYPE_REF) {
				lw.append(", ").append(type(insn));
			} else if (insn.getOpcode() == CONST_METHOD_HANDLE) {
				lw.append(", ").append(methodHandle(insn));
			} else if (insn.getOpcode() == CONST_METHOD_TYPE) {
				lw.append(", ").append(proto(insn, insn.getIndex()));
			}
		}
		line.addInsnLine(insn.getOffset(), lw.toString());
	}

	private String formatInsnName(InsnData insn) {
		if (isJavaBytecode) {
			// add api opcode, because registers not used
			return String.format("%-" + INSN_COL_WIDTH + "s | %-15s",
					insn.getOpcodeMnemonic(), insn.getOpcode().name().toLowerCase(Locale.ROOT).replace('_', '-'));
		}
		return String.format(FMT_INSN_COL, insn.getOpcodeMnemonic());
	}

	private boolean tryFormatTargetIns(InsnData insn, InsnType insnType, LineInfo line) {
		switch (insnType) {
			case IF: {
				int target = insn.getTarget();
				line.addTip(target, String.format(FMT_COND_TAG, target), "");
				line.getLineWriter().append(", ").append(String.format(FMT_COND, target));
				return true;
			}
			case GOTO: {
				int target = insn.getTarget();
				line.addTip(target, String.format(FMT_GOTO_TAG, target), "");
				line.getLineWriter().append(String.format(FMT_GOTO, target));
				return true;
			}
			case FILL_ARRAY: {
				int target = insn.getTarget();
				line.addTip(target, String.format(FMT_DATA_TAG, target), "");
				line.getLineWriter().append(", ").append(String.format(FMT_DATA, target));
				return true;
			}
			case SWITCH: {
				int target = insn.getTarget();
				if (insn.getOpcode() == Opcode.PACKED_SWITCH) {
					line.addTip(target, String.format(FMT_P_SWITCH_TAG, target), "");
					line.getLineWriter().append(", ").append(String.format(FMT_P_SWITCH, target));
				} else {
					line.addTip(target, String.format(FMT_S_SWITCH_TAG, target), "");
					line.getLineWriter().append(", ").append(String.format(FMT_S_SWITCH, target));
				}
				return true;
			}
		}
		return false;
	}

	private static boolean hasStaticFlag(int flag) {
		return (flag & AccessFlags.STATIC) != 0;
	}

	private void writeMethodDef(SmaliWriter smali, IMethodData mth, LineInfo lineInfo) {
		smali.add(AccessFlags.format(mth.getAccessFlags(), METHOD));

		IMethodRef methodRef = mth.getMethodRef();
		methodRef.load();
		lineInfo.smaliMthNode.setDefPos(smali.getLength());
		smali.add(methodRef.getName())
				.add('(');
		methodRef.getArgTypes().forEach(smali::add);
		smali.add(')');
		smali.add(methodRef.getReturnType());

		AnnotationsAttr annotationsAttr = new AttributeStorage(mth.getAttributes()).get(JadxAttrType.ANNOTATION_LIST);
		if (annotationsAttr != null && !annotationsAttr.isEmpty()) {
			smali.incIndent();
			writeAnnotations(smali, annotationsAttr.getList());
			smali.decIndent();
			smali.startLine();
		}
	}

	private void formatMthParamInfo(IMethodData mth, SmaliWriter smali, LineInfo line,
			int regsCount, List<ILocalVar> localVars) {
		List<String> types = mth.getMethodRef().getArgTypes();
		if (types.isEmpty()) {
			return;
		}
		int paramStart = 0;
		int regNum = line.smaliMthNode.getParamRegStart();
		if (!hasStaticFlag(mth.getAccessFlags())) {
			// add 'this' register
			line.addRegName(regNum, "p0");
			line.smaliMthNode.setParamReg(regNum, "p0");
			regNum++;
			paramStart++;
		}
		if (localVars.isEmpty()) {
			return;
		}
		ILocalVar[] params = new ILocalVar[regsCount];
		for (ILocalVar var : localVars) {
			if (var.isMarkedAsParameter()) {
				params[var.getRegNum()] = var;
			}
		}
		smali.newLine();
		for (String paramType : types) {
			ILocalVar param = params[regNum];
			if (param != null) {
				String name = Utils.getOrElse(param.getName(), "");
				String type = Utils.getOrElse(param.getSignature(), paramType);
				String varName = "p" + paramStart;
				smali.startLine(String.format(".param %s, \"%s\" # %s", varName, name, type));
				line.addRegName(regNum, varName);
				line.smaliMthNode.setParamReg(regNum, varName);
			}
			int regSize = isWideType(paramType) ? 2 : 1;
			regNum += regSize;
			paramStart += regSize;
		}
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
				if (!hasStaticFlag(mth.getAccessFlags())) {
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

	private void writeAnnotations(SmaliWriter smali, List<IAnnotation> annoList) {
		if (annoList.size() > 0) {
			for (int i = 0; i < annoList.size(); i++) {
				smali.startLine();
				writeAnnotation(smali, annoList.get(i));
				if (i != annoList.size() - 1) {
					smali.startLine();
				}
			}
		}
	}

	private void writeAnnotation(SmaliWriter smali, IAnnotation anno) {
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
			writeEncodedValue(smali, v, true);
			smali.decIndent();
		});
		smali.startLine(".end annotation");
	}

	private void formatDbgInfo(IDebugInfo dbgInfo, List<ILocalVar> localVars, LineInfo line) {
		dbgInfo.getSourceLineMapping().forEach((codeOffset, srcLine) -> {
			if (codeOffset > -1) {
				line.addDebugLineTip(codeOffset, String.format(".line %d", srcLine), "");
			}
		});
		for (ILocalVar localVar : localVars) {
			if (localVar.isMarkedAsParameter()) {
				continue;
			}
			String type = localVar.getType();
			String sign = localVar.getSignature();
			String longTypeStr;
			if (sign == null || sign.trim().isEmpty()) {
				longTypeStr = String.format(", \"%s\":%s", localVar.getName(), type);
			} else {
				longTypeStr = String.format(", \"%s\":%s, \"%s\"", localVar.getName(), type, localVar.getSignature());
			}
			line.addTip(
					localVar.getStartOffset(),
					".local " + formatVarName(line.smaliMthNode, localVar),
					longTypeStr);
			line.addTip(
					localVar.getEndOffset(),
					".end local " + formatVarName(line.smaliMthNode, localVar),
					String.format(" # \"%s\":%s", localVar.getName(), type));
		}
	}

	private String formatVarName(SmaliMethodNode smaliMthNode, ILocalVar localVar) {
		int paramRegStart = smaliMthNode.getParamRegStart();
		int regNum = localVar.getRegNum();
		if (regNum < paramRegStart) {
			return "v" + regNum;
		}
		return "p" + (regNum - paramRegStart);
	}

	private void writeEncodedValue(SmaliWriter smali, EncodedValue value, boolean wrapArray) {
		switch (value.getType()) {
			case ENCODED_ARRAY:
				smali.add("{");
				if (wrapArray) {
					smali.incIndent();
					smali.startLine();
				}
				List<EncodedValue> values = (List<EncodedValue>) value.getValue();
				for (int i = 0; i < values.size(); i++) {
					writeEncodedValue(smali, values.get(i), wrapArray);
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
			case ENCODED_NULL:
				smali.add("null");
				break;
			case ENCODED_ANNOTATION:
				writeAnnotation(smali, (IAnnotation) value.getValue());
				break;
			case ENCODED_BYTE:
				smali.add(TypeGen.formatByte((Byte) value.getValue(), false));
				break;
			case ENCODED_SHORT:
				smali.add(TypeGen.formatShort((Short) value.getValue(), false));
				break;
			case ENCODED_CHAR:
				smali.add(smali.getClassNode().root().getStringUtils().unescapeChar((Character) value.getValue()));
				break;
			case ENCODED_INT:
				smali.add(TypeGen.formatInteger((Integer) value.getValue(), false));
				break;
			case ENCODED_LONG:
				smali.add(TypeGen.formatLong((Long) value.getValue(), false));
				break;
			case ENCODED_FLOAT:
				smali.add(TypeGen.formatFloat((Float) value.getValue()));
				break;
			case ENCODED_DOUBLE:
				smali.add(TypeGen.formatDouble((Double) value.getValue()));
				break;
			case ENCODED_STRING:
				smali.add(smali.getClassNode().root().getStringUtils().unescapeString((String) value.getValue()));
				break;
			case ENCODED_TYPE:
				smali.add(ArgType.parse((String) value.getValue()) + ".class");
				break;
			default:
				smali.add(String.valueOf(value.getValue()));
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
	private static final String FMT_DATA = ":array_" + FMT_TARGET_OFFSET;
	private static final String FMT_P_SWITCH = ":p_switch_" + FMT_TARGET_OFFSET;
	private static final String FMT_S_SWITCH = ":s_switch_" + FMT_TARGET_OFFSET;
	private static final String FMT_P_SWITCH_CASE = ":p_case_" + FMT_TARGET_OFFSET;
	private static final String FMT_S_SWITCH_CASE = ":s_case_" + FMT_TARGET_OFFSET;

	private static final String FMT_TRY_TAG = "try_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_TRY_END_TAG = "try_end_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_CATCH_TAG = "catch_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_CATCH_ALL_TAG = "catch_all_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_GOTO_TAG = "goto_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_COND_TAG = "cond_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_DATA_TAG = "array_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_P_SWITCH_TAG = "p_switch_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_S_SWITCH_TAG = "s_switch_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_P_SWITCH_CASE_TAG = "p_case_" + FMT_TARGET_OFFSET + ":";
	private static final String FMT_S_SWITCH_CASE_TAG = "s_case_" + FMT_TARGET_OFFSET + ":";

	private void fmtRegs(InsnData insn, InsnType insnType, LineInfo line) {
		boolean appendBrace = insnType == InsnType.INVOKE || isRegList(insn);
		StringBuilder lw = line.getLineWriter();
		if (insnType == InsnType.INVOKE) {
			int resultReg = insn.getResultReg();
			if (resultReg != -1) {
				lw.append(line.getRegName(resultReg)).append(" <= ");
			}
		}
		if (appendBrace) {
			lw.append("{");
		}
		if (isRangeRegIns(insn)) {
			lw.append(line.getRegName(insn.getReg(0)))
					.append(" .. ")
					.append(line.getRegName(insn.getReg(insn.getRegsCount() - 1)));
		} else if (insn.getRegsCount() > 0) {
			for (int i = 0; i < insn.getRegsCount(); i++) {
				if (i > 0) {
					lw.append(", ");
				}
				lw.append(line.getRegName(insn.getReg(i)));
			}
		}
		if (appendBrace) {
			lw.append("}");
		}
	}

	private int getInsnColStart() {
		int start = 0;
		if (printFileOffset) {
			start += 8 + 1 + 1; // plus 1s for space and the ':'
		}
		if (printBytecode) {
			start += BYTECODE_COLUMN_WIDTH + 1; // plus 1 for space
		}
		return start;
	}

	private void fmtCols(InsnData insn, LineInfo line) {
		if (printFileOffset) {
			line.getLineWriter().append(String.format(FMT_FILE_OFFSET + " ", insn.getFileOffset()));
		}
		if (printBytecode) {
			formatByteCode(line.getLineWriter(), insn.getByteCode());
			line.getLineWriter().append(" ");
			line.getLineWriter().append(String.format(FMT_CODE_OFFSET + " ", insn.getOffset()));
		}
	}

	private void formatByteCode(StringBuilder smali, byte[] bytes) {
		int maxLen = Math.min(bytes.length, 4 * 2); // limit to 4 units
		StringBuilder inHex = new StringBuilder();
		for (int i = 0; i < maxLen; i++) {
			inHex.append(String.format("%02x", bytes[i]));
			if (i % 2 == 1) {
				inHex.append(' ');
			}
		}
		smali.append(String.format(FMT_BYTECODE_COL, inHex));
		if (maxLen < bytes.length) {
			smali.append("...");
		} else {
			smali.append("   ");
		}
	}

	private boolean fmtPayloadInsn(InsnData insn, LineInfo line) {
		Opcode opcode = insn.getOpcode();
		if (opcode == PACKED_SWITCH_PAYLOAD) {
			line.getLineWriter().append("packed-switch-payload");
			line.addInsnLine(insn.getOffset(), line.getLineWriter().toString());

			ISwitchPayload payload = (ISwitchPayload) insn.getPayload();
			if (payload != null) {
				fmtSwitchPayload(insn, FMT_P_SWITCH_CASE, FMT_P_SWITCH_CASE_TAG, line, payload);
			}
			return true;
		}
		if (opcode == SPARSE_SWITCH_PAYLOAD) {
			line.getLineWriter().append("sparse-switch-payload");
			line.addInsnLine(insn.getOffset(), line.getLineWriter().toString());

			ISwitchPayload payload = (ISwitchPayload) insn.getPayload();
			if (payload != null) {
				fmtSwitchPayload(insn, FMT_S_SWITCH_CASE, FMT_S_SWITCH_CASE_TAG, line, payload);
			}
			return true;
		}
		if (opcode == FILL_ARRAY_DATA_PAYLOAD) {
			line.getLineWriter().append("fill-array-data-payload");
			line.addInsnLine(insn.getOffset(), line.getLineWriter().toString());
			return true;
		}
		return false;
	}

	private void fmtSwitchPayload(InsnData insn, String fmtTarget, String fmtTag, LineInfo line, ISwitchPayload payload) {
		int lineStart = getInsnColStart();
		lineStart += CODE_OFFSET_COLUMN_WIDTH + 1 + 1; // plus 1s for space and the ':'
		String basicIndent = new String(new byte[lineStart]).replace("\0", " ");
		String indent = SmaliWriter.INDENT_STR + basicIndent;
		int[] keys = payload.getKeys();
		int[] targets = payload.getTargets();
		Integer switchOffset = line.payloadOffsetMap.get(insn.getOffset());
		if (switchOffset == null) {
			throw new JadxRuntimeException("Unknown switch insn for payload at " + insn.getOffset());
		}
		for (int i = 0; i < keys.length; i++) {
			int target = switchOffset + targets[i];
			line.addInsnLine(insn.getOffset(),
					String.format("%scase %d: -> " + fmtTarget, indent, keys[i], target));
			line.addTip(target,
					String.format(fmtTag, target), String.format(" # case %d", keys[i]));
		}
		line.addInsnLine(insn.getOffset(), basicIndent + ".end payload");
	}

	private static String literal(InsnData insn) {
		long it = insn.getLiteral();
		String tip = "";
		if (it > Integer.MAX_VALUE) {
			if (isWideIns(insn)) {
				tip = " # double: " + Double.longBitsToDouble(it);
			} else if (getOpenCodeByte(insn) == 0x15) { // CONST_HIGH16 = 0x15;
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
		Opcode op = insn.getOpcode();
		if (op == INVOKE_CUSTOM || op == INVOKE_CUSTOM_RANGE) {
			insn.getIndexAsCallSite().load();
			return String.format("%s # call_site@%04x", insn.getIndexAsCallSite().toString(), insn.getIndex());
		}
		IMethodRef mthRef = insn.getIndexAsMethod();
		mthRef.load();
		if (op == INVOKE_POLYMORPHIC || op == INVOKE_POLYMORPHIC_RANGE) {
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

	protected static boolean isRangeRegIns(InsnData insn) {
		switch (insn.getOpcode()) {
			case INVOKE_VIRTUAL_RANGE:
			case INVOKE_SUPER_RANGE:
			case INVOKE_DIRECT_RANGE:
			case INVOKE_STATIC_RANGE:
			case INVOKE_INTERFACE_RANGE:
			case FILLED_NEW_ARRAY_RANGE:
			case INVOKE_CUSTOM_RANGE:
			case INVOKE_POLYMORPHIC_RANGE:
				return true;
		}
		return false;
	}

	private static int getOpenCodeByte(InsnData insn) {
		return insn.getRawOpcodeUnit() & 0xff;
	}

	private static boolean isWideIns(InsnData insn) {
		return insn.getOpcode() == CONST_WIDE;
	}

	private static boolean hasLiteral(InsnData insn) {
		int opcode = getOpenCodeByte(insn);
		return insn.getOpcode() == CONST
				|| insn.getOpcode() == CONST_WIDE
				|| (opcode >= 0xd0 && opcode <= 0xe2); // add-int/lit16 to ushr-int/lit8
	}

	private static boolean isRegList(InsnData insn) {
		return insn.getOpcode() == FILLED_NEW_ARRAY || insn.getOpcode() == FILLED_NEW_ARRAY_RANGE;
	}

	private class LineInfo {
		private SmaliMethodNode smaliMthNode = new SmaliMethodNode();
		private final StringBuilder lineWriter = new StringBuilder(50);

		private String lastDebugTip = "";
		private final Map<Integer, List<String>> insnOffsetMap = new LinkedHashMap<>();
		private final Map<Integer, String> regNameMap = new HashMap<>();
		private Map<Integer, Map<String, Object>> tipMap = Collections.emptyMap();
		private Map<Integer, Integer> payloadOffsetMap = Collections.emptyMap();

		public LineInfo() {
		}

		public StringBuilder getLineWriter() {
			return lineWriter;
		}

		public void reset() {
			lastDebugTip = "";
			payloadOffsetMap = Collections.emptyMap();
			tipMap = Collections.emptyMap();
			insnOffsetMap.clear();
			regNameMap.clear();
			smaliMthNode = new SmaliMethodNode();
		}

		public void addRegName(int regNum, String name) {
			regNameMap.put(regNum, name);
		}

		public String getRegName(int regNum) {
			String name = regNameMap.get(regNum);
			if (name == null) {
				name = "v" + regNum;
			}
			return name;
		}

		public void addInsnLine(int codeOffset, String insnLine) {
			List<String> insnList = insnOffsetMap.computeIfAbsent(codeOffset, k -> new ArrayList<>(1));
			insnList.add(insnLine);
		}

		public void addTip(int offset, String tip, String extra) {
			if (tipMap.isEmpty()) {
				tipMap = new LinkedHashMap<>();
			}
			Map<String, Object> innerMap = tipMap.computeIfAbsent(offset, k -> new LinkedHashMap<>());
			Object obj = innerMap.get(tip);
			if (obj != null) {
				if (obj instanceof String) {
					if (obj.equals("")) {
						innerMap.put(tip, 2);
					} else {
						List<String> extras = new ArrayList<>(2);
						extras.add((String) obj);
						extras.add(extra);
						innerMap.put(tip, extras);
					}
				} else if (obj instanceof Integer) {
					innerMap.put(tip, ((int) obj) + 1);
				} else if (obj instanceof List) {
					if (!extra.equals("")) {
						List<String> extras = (List<String>) obj;
						extras.add(extra);
					}
				}
			} else {
				innerMap.put(tip, extra);
			}
		}

		public void addDebugLineTip(int offset, String tip, String extra) {
			if (tip.equals(lastDebugTip)) {
				return;
			}
			lastDebugTip = tip;
			if (tipMap.isEmpty()) {
				tipMap = new LinkedHashMap<>();
			}
			Map<String, Object> innerMap = tipMap.computeIfAbsent(offset, k -> new LinkedHashMap<>());
			innerMap.put(tip, extra);
		}

		public void addPayloadOffset(int curOffset, int payloadOffset) {
			if (payloadOffsetMap.isEmpty()) {
				payloadOffsetMap = new HashMap<>();
			}
			payloadOffsetMap.put(payloadOffset, curOffset);
		}

		public void write(SmaliWriter smali) {
			int lineOffset = getInsnColStart();
			for (Entry<Integer, List<String>> entry : insnOffsetMap.entrySet()) {
				writeTip(smali, entry.getKey(), lineOffset);
				smaliMthNode.setInsnInfo(entry.getKey(), lineOffset + smali.getLength());
				smaliMthNode.attachLine(smali.getLine(), entry.getKey());
				smali.attachSourceLine(entry.getKey());
				for (String s : entry.getValue()) {
					smali.add(s).startLine();
				}
			}
		}

		private void writeTip(SmaliWriter smali, int codeOffset, int lineOffset) {
			Map<String, Object> tip = tipMap.get(codeOffset);
			if (tip != null) {
				for (Entry<String, Object> entry : tip.entrySet()) {
					int start = Math.max(0, lineOffset - entry.getKey().length());
					if (start > 0) {
						smali.add(new String(new byte[start]).replace("\0", " "));
					}
					if (entry.getValue() instanceof Integer) {
						smali.add(String.format("%s # %d refs", entry.getKey(), entry.getValue()))
								.startLine();
					} else if (entry.getValue() instanceof String) {
						smali.add(String.format("%s%s", entry.getKey(), entry.getValue()))
								.startLine();
					} else if (entry.getValue() instanceof List) {
						List<String> extras = (List<String>) entry.getValue();
						smali.add(String.format("%s%s", entry.getKey(), extras.get(0)))
								.startLine();
						String pad = new String(new byte[lineOffset]).replace("\0", " ");
						for (int i = 1; i < extras.size(); i++) {
							smali.add(String.format("%s%s", pad, extras.get(i)))
									.startLine();
						}
					} else {
						smali.add(String.format("%s%s", entry.getKey(), entry.getValue()))
								.startLine();
					}
				}
			}
		}
	}

	private static class SmaliInsnDecoder extends InsnDecoder {
		@Override
		protected @NotNull InsnNode decode(InsnData insn) {
			try {
				return super.decode(insn);
			} catch (Exception e) {
				switch (insn.getOpcode()) {
					case INVOKE_CUSTOM:
					case INVOKE_CUSTOM_RANGE:
					case INVOKE_POLYMORPHIC:
					case INVOKE_POLYMORPHIC_RANGE:
					case CONST_METHOD_HANDLE:
					case CONST_METHOD_TYPE:
						return new InsnNode(InsnType.INVOKE, insn.getRegsCount());
					default:
						throw new RuntimeException(e);
				}
			}
		}

		public SmaliInsnDecoder(MethodNode mthNode) {
			super(mthNode);
		}

		@Override
		public InsnNode[] process(ICodeReader codeReader) {
			return null;
		}
	}

	private static class RawField {
		boolean isStatic;
		String accessFlag;
		String name;
		String type;
		AttributeStorage attributes;

		private static RawField make(IFieldData f) {
			RawField field = new RawField();
			field.isStatic = hasStaticFlag(f.getAccessFlags());
			field.accessFlag = AccessFlags.format(f.getAccessFlags(), FIELD);
			field.name = f.getName();
			field.type = f.getType();
			field.attributes = new AttributeStorage(f.getAttributes());
			return field;
		}
	}
}
