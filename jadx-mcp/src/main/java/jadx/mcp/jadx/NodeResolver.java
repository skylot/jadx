package jadx.mcp.jadx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;

import jadx.api.ICodeInfo;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.JavaVariable;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;

/**
 * Resolve nodes by stable ids, original names or current (alias) names.
 * Report ambiguity with candidate ids instead of guessing.
 */
public final class NodeResolver {
	private static final int MAX_CANDIDATES = 20;

	/**
	 * Raw class name index, raw names don't change on renames, so index valid for whole project
	 * generation
	 */
	private static final Map<RootNode, Map<String, ClassNode>> RAW_INDEX_CACHE = new WeakHashMap<>();

	private final ProjectLease lease;
	private final JadxDecompiler decompiler;

	public NodeResolver(ProjectLease lease) {
		this.lease = lease;
		this.decompiler = lease.getDecompiler();
	}

	public JadxDecompiler getDecompiler() {
		return decompiler;
	}

	public JavaClass toJava(ClassNode cls) {
		return (JavaClass) decompiler.getJavaNodeByRef(cls);
	}

	public JavaMethod toJava(MethodNode mth) {
		return (JavaMethod) decompiler.getJavaNodeByRef(mth);
	}

	public JavaField toJava(FieldNode fld) {
		return (JavaField) decompiler.getJavaNodeByRef(fld);
	}

	private Map<String, ClassNode> getRawIndex() {
		RootNode root = decompiler.getRoot();
		synchronized (RAW_INDEX_CACHE) {
			Map<String, ClassNode> index = RAW_INDEX_CACHE.get(root);
			if (index == null) {
				List<ClassNode> classes = root.getClasses();
				index = new HashMap<>(classes.size() * 2);
				for (ClassNode cls : classes) {
					index.put(cls.getClassInfo().getRawName(), cls);
				}
				RAW_INDEX_CACHE.put(root, index);
			}
			return index;
		}
	}

	/**
	 * Normalize class reference: smali descriptor 'Lcom/a/B;' or path 'com/a/B' to 'com.a.B'
	 */
	static String normalizeClassRef(String ref) {
		String s = ref.trim();
		if (s.startsWith("L") && s.endsWith(";") && s.length() > 2) {
			s = s.substring(1, s.length() - 1);
		}
		if (s.endsWith(".java") || s.endsWith(".class")) {
			s = s.substring(0, s.lastIndexOf('.'));
		}
		return s.replace('/', '.');
	}

	/**
	 * Resolve class by: id (raw name with '$' for inner), original full name ('Outer.Inner'),
	 * current full name (with renames/deobfuscation), smali descriptor.
	 */
	public JavaClass resolveClass(String ref) throws ToolException {
		String name = normalizeClassRef(ref);
		if (name.isEmpty()) {
			throw ToolException.invalidArg("Class name is empty");
		}
		ClassNode byRaw = getRawIndex().get(name);
		if (byRaw != null) {
			return toJava(byRaw);
		}
		Set<ClassNode> matches = new LinkedHashSet<>();
		for (ClassNode cls : decompiler.getRoot().getClasses()) {
			if (cls.getClassInfo().getFullName().equals(name)
					|| cls.getClassInfo().getAliasFullName().equals(name)) {
				matches.add(cls);
			}
		}
		lease.check();
		if (matches.size() == 1) {
			return toJava(matches.iterator().next());
		}
		if (matches.size() > 1) {
			JsonArray candidates = new JsonArray();
			matches.stream().limit(MAX_CANDIDATES).forEach(c -> candidates.add(NodeIds.classId(c)));
			throw new ToolException(ToolErrorCode.AMBIGUOUS, "Several classes match '" + ref + "', use class id")
					.detail("candidates", candidates);
		}
		throw classNotFound(ref, name);
	}

	private ToolException classNotFound(String ref, String name) {
		String shortName = name.substring(name.lastIndexOf('.') + 1);
		String shortLower = shortName.toLowerCase(Locale.ROOT);
		JsonArray similar = new JsonArray();
		for (ClassNode cls : decompiler.getRoot().getClasses()) {
			if (cls.getClassInfo().getShortName().toLowerCase(Locale.ROOT).equals(shortLower)
					|| cls.getClassInfo().getAliasShortName().toLowerCase(Locale.ROOT).equals(shortLower)) {
				similar.add(NodeIds.classId(cls));
				if (similar.size() >= MAX_CANDIDATES) {
					break;
				}
			}
		}
		ToolException ex = ToolException.notFound("Class not found: " + ref
				+ ". Use 'search_classes_by_keyword' or 'get_all_classes' to find class ids.");
		if (!similar.isEmpty()) {
			ex.detail("similar", similar);
		}
		return ex;
	}

