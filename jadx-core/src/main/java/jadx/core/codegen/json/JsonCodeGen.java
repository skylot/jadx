package jadx.core.codegen.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.impl.AnnotatedCodeWriter;
import jadx.api.impl.SimpleCodeWriter;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.core.codegen.ClassGen;
import jadx.core.codegen.MethodGen;
import jadx.core.codegen.json.cls.JsonClass;
import jadx.core.codegen.json.cls.JsonCodeLine;
import jadx.core.codegen.json.cls.JsonField;
import jadx.core.codegen.json.cls.JsonMethod;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.CodeGenUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class JsonCodeGen {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
			.disableHtmlEscaping()
			.create();

	private final ClassNode cls;
	private final JadxArgs args;
	private final RootNode root;

	public JsonCodeGen(ClassNode cls) {
		this.cls = cls;
		this.root = cls.root();
		this.args = root.getArgs();
	}

	public String process() {
		JsonClass jsonCls = processCls(cls, null);
		return GSON.toJson(jsonCls);
	}

	private JsonClass processCls(ClassNode cls, @Nullable ClassGen parentCodeGen) {
		ClassGen classGen;
		if (parentCodeGen == null) {
			classGen = new ClassGen(cls, args);
		} else {
			classGen = new ClassGen(cls, parentCodeGen);
		}
		ClassInfo classInfo = cls.getClassInfo();

		JsonClass jsonCls = new JsonClass();
		jsonCls.setPkg(classInfo.getAliasPkg());
		jsonCls.setDex(cls.getInputFileName());
		jsonCls.setName(classInfo.getFullName());
		if (classInfo.hasAlias()) {
			jsonCls.setAlias(classInfo.getAliasFullName());
		}
		jsonCls.setType(getClassTypeStr(cls));
		jsonCls.setAccessFlags(cls.getAccessFlags().rawValue());
		if (!Objects.equals(cls.getSuperClass(), ArgType.OBJECT)) {
			jsonCls.setSuperClass(getTypeAlias(cls.getSuperClass()));
		}
		if (!cls.getInterfaces().isEmpty()) {
			jsonCls.setInterfaces(Utils.collectionMap(cls.getInterfaces(), this::getTypeAlias));
		}

		ICodeWriter cw = new SimpleCodeWriter();
		CodeGenUtils.addErrorsAndComments(cw, cls);
		classGen.addClassDeclaration(cw);
		jsonCls.setDeclaration(cw.getCodeStr());

		addFields(cls, jsonCls, classGen);
		addMethods(cls, jsonCls, classGen);
		addInnerClasses(cls, jsonCls, classGen);

		if (!cls.getClassInfo().isInner()) {
			List<String> imports = Utils.collectionMap(classGen.getImports(), ClassInfo::getAliasFullName);
			Collections.sort(imports);
			jsonCls.setImports(imports);
		}
		return jsonCls;
	}

	private void addInnerClasses(ClassNode cls, JsonClass jsonCls, ClassGen classGen) {
		List<ClassNode> innerClasses = cls.getInnerClasses();
		if (innerClasses.isEmpty()) {
			return;
		}
		jsonCls.setInnerClasses(new ArrayList<>(innerClasses.size()));
		for (ClassNode innerCls : innerClasses) {
			if (innerCls.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			JsonClass innerJsonCls = processCls(innerCls, classGen);
			jsonCls.getInnerClasses().add(innerJsonCls);
		}
	}

	private void addFields(ClassNode cls, JsonClass jsonCls, ClassGen classGen) {
		jsonCls.setFields(new ArrayList<>());
		for (FieldNode field : cls.getFields()) {
			if (field.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			JsonField jsonField = new JsonField();
			jsonField.setName(field.getName());
			if (field.getFieldInfo().hasAlias()) {
				jsonField.setAlias(field.getAlias());
			}

			ICodeWriter cw = new SimpleCodeWriter();
			classGen.addField(cw, field);
			jsonField.setDeclaration(cw.getCodeStr());
			jsonField.setAccessFlags(field.getAccessFlags().rawValue());
			jsonCls.getFields().add(jsonField);
		}
	}

	private void addMethods(ClassNode cls, JsonClass jsonCls, ClassGen classGen) {
		jsonCls.setMethods(new ArrayList<>());
		for (MethodNode mth : cls.getMethods()) {
			if (mth.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			JsonMethod jsonMth = new JsonMethod();
			jsonMth.setName(mth.getName());
			if (mth.getMethodInfo().hasAlias()) {
				jsonMth.setAlias(mth.getAlias());
			}
			jsonMth.setSignature(mth.getMethodInfo().getShortId());
			jsonMth.setReturnType(getTypeAlias(mth.getReturnType()));
			jsonMth.setArguments(Utils.collectionMap(mth.getMethodInfo().getArgumentsTypes(), this::getTypeAlias));

			MethodGen mthGen = new MethodGen(classGen, mth);
			ICodeWriter cw = new AnnotatedCodeWriter();
			mthGen.addDefinition(cw);
			jsonMth.setDeclaration(cw.getCodeStr());
			jsonMth.setAccessFlags(mth.getAccessFlags().rawValue());
			jsonMth.setLines(fillMthCode(mth, mthGen));
			jsonMth.setOffset("0x" + Long.toHexString(mth.getMethodCodeOffset()));
			jsonCls.getMethods().add(jsonMth);
		}
	}

	private List<JsonCodeLine> fillMthCode(MethodNode mth, MethodGen mthGen) {
		if (mth.isNoCode()) {
			return Collections.emptyList();
		}

		ICodeWriter cw = mth.root().makeCodeWriter();
		try {
			mthGen.addInstructions(cw);
		} catch (Exception e) {
			throw new JadxRuntimeException("Method generation error", e);
		}
		ICodeInfo code = cw.finish();
		String codeStr = code.getCodeStr();
		if (codeStr.isEmpty()) {
			return Collections.emptyList();
		}

		String[] lines = codeStr.split(ICodeWriter.NL);
		Map<Integer, Integer> lineMapping = code.getCodeMetadata().getLineMapping();
		ICodeMetadata metadata = code.getCodeMetadata();
		long mthCodeOffset = mth.getMethodCodeOffset() + 16;

		int linesCount = lines.length;
		List<JsonCodeLine> codeLines = new ArrayList<>(linesCount);
		int lineStartPos = 0;
		int newLineLen = ICodeWriter.NL.length();
		for (int i = 0; i < linesCount; i++) {
			String codeLine = lines[i];
			int line = i + 2;
			JsonCodeLine jsonCodeLine = new JsonCodeLine();
			jsonCodeLine.setCode(codeLine);
			jsonCodeLine.setSourceLine(lineMapping.get(line));
			Object obj = metadata.getAt(lineStartPos);
			if (obj instanceof InsnCodeOffset) {
				long offset = ((InsnCodeOffset) obj).getOffset();
				jsonCodeLine.setOffset("0x" + Long.toHexString(mthCodeOffset + offset * 2));
			}
			codeLines.add(jsonCodeLine);
			lineStartPos += codeLine.length() + newLineLen;
		}
		return codeLines;
	}

	private String getTypeAlias(ArgType clsType) {
		if (Objects.equals(clsType, ArgType.OBJECT)) {
			return ArgType.OBJECT.getObject();
		}
		if (clsType.isObject()) {
			ClassInfo classInfo = ClassInfo.fromType(root, clsType);
			return classInfo.getAliasFullName();
		}
		return clsType.toString();
	}

	private String getClassTypeStr(ClassNode cls) {
		if (cls.isEnum()) {
			return "enum";
		}
		if (cls.getAccessFlags().isInterface()) {
			return "interface";
		}
		return "class";
	}
}
