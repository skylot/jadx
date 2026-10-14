package jadx.mcp.jadx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.api.ICodeInfo;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolArgs;
import jadx.mcp.tools.ToolRegistry;

import static jadx.mcp.jadx.ToolSupport.classNameParam;
import static jadx.mcp.jadx.ToolSupport.limitParam;
import static jadx.mcp.jadx.ToolSupport.offsetParam;
import static jadx.mcp.jadx.ToolSupport.optClassNameParam;
import static jadx.mcp.jadx.ToolSupport.withProject;

/**
 * Cross references based on jadx usage info. Paged by using top-level classes,
 * code lines resolved by decompiling only classes of the current page.
 */
final class XrefTools {
	private static final int MAX_LINE_LEN = 300;

	private XrefTools() {
	}

	static void register(ToolRegistry registry, McpBackend backend) {
		registry.register(McpTool.builder("get_xrefs_to_class")
				.title("Class usages")
				.description("Find where a class is used (including constructor calls). Paged by using classes; "
						+ "each usage includes enclosing method and code line.")
				.input(xrefInput(Schema.object().required("class_name", classNameParam())))
				.output(xrefOutputSchema())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaClass cls = resolver.resolveClass(args.getRequiredString("class_name"));
					Map<JavaNode, List<JavaNode>> query = new LinkedHashMap<>();
					query.put(cls, cls.getUseIn());
					for (JavaMethod mth : cls.getMethods()) {
						if (mth.isConstructor()) {
							query.put(mth, mth.getUseIn());
						}
					}
					return buildXrefs(lease, cls, query, args);
				}))
				.build());

		registry.register(McpTool.builder("get_xrefs_to_method")
				.title("Method usages")
				.description("Find callers of a method. By default includes usages of overriding/overridden methods "
						+ "(calls through super class or interface).")
				.input(xrefInput(Schema.object()
						.required("method_name", Schema.nonEmptyString("Method id, name or descriptor"))
						.optional("class_name", optClassNameParam())
						.optional("include_overrides", Schema.bool("Include usages of override-related methods", true))))
				.output(xrefOutputSchema())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaMethod mth = resolver.resolveMethod(args.getOptString("class_name"), args.getRequiredString("method_name"));
					Map<JavaNode, List<JavaNode>> query = new LinkedHashMap<>();
					List<JavaMethod> related = args.getBool("include_overrides", true) ? mth.getOverrideRelatedMethods() : List.of();
					if (related.isEmpty()) {
						query.put(mth, mth.getUseIn());
					} else {
						for (JavaMethod relatedMth : related) {
							query.put(relatedMth, relatedMth.getUseIn());
						}
						query.putIfAbsent(mth, mth.getUseIn());
					}
					return buildXrefs(lease, mth, query, args);
				}))
				.build());

		registry.register(McpTool.builder("get_xrefs_to_field")
				.title("Field usages")
				.description("Find reads and writes of a field. Note: usages of constant fields may be inlined "
						+ "by compiler and not reported.")
				.input(xrefInput(Schema.object()
						.required("field_name", Schema.nonEmptyString("Field id or name"))
						.optional("class_name", optClassNameParam())))
				.output(xrefOutputSchema())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaField fld = resolver.resolveField(args.getOptString("class_name"), args.getRequiredString("field_name"));
					Map<JavaNode, List<JavaNode>> query = new LinkedHashMap<>();
					query.put(fld, fld.getUseIn());
					return buildXrefs(lease, fld, query, args);
				}))
				.build());

		registry.register(McpTool.builder("get_method_calls")
				.title("Method callees")
				.description("List methods and fields used (called/accessed) by a method, from jadx usage info")
				.input(Schema.object()
						.required("method_name", Schema.nonEmptyString("Method id, name or descriptor"))
						.optional("class_name", optClassNameParam())
						.build())
				.output(Schema.object().open()
						.required("method", NodeJson.methodSummarySchema())
						.required("used", Schema.array(NodeJson.nodeRefSchema(), "Used nodes"))
						.required("unresolved", Schema.stringArray("Called methods not found in project (framework/library)"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					JavaMethod mth = resolver.resolveMethod(args.getOptString("class_name"), args.getRequiredString("method_name"));
					JsonObject result = new JsonObject();
					result.add("method", NodeJson.methodSummary(mth));
					JsonArray used = new JsonArray();
					for (JavaNode node : mth.getUsed()) {
						used.add(NodeJson.nodeRef(node));
					}
					result.add("used", used);
					JsonArray unresolved = new JsonArray();
					mth.getUnresolvedUsed().forEach(m -> unresolved.add(m.getRawFullId()));
					result.add("unresolved", unresolved);
					return result;
				}))
				.build());
	}

	private static JsonObject xrefInput(Schema.ObjectBuilder builder) {
		return builder
				.optional("include_code", Schema.bool("Decompile using classes to get exact lines", true))
				.optional("offset", offsetParam())
				.optional("limit", limitParam(20, 200))
				.build();
	}

	private static JsonObject xrefOutputSchema() {
		return NodeJson.pagingSchema(Schema.object().open())
				.required("target", NodeJson.nodeRefSchema())
				.required("usages", Schema.array(Schema.object().open()
						.required("class_id", Schema.string("Using top-level class id"))
						.required("class_name", Schema.string("Using class current name"))
						.required("node", NodeJson.nullableRefSchema("Using method/class"))
						.required("line", Schema.nullableInteger("Line in class code, null if not resolved"))
						.required("code", Schema.nullableString("Code line"))
						.build(), "Usages in classes of this page"))
				.required("pagination_unit", Schema.enumOf("Paging unit: 'offset', 'limit', 'total' and 'count' "
						+ "refer to using classes, not usages", null, "class"))
				.required("total_classes", Schema.integer("Total classes using the target"))
				.required("returned_classes", Schema.integer("Classes in this page"))
				.required("returned_usages", Schema.integer("Usage places (array 'usages' size) in this page"))
				.build();
	}

	private static JsonObject buildXrefs(ProjectLease lease, JavaNode target,
			Map<JavaNode, List<JavaNode>> query, ToolArgs args) {
		JadxDecompiler decompiler = lease.getDecompiler();
		// top class -> (searched node -> using nodes)
		Map<String, JavaClass> topClasses = new TreeMap<>();
		Map<JavaClass, Map<JavaNode, List<JavaNode>>> usesByClass = new LinkedHashMap<>();
		for (Map.Entry<JavaNode, List<JavaNode>> entry : query.entrySet()) {
			for (JavaNode useNode : entry.getValue()) {
				JavaClass top = useNode.getTopParentClass();
				if (top == null) {
					continue;
				}
				topClasses.putIfAbsent(NodeIds.classId(top), top);
				usesByClass.computeIfAbsent(top, k -> new LinkedHashMap<>())
						.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
						.add(useNode);
			}
		}
		lease.check();
		List<JavaClass> sorted = new ArrayList<>(topClasses.values());
		boolean includeCode = args.getBool("include_code", true);
		JsonObject result = new JsonObject();
		result.add("target", NodeJson.nodeRef(target));
		JsonArray usages = new JsonArray();
		for (JavaClass top : ToolSupport.pageSubList(result, sorted, args, 20)) {
			lease.check();
			Map<JavaNode, List<JavaNode>> uses = usesByClass.get(top);
			if (includeCode) {
				addCodeUsages(decompiler, top, uses, usages);
			} else {
				addNodeUsages(top, uses, usages);
			}
		}
		result.add("usages", usages);
		result.addProperty("pagination_unit", "class");
		result.addProperty("total_classes", result.get("total").getAsInt());
		result.addProperty("returned_classes", result.get("count").getAsInt());
		result.addProperty("returned_usages", usages.size());
		return result;
	}

	private static void addCodeUsages(JadxDecompiler decompiler, JavaClass top,
			Map<JavaNode, List<JavaNode>> uses, JsonArray usages) {
		ICodeInfo codeInfo = top.getCodeInfo();
		CodeText code = new CodeText(codeInfo.getCodeStr());
		Set<Integer> positions = new TreeSet<>();
		for (JavaNode searchNode : uses.keySet()) {
			positions.addAll(top.getUsePlacesFor(codeInfo, searchNode));
		}
		int added = 0;
		int lastLine = -1;
		for (int pos : positions) {
			int line = code.lineForPos(pos);
			String lineStr = code.getLine(line).trim();
			if (line == lastLine || lineStr.startsWith("import ")) {
				continue;
			}
			lastLine = line;
			JsonObject usage = new JsonObject();
			usage.addProperty("class_id", NodeIds.classId(top));
			usage.addProperty("class_name", top.getFullName());
			JavaNode enclosing = decompiler.getEnclosingNode(codeInfo, pos);
			usage.add("node", enclosing == null ? null : NodeJson.nodeRef(enclosing));
			usage.addProperty("line", line);
			usage.addProperty("code", lineStr.length() > MAX_LINE_LEN ? lineStr.substring(0, MAX_LINE_LEN) + "..." : lineStr);
			usages.add(usage);
			added++;
		}
		if (added == 0) {
			// usage not visible in code (inlined constant, synthetic accessor), report nodes
			addNodeUsages(top, uses, usages);
		}
	}

	private static void addNodeUsages(JavaClass top, Map<JavaNode, List<JavaNode>> uses, JsonArray usages) {
		List<JavaNode> nodes = new ArrayList<>();
		for (List<JavaNode> list : uses.values()) {
			for (JavaNode node : list) {
				if (!nodes.contains(node)) {
					nodes.add(node);
				}
			}
		}
		nodes.sort(Comparator.comparing(NodeIds::id));
		for (JavaNode node : nodes) {
			JsonObject usage = new JsonObject();
			usage.addProperty("class_id", NodeIds.classId(top));
			usage.addProperty("class_name", top.getFullName());
			usage.add("node", NodeJson.nodeRef(node));
			usage.add("line", null);
			usage.add("code", null);
			usages.add(usage);
		}
	}
}