	/**
	 * Resolve method by full id ('cls->name(args)ret') or by member reference inside class:
	 * descriptor 'name(I)V', descriptor without return type 'name(I)', Java style 'name(int, String)'
	 * or name only.
	 */
	public JavaMethod resolveMethod(@Nullable String className, String methodRef) throws ToolException {
		String ref = methodRef.trim();
		String clsPart = className;
		String memberPart = ref;
		int sep = ref.indexOf(NodeIds.MEMBER_SEPARATOR);
		if (sep != -1) {
			clsPart = ref.substring(0, sep);
			memberPart = ref.substring(sep + NodeIds.MEMBER_SEPARATOR.length());
		}
		if (clsPart == null || clsPart.trim().isEmpty()) {
			throw ToolException.invalidArg("Class is required: pass 'class_name' or full method id 'class->name(args)ret'");
		}
		JavaClass cls = resolveClass(clsPart);
		List<MethodNode> matches = matchMethods(cls.getClassNode(), memberPart);
		if (matches.size() == 1) {
			return toJava(matches.get(0));
		}
		if (matches.isEmpty()) {
			JsonArray available = new JsonArray();
			for (MethodNode mth : cls.getClassNode().getMethods()) {
				if (available.size() < 50) {
					available.add(NodeIds.methodId(toJava(mth)));
				}
			}
			throw ToolException.notFound("Method '" + memberPart + "' not found in class " + NodeIds.classId(cls))
					.detail("available", available);
		}
		throw ambiguousMethods(memberPart, matches);
	}

	/**
	 * Return all methods matching reference (for overloads listing)
	 */
	public List<JavaMethod> resolveMethods(@Nullable String className, String methodRef) throws ToolException {
		String ref = methodRef.trim();
		String clsPart = className;
		String memberPart = ref;
		int sep = ref.indexOf(NodeIds.MEMBER_SEPARATOR);
		if (sep != -1) {
			clsPart = ref.substring(0, sep);
			memberPart = ref.substring(sep + NodeIds.MEMBER_SEPARATOR.length());
		}
		if (clsPart == null || clsPart.trim().isEmpty()) {
			throw ToolException.invalidArg("Class is required: pass 'class_name' or full method id 'class->name(args)ret'");
		}
		JavaClass cls = resolveClass(clsPart);
		List<MethodNode> matches = matchMethods(cls.getClassNode(), memberPart);
		if (matches.isEmpty()) {
			throw ToolException.notFound("Method '" + memberPart + "' not found in class " + NodeIds.classId(cls));
		}
		List<JavaMethod> list = new ArrayList<>(matches.size());
		for (MethodNode mth : matches) {
			list.add(toJava(mth));
		}
		return list;
	}

	private ToolException ambiguousMethods(String ref, List<MethodNode> matches) {
		JsonArray candidates = new JsonArray();
		for (MethodNode mth : matches) {
			if (candidates.size() < MAX_CANDIDATES) {
				candidates.add(NodeIds.methodId(toJava(mth)));
			}
		}
		return new ToolException(ToolErrorCode.AMBIGUOUS,
				"Method reference '" + ref + "' matches " + matches.size()
						+ " overloads, use method id or descriptor from candidates")
								.detail("candidates", candidates);
	}

	static List<MethodNode> matchMethods(ClassNode cls, String memberRef) {
		String ref = memberRef.trim();
		List<MethodNode> methods = cls.getMethods();
		// exact descriptor
		for (MethodNode mth : methods) {
			if (mth.getMethodInfo().getShortId().equals(ref)) {
				return Collections.singletonList(mth);
			}
		}
		int paren = ref.indexOf('(');
		List<MethodNode> result = new ArrayList<>();
		if (paren == -1) {
			for (MethodNode mth : methods) {
				if (mth.getMethodInfo().getName().equals(ref) || mth.getMethodInfo().getAlias().equals(ref)) {
					result.add(mth);
				}
			}
			return result;
		}
		String name = ref.substring(0, paren).trim();
		String argsPart = ref.substring(paren);
		for (MethodNode mth : methods) {
			if (!mth.getMethodInfo().getName().equals(name) && !mth.getMethodInfo().getAlias().equals(name)) {
				continue;
			}
			String shortId = mth.getMethodInfo().getShortId();
			String argsDesc = shortId.substring(shortId.indexOf('('), shortId.indexOf(')') + 1);
			if (argsDesc.equals(argsPart) || matchJavaArgs(mth.getMethodInfo().getArgumentsTypes(), argsPart)) {
				result.add(mth);
			}
		}
		return result;
	}

