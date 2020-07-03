package jadx.plugins.input.dex.smali;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import jadx.api.plugins.input.insns.InsnData;
import jadx.plugins.input.dex.insns.DexOpcodes;

public class SmaliInsnFormat {

	private static SmaliInsnFormat instance;

	public static synchronized SmaliInsnFormat getInstance() {
		SmaliInsnFormat instance = SmaliInsnFormat.instance;
		if (instance == null) {
			instance = new SmaliInsnFormat();
			SmaliInsnFormat.instance = instance;
		}
		return instance;
	}

	private final Map<Integer, InsnFormatter> formatters;

	public SmaliInsnFormat() {
		formatters = registerFormatters();
	}

	private Map<Integer, InsnFormatter> registerFormatters() {
		Map<Integer, InsnFormatter> map = new HashMap<>();
		map.put(DexOpcodes.NOP, fi -> fi.getCodeWriter().add("nop"));
		map.put(DexOpcodes.SGET_OBJECT, staticFieldInsn("sget-object"));
		map.put(DexOpcodes.SPUT_BOOLEAN, staticFieldInsn("sput-boolean"));
		map.put(DexOpcodes.CONST, constInsn("const"));
		map.put(DexOpcodes.CONST_HIGH16, constInsn("const/high16"));
		map.put(DexOpcodes.CONST_STRING, stringInsn("const-string"));
		map.put(DexOpcodes.INVOKE_VIRTUAL, invokeInsn("invoke-virtual"));
		map.put(DexOpcodes.INVOKE_DIRECT, invokeInsn("invoke-direct"));
		map.put(DexOpcodes.INVOKE_SUPER, invokeInsn("invoke-super"));
		map.put(DexOpcodes.INVOKE_STATIC, invokeInsn("invoke-static"));
		map.put(DexOpcodes.MOVE_RESULT, oneArgsInsn("move-result"));
		map.put(DexOpcodes.RETURN_VOID, noArgsInsn("return-void"));
		map.put(DexOpcodes.GOTO, gotoInsn("goto"));
		map.put(DexOpcodes.GOTO_16, gotoInsn("goto-16"));
		map.put(DexOpcodes.MOVE, simpleInsn("move"));
		// TODO: complete list
		return map;
	}

	private InsnFormatter simpleInsn(String name) {
		return fi -> {
			SmaliCodeWriter code = fi.getCodeWriter();
			code.add(name);
			InsnData insn = fi.getInsn();
			int regsCount = insn.getRegsCount();
			for (int i = 0; i < regsCount; i++) {
				if (i == 0) {
					code.add(' ');
				} else {
					code.add(", ");
				}
				code.add(regAt(fi, i));
			}
		};
	}

	private InsnFormatter gotoInsn(String name) {
		return fi -> fi.getCodeWriter().add(name).add(" :goto").add(Integer.toHexString(fi.getInsn().getTarget()));
	}

	@NotNull
	private InsnFormatter staticFieldInsn(String name) {
		return fi -> fi.getCodeWriter().add(name).add(' ').add(regAt(fi, 0)).add(", ").add(field(fi));
	}

	@NotNull
	private InsnFormatter constInsn(String name) {
		return fi -> fi.getCodeWriter().add(name).add(' ').add(regAt(fi, 0)).add(", ").add(literal(fi));
	}

	@NotNull
	private InsnFormatter stringInsn(String name) {
		return fi -> fi.getCodeWriter().add(name).add(' ').add(regAt(fi, 0)).add(", ").add(str(fi));
	}

	@NotNull
	private InsnFormatter invokeInsn(String name) {
		return fi -> {
			SmaliCodeWriter code = fi.getCodeWriter();
			code.add(name).add(' ');
			regsList(code, fi.getInsn());
			code.add(", ").add(method(fi));
		};
	}

	private InsnFormatter oneArgsInsn(String name) {
		return fi -> fi.getCodeWriter().add(name).add(' ').add(regAt(fi, 0));
	}

	private InsnFormatter noArgsInsn(String name) {
		return (fi) -> fi.getCodeWriter().add(name);
	}

	private String literal(InsnFormatterInfo fi) {
		return "0x" + Long.toHexString(fi.getInsn().getLiteral());
	}

	private String str(InsnFormatterInfo fi) {
		return "\"" + fi.getInsn().getIndexAsString() + "\"";
	}

	private String field(InsnFormatterInfo fi) {
		return fi.getInsn().getIndexAsField().toString();
	}

	private String method(InsnFormatterInfo fi) {
		return fi.getInsn().getIndexAsMethod().toString();
	}

	private void regsList(SmaliCodeWriter code, InsnData insn) {
		int argsCount = insn.getRegsCount();
		code.add('{');
		for (int i = 0; i < argsCount; i++) {
			if (i != 0) {
				code.add(", ");
			}
			code.add("v").add(insn.getReg(i));
		}
		code.add('}');
	}

	private String regAt(InsnFormatterInfo fi, int argNum) {
		return "v" + fi.getInsn().getReg(argNum);
	}

	public void format(InsnFormatterInfo formatInfo) {
		InsnData insn = formatInfo.getInsn();
		insn.decode();
		int rawOpcodeUnit = insn.getRawOpcodeUnit();
		int opcode = rawOpcodeUnit & 0xFF;
		InsnFormatter insnFormatter = formatters.get(opcode);
		if (insnFormatter != null) {
			insnFormatter.format(formatInfo);
		} else {
			formatInfo.getCodeWriter().add("# ").add(insn.getOpcode()).add(" (?0x").add(Integer.toHexString(rawOpcodeUnit)).add(')');
		}
	}

	public String format(InsnData insn) {
		InsnFormatterInfo formatInfo = new InsnFormatterInfo(new SmaliCodeWriter(), insn);
		format(formatInfo);
		return formatInfo.getCodeWriter().getCode();
	}
}
