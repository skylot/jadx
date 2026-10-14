package jadx.mcp.jadx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaPackage;
import jadx.api.JavaVariable;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolArgs;
import jadx.mcp.tools.ToolRegistry;

import static jadx.mcp.jadx.ToolSupport.DEFAULT_LIMIT;
import static jadx.mcp.jadx.ToolSupport.MAX_LIMIT;
import static jadx.mcp.jadx.ToolSupport.classNameParam;
import static jadx.mcp.jadx.ToolSupport.limitParam;
import static jadx.mcp.jadx.ToolSupport.offsetParam;
import static jadx.mcp.jadx.ToolSupport.optClassNameParam;
import static jadx.mcp.jadx.ToolSupport.page;
import static jadx.mcp.jadx.ToolSupport.pageList;
import static jadx.mcp.jadx.ToolSupport.textPageParams;
import static jadx.mcp.jadx.ToolSupport.withProject;

/**
 * Classes, members and code access tools
 */
final class ClassTools {

	private ClassTools() {
	}

	static void register(ToolRegistry registry, McpBackend backend) {
		registry.register(McpTool.builder("get_all_classes")
				.title("List classes")
				.description("List classes (paged, sorted by id). Filter by package and name substring. "
						+ "Returns stable class ids to use in other tools.")
				.input(Schema.object()
						.optional("package", Schema.string("Package name (current or original); includes subpackages"))
						.optional("name_filter", Schema.string("Case-insensitive substring of class full name"))
						.optional("include_inner", Schema.bool("Include inner classes", false))
						.optional("offset", offsetParam())
						.optional("limit", limitParam(DEFAULT_LIMIT, MAX_LIMIT))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("classes", Schema.array(NodeJson.classSummarySchema(), "Classes"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					String pkg = args.getOptString("package");
					String filter = args.getOptString("name_filter");
					String filterLower = filter == null ? null : filter.toLowerCase(Locale.ROOT);
					boolean includeInner = args.getBool("include_inner", false);
					List<ClassNode> list = new ArrayList<>();
					for (ClassNode cls : lease.getDecompiler().getRoot().getClasses()) {
						if (cls.contains(AFlag.DONT_GENERATE) || (!includeInner && cls.isInner())) {
							continue;
						}
						ClassInfo info = cls.getClassInfo();
						if (pkg != null && !inPackage(info.getAliasPkg(), pkg) && !inPackage(info.getPackage(), pkg)) {
							continue;
						}
						if (filterLower != null
								&& !info.getAliasFullName().toLowerCase(Locale.ROOT).contains(filterLower)
								&& !info.getFullName().toLowerCase(Locale.ROOT).contains(filterLower)) {
							continue;
						}
						list.add(cls);
					}
					lease.check();
					list.sort(Comparator.comparing(c -> c.getClassInfo().getRawName()));
					JsonObject result = new JsonObject();
					result.add("classes", pageList(result, list, args, DEFAULT_LIMIT,
							c -> NodeJson.classSummary(resolver.toJava(c))));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_packages")
				.title("List packages")
				.description("List packages with class counts (paged, sorted by current name)")
				.input(Schema.object()
						.optional("name_filter", Schema.string("Case-insensitive substring of package name"))
						.optional("offset", offsetParam())
						.optional("limit", limitParam(200, MAX_LIMIT))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("packages", Schema.array(NodeJson.packageSummarySchema(), "Packages"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					String filter = args.getOptString("name_filter");
					String filterLower = filter == null ? null : filter.toLowerCase(Locale.ROOT);
					List<JavaPackage> list = new ArrayList<>();
					for (JavaPackage pkg : lease.getDecompiler().getPackages()) {
						if (filterLower == null
								|| pkg.getFullName().toLowerCase(Locale.ROOT).contains(filterLower)
								|| pkg.getRawFullName().toLowerCase(Locale.ROOT).contains(filterLower)) {
							list.add(pkg);
						}
					}
					list.sort(Comparator.comparing(JavaPackage::getFullName));
					JsonObject result = new JsonObject();
					result.add("packages", pageList(result, list, args, 200, NodeJson::packageSummary));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_class_info")
				.title("Class info")
				.description("Get class metadata: names, access, super class, interfaces, outer/inner classes, "
						+ "members count, source file. Does not return code.")
				.input(Schema.object().required("class_name", classNameParam()).build())
				.output(Schema.object().open()
						.required("class", NodeJson.classSummarySchema())
						.required("super_class", Schema.nullableString("Super class type"))
						.required("interfaces", Schema.stringArray("Implemented interfaces"))
						.required("outer_class_id", Schema.nullableString("Outer class id for inner classes"))
						.required("top_class_id", Schema.string("Top-level class id (code container)"))
						.required("inner_classes", Schema.stringArray("Inner class ids"))
						.required("methods_count", Schema.integer("Methods count"))
						.required("fields_count", Schema.integer("Fields count"))
						.required("source_file", Schema.nullableString("Source file name from debug info"))
						.required("input_file", Schema.nullableString("Input file containing the class"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaClass cls = resolver.resolveClass(args.getRequiredString("class_name"));
					ClassNode clsNode = cls.getClassNode();
					JsonObject result = new JsonObject();
					result.add("class", NodeJson.classSummary(cls));
					ArgType superClass = clsNode.getSuperClass();
					result.addProperty("super_class", superClass == null ? null : superClass.toString());
					JsonArray interfaces = new JsonArray();
					clsNode.getInterfaces().forEach(i -> interfaces.add(i.toString()));
					result.add("interfaces", interfaces);
					ClassNode parent = clsNode.isInner() ? clsNode.getParentClass() : null;
					result.addProperty("outer_class_id", parent == null ? null : NodeIds.classId(parent));
					result.addProperty("top_class_id", NodeIds.classId(clsNode.getTopParentClass()));
					JsonArray inner = new JsonArray();
					clsNode.getInnerClasses().forEach(c -> inner.add(NodeIds.classId(c)));
					result.add("inner_classes", inner);
					result.addProperty("methods_count", clsNode.getMethods().size());
					result.addProperty("fields_count", clsNode.getFields().size());
					SourceFileAttr sourceFile = clsNode.get(JadxAttrType.SOURCE_FILE);
					result.addProperty("source_file", sourceFile == null ? null : sourceFile.getFileName());
					result.addProperty("input_file", clsNode.getInputFileName());
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_class_source")
				.title("Class source")
				.description("Get decompiled Java source of a class (paged by lines). "
						+ "For inner classes returns only the inner class part of the top-level class code. "
						+ "Line numbers refer to the code of the top-level class (same as in GUI and xrefs).")
				.input(textPageParams(Schema.object().required("class_name", classNameParam())).build())
				.output(Schema.object().open()
						.required("class", NodeJson.classSummarySchema())
						.required("code_class_id", Schema.string("Top-level class whose code contains the source"))
						.required("range_start_line", Schema.integer("First line of class in code"))
						.required("range_end_line", Schema.integer("Last line of class in code"))
						.required("source", NodeJson.pageSchema("Source page"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaClass cls = resolver.resolveClass(args.getRequiredString("class_name"));
					return classSource(lease, cls, args);
				}))
				.build());

		registry.register(McpTool.builder("get_methods_of_class")
				.title("Class methods")
				.description("List methods of a class with ids, signatures and access (paged). Decompiles class if needed.")
				.input(Schema.object()
						.required("class_name", classNameParam())
						.optional("offset", offsetParam())
						.optional("limit", limitParam(200, MAX_LIMIT))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("class_id", Schema.string("Class id"))
						.required("methods", Schema.array(NodeJson.methodSummarySchema(), "Methods"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaClass cls = resolver.resolveClass(args.getRequiredString("class_name"));
					List<JavaMethod> methods = cls.getMethods();
					lease.check();
					JsonObject result = new JsonObject();
					result.addProperty("class_id", NodeIds.classId(cls));
					result.add("methods", pageList(result, methods, args, 200, NodeJson::methodSummary));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_fields_of_class")
				.title("Class fields")
				.description("List fields of a class with ids, types and access (paged)")
				.input(Schema.object()
						.required("class_name", classNameParam())
						.optional("offset", offsetParam())
						.optional("limit", limitParam(200, MAX_LIMIT))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("class_id", Schema.string("Class id"))
						.required("fields", Schema.array(NodeJson.fieldSummarySchema(), "Fields"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaClass cls = resolver.resolveClass(args.getRequiredString("class_name"));
					List<JavaField> fields = cls.getFields();
					lease.check();
					JsonObject result = new JsonObject();
					result.addProperty("class_id", NodeIds.classId(cls));
					result.add("fields", pageList(result, fields, args, 200, NodeJson::fieldSummary));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_method_by_name")
				.title("Method source")
				.description("Get decompiled code of a method. Reference: full method id 'class->name(desc)ret', "
						+ "or 'class_name' + method name / descriptor 'name(I)V' / Java args 'name(int, String)'. "
						+ "If name matches several overloads returns AMBIGUOUS error with candidate ids "
						+ "unless 'all_overloads' is true. Code is paged inside method range: "
						+ "if 'source.truncated', call again with 'start_line' = 'source.next_start_line'.")
				.input(textPageParams(Schema.object()
						.required("method_name", Schema.nonEmptyString("Method id, name or descriptor"))
						.optional("class_name", optClassNameParam())
						.optional("all_overloads", Schema.bool("Return all overloads matching the name", false)))
								.build())
				.output(Schema.object().open()
						.required("methods", Schema.array(methodCodeSchema(), "Matched methods with code"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					String className = args.getOptString("class_name");
					String methodName = args.getRequiredString("method_name");
					List<JavaMethod> methods;
					if (args.getBool("all_overloads", false)) {
						methods = resolver.resolveMethods(className, methodName);
					} else {
						methods = List.of(resolver.resolveMethod(className, methodName));
					}
					JsonArray arr = new JsonArray();
					for (JavaMethod mth : methods) {
						arr.add(methodCode(lease, mth, args));
					}
					JsonObject result = new JsonObject();
					result.add("methods", arr);
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_smali_of_class")
				.title("Class smali")
				.description("Get smali (Dalvik bytecode disassembly) of a class (paged by lines)")
				.input(textPageParams(Schema.object().required("class_name", classNameParam())).build())
				.output(Schema.object().open()
						.required("class", NodeJson.classSummarySchema())
						.required("source", NodeJson.pageSchema("Smali page"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaClass cls = resolver.resolveClass(args.getRequiredString("class_name"));
					CodeText smali = new CodeText(cls.getSmali());
					lease.check();
					JsonObject result = new JsonObject();
					result.add("class", NodeJson.classSummary(cls));
					result.add("source", page(smali, args));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_method_variables")
				.title("Method variables")
				.description("List parameters and local variables of a method from decompiled code "
						+ "(ids for 'rename_variable')")
				.input(Schema.object()
						.required("method_name", Schema.nonEmptyString("Method id, name or descriptor"))
						.optional("class_name", optClassNameParam())
						.build())
				.output(Schema.object().open()
						.required("method", NodeJson.methodSummarySchema())
						.required("variables", Schema.array(NodeJson.variableSummarySchema(), "Variables"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaMethod mth = resolver.resolveMethod(args.getOptString("class_name"), args.getRequiredString("method_name"));
					List<JavaVariable> vars = resolver.collectVariables(mth);
					JsonObject result = new JsonObject();
					result.add("method", NodeJson.methodSummary(mth));
					result.add("variables", ToolSupport.toArray(vars, NodeJson::variableSummary));
					return result;
				}))
				.build());
	}

	static boolean inPackage(String clsPkg, String pkg) {
		return clsPkg.equals(pkg) || clsPkg.startsWith(pkg + '.');
	}

	static JsonObject classSource(ProjectLease lease, JavaClass cls, ToolArgs args) {
		JavaClass topCls = cls.getTopParentClass();
		CodeText code = new CodeText(topCls.getCode());
		lease.check();
		int rangeStart = 1;
		int rangeEnd = code.getLinesCount();
		if (cls != topCls) {
			int[] lines = CodeRanges.nodeLines(code, cls.getDefPos(), true);
			if (lines != null) {
				rangeStart = lines[0];
				rangeEnd = lines[1];
			}
		}
		JsonObject result = new JsonObject();
		result.add("class", NodeJson.classSummary(cls));
		result.addProperty("code_class_id", NodeIds.classId(topCls));
		result.addProperty("range_start_line", rangeStart);
		result.addProperty("range_end_line", rangeEnd);
		result.add("source", page(code, args, rangeStart, rangeEnd));
		return result;
	}

	/**
	 * Method code page. Same layout as class source: full method range and page inside it.
	 */
	static JsonObject methodCode(ProjectLease lease, JavaMethod mth, ToolArgs args) {
		JavaClass topCls = mth.getTopParentClass();
		CodeText code = new CodeText(topCls.getCode());
		lease.check();
		JsonObject obj = new JsonObject();
		obj.add("method", NodeJson.methodSummary(mth));
		obj.addProperty("code_class_id", NodeIds.classId(topCls));
		int defPos = mth.getDefPos();
		boolean hasBody = !mth.getMethodNode().isNoCode();
		int[] lines = defPos <= 0 ? null : CodeRanges.nodeLines(code, defPos, hasBody);
		if (lines == null) {
			obj.add("range_start_line", null);
			obj.add("range_end_line", null);
			obj.add("source", null);
			obj.addProperty("message", "Method is not present in decompiled code (synthetic, inlined or not generated)");
			return obj;
		}
		obj.addProperty("range_start_line", lines[0]);
		obj.addProperty("range_end_line", lines[1]);
		obj.add("source", page(code, args, lines[0], lines[1]));
		return obj;
	}

	static JsonObject methodCodeSchema() {
		return Schema.object().open()
				.required("method", NodeJson.methodSummarySchema())
				.required("code_class_id", Schema.string("Top-level class containing the code"))
				.required("range_start_line", Schema.nullableInteger("First line of method (with comments and annotations) "
						+ "in top-level class code"))
				.required("range_end_line", Schema.nullableInteger("Last line of method in top-level class code"))
				.required("source", ProjectTools.nullable(NodeJson.pageSchema("Method code page"),
						"Method code page, null if method is not in decompiled code"))
				.optional("message", Schema.string("Reason if code not available"))
				.build();
	}
}