	/**
	 * Match Java style arguments list: '(int, String)' or '(int, java.lang.String[])'
	 */
	private static boolean matchJavaArgs(List<ArgType> argTypes, String argsPart) {
		String inner = argsPart.trim();
		if (!inner.startsWith("(") || !inner.endsWith(")")) {
			return false;
		}
		inner = inner.substring(1, inner.length() - 1).trim();
		List<String> names = new ArrayList<>();
		if (!inner.isEmpty()) {
			int depth = 0;
			int start = 0;
			for (int i = 0; i < inner.length(); i++) {
				char c = inner.charAt(i);
				if (c == '<') {
					depth++;
				} else if (c == '>') {
					depth--;
				} else if (c == ',' && depth == 0) {
					names.add(inner.substring(start, i).trim());
					start = i + 1;
				}
			}
			names.add(inner.substring(start).trim());
		}
		if (names.size() != argTypes.size()) {
			return false;
		}
		for (int i = 0; i < names.size(); i++) {
			if (!typeMatches(argTypes.get(i), names.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean typeMatches(ArgType type, String name) {
		String expected = stripGenerics(name.replace(" ", "")).replace('$', '.');
		String typeStr = stripGenerics(type.toString()).replace('$', '.');
		if (typeStr.equals(expected)) {
			return true;
		}
		// simple name match: 'String' for 'java.lang.String', 'Inner' for 'a.Outer.Inner'
		int dot = typeStr.lastIndexOf('.');
		return dot != -1 && typeStr.substring(dot + 1).equals(expected);
	}

	private static String stripGenerics(String str) {
		StringBuilder sb = new StringBuilder(str.length());
		int depth = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '<') {
				depth++;
			} else if (c == '>') {
				depth--;
			} else if (depth == 0) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Resolve field by full id 'cls->name:type' or by name/'name:type' inside class
	 */
	public JavaField resolveField(@Nullable String className, String fieldRef) throws ToolException {
		String ref = fieldRef.trim();
		String clsPart = className;
		String memberPart = ref;
		int sep = ref.indexOf(NodeIds.MEMBER_SEPARATOR);
		if (sep != -1) {
			clsPart = ref.substring(0, sep);
			memberPart = ref.substring(sep + NodeIds.MEMBER_SEPARATOR.length());
		}
		if (clsPart == null || clsPart.trim().isEmpty()) {
			throw ToolException.invalidArg("Class is required: pass 'class_name' or full field id 'class->name:type'");
		}
		JavaClass cls = resolveClass(clsPart);
		List<FieldNode> matches = new ArrayList<>();
		for (FieldNode fld : cls.getClassNode().getFields()) {
			if (fld.getFieldInfo().getShortId().equals(memberPart)) {
				return toJava(fld);
			}
			if (fld.getFieldInfo().getName().equals(memberPart) || fld.getFieldInfo().getAlias().equals(memberPart)) {
				matches.add(fld);
			}
		}
		if (matches.size() == 1) {
			return toJava(matches.get(0));
		}
		if (matches.isEmpty()) {
			JsonArray available = new JsonArray();
			for (FieldNode fld : cls.getClassNode().getFields()) {
				if (available.size() < 50) {
					available.add(NodeIds.fieldId(toJava(fld)));
				}
			}
			throw ToolException.notFound("Field '" + memberPart + "' not found in class " + NodeIds.classId(cls))
					.detail("available", available);
		}
		JsonArray candidates = new JsonArray();
		matches.forEach(f -> candidates.add(NodeIds.fieldId(toJava(f))));
		throw new ToolException(ToolErrorCode.AMBIGUOUS, "Several fields match '" + memberPart + "', use field id")
				.detail("candidates", candidates);
	}

	/**
	 * Resolve package by raw (original) or current full name
	 */
	public JavaPackage resolvePackage(String ref) throws ToolException {
		String name = ref.trim().replace('/', '.');
		JavaPackage aliasMatch = null;
		for (JavaPackage pkg : decompiler.getPackages()) {
			if (pkg.getRawFullName().equals(name)) {
				return pkg;
			}
			if (aliasMatch == null && pkg.getFullName().equals(name)) {
				aliasMatch = pkg;
			}
		}
		if (aliasMatch != null) {
			return aliasMatch;
		}
		throw ToolException.notFound("Package not found: " + ref + ". Use 'get_packages' to list packages.");
	}

	/**
	 * Resolve any node by stable id. Ids of members contain '->', variables contain '#'.
	 * Without member separator: class, then package.
	 */
	public JavaNode resolveById(String id) throws ToolException {
		String ref = id.trim();
		int varSep = ref.lastIndexOf(NodeIds.VAR_SEPARATOR);
		int memberSep = ref.indexOf(NodeIds.MEMBER_SEPARATOR);
		if (memberSep != -1 && varSep > memberSep) {
			JavaMethod mth = resolveMethod(null, ref.substring(0, varSep));
			return resolveVariable(mth, ref.substring(varSep + 1));
		}
		if (memberSep != -1) {
			String member = ref.substring(memberSep + NodeIds.MEMBER_SEPARATOR.length());
			if (member.contains("(")) {
				return resolveMethod(null, ref);
			}
			if (member.contains(":")) {
				return resolveField(null, ref);
			}
			// name only: try method, then field
			try {
				return resolveMethod(null, ref);
			} catch (ToolException e) {
				if (e.getCode() != ToolErrorCode.NOT_FOUND) {
					throw e;
				}
				return resolveField(null, ref);
			}
		}
		try {
			return resolveClass(ref);
		} catch (ToolException e) {
			if (e.getCode() != ToolErrorCode.NOT_FOUND) {
				throw e;
			}
			try {
				return resolvePackage(ref);
			} catch (ToolException pkgEx) {
				throw e;
			}
		}
	}

	/**
	 * Variables declared in method (parameters and locals) from decompiled code metadata
	 */
	public List<JavaVariable> collectVariables(JavaMethod mth) {
		JavaClass topCls = mth.getTopParentClass();
		ICodeInfo codeInfo = topCls.getCodeInfo();
		lease.check();
		MethodNode mthNode = mth.getMethodNode();
		Map<String, JavaVariable> vars = new LinkedHashMap<>();
		if (!codeInfo.hasMetadata()) {
			return Collections.emptyList();
		}
		codeInfo.getCodeMetadata().searchDown(0, (pos, ann) -> {
			if (ann.getAnnType() == ICodeAnnotation.AnnType.DECLARATION) {
				ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
				if (node instanceof VarNode) {
					VarNode varNode = (VarNode) node;
					if (varNode.getMth().equals(mthNode)) {
						String key = NodeIds.varShortId(varNode.getReg(), varNode.getSsa());
						vars.putIfAbsent(key, new JavaVariable(mth, varNode));
					}
				}
			}
			return null;
		});
		return new ArrayList<>(vars.values());
	}

	/**
	 * Resolve variable by short id 'r1v2' or by current name
	 */
	public JavaVariable resolveVariable(JavaMethod mth, String varRef) throws ToolException {
		String ref = varRef.trim();
		if (ref.startsWith(NodeIds.VAR_SEPARATOR)) {
			ref = ref.substring(1);
		}
		List<JavaVariable> vars = collectVariables(mth);
		List<JavaVariable> byName = new ArrayList<>();
		for (JavaVariable var : vars) {
			if (NodeIds.varShortId(var.getReg(), var.getSsa()).equals(ref)) {
				return var;
			}
			if (ref.equals(var.getName())) {
				byName.add(var);
			}
		}
		if (byName.size() == 1) {
			return byName.get(0);
		}
		if (byName.isEmpty()) {
			JsonArray available = new JsonArray();
			for (JavaVariable var : vars) {
				available.add(var.getName() + " (" + NodeIds.varShortId(var.getReg(), var.getSsa()) + ')');
			}
			throw ToolException.notFound("Variable '" + varRef + "' not found in method " + NodeIds.methodId(mth))
					.detail("available", available);
		}
		JsonArray candidates = new JsonArray();
		byName.forEach(v -> candidates.add(NodeIds.variableId(v)));
		throw new ToolException(ToolErrorCode.AMBIGUOUS,
				"Several variables named '" + varRef + "' in method, use variable id (rNvM)")
						.detail("candidates", candidates);
	}
}
