package jadx.mcp.jadx;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.api.ICodeInfo;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolArgs;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;
import jadx.mcp.tools.ToolRegistry;

import static jadx.mcp.jadx.ToolSupport.DEFAULT_LIMIT;
import static jadx.mcp.jadx.ToolSupport.MAX_LIMIT;
import static jadx.mcp.jadx.ToolSupport.limitParam;
import static jadx.mcp.jadx.ToolSupport.offsetParam;
import static jadx.mcp.jadx.ToolSupport.pageList;
import static jadx.mcp.jadx.ToolSupport.withProject;

/**
 * Name and full text search. Code search decompiles classes on demand and returns a cursor
 * to continue when time budget or results limit reached.
 */
final class SearchTools {
	private static final int MAX_LINE_LEN = 300;
	private static final long DEFAULT_TIME_BUDGET_MS = 20_000;
	private static final long TIME_RESERVE_MS = 3_000;

	private SearchTools() {
	}

	static void register(ToolRegistry registry, McpBackend backend) {
		registry.register(McpTool.builder("search_classes_by_keyword")
				.title("Search classes")
				.description("Search classes by name (current, original and raw names). Fast, no decompilation.")
				.input(Schema.object()
						.required("keyword", Schema.nonEmptyString("Substring or regex of class full name"))
						.optional("regex", Schema.bool("Treat keyword as regular expression", false))
						.optional("case_sensitive", Schema.bool("Case sensitive match", false))
						.optional("include_inner", Schema.bool("Include inner classes", true))
						.optional("offset", offsetParam())
						.optional("limit", limitParam(DEFAULT_LIMIT, MAX_LIMIT))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("classes", Schema.array(NodeJson.classSummarySchema(), "Matched classes"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					Matcher matcher = buildMatcher(args, "keyword");
					boolean includeInner = args.getBool("include_inner", true);
					List<ClassNode> list = new ArrayList<>();
					for (ClassNode cls : lease.getDecompiler().getRoot().getClasses()) {
						if (cls.contains(AFlag.DONT_GENERATE) || (!includeInner && cls.isInner())) {
							continue;
						}
						ClassInfo info = cls.getClassInfo();
						if (find(matcher, info.getAliasFullName()) || find(matcher, info.getFullName())
								|| find(matcher, info.getRawName())) {
							list.add(cls);
						}
					}
					lease.check();
					list.sort(Comparator.comparing(c -> c.getClassInfo().getRawName()));
					JsonObject result = new JsonObject();
					result.add("classes", pageList(result, list, args, DEFAULT_LIMIT,
							c -> NodeJson.classSummary(resolver.toJava(c))));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("search_method_by_name")
				.title("Search methods")
				.description("Search methods by name (current or original) across all classes. Fast, no decompilation.")
				.input(memberSearchInput())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("methods", Schema.array(NodeJson.methodSummarySchema(), "Matched methods"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					Matcher matcher = buildMatcher(args, "keyword");
					String clsFilter = args.getOptString("class_filter");
					List<MethodNode> list = new ArrayList<>();
					for (ClassNode cls : filteredClasses(lease, clsFilter)) {
						for (MethodNode mth : cls.getMethods()) {
							MethodInfo info = mth.getMethodInfo();
							if (find(matcher, info.getAlias()) || find(matcher, info.getName())) {
								list.add(mth);
							}
						}
					}
					list.sort(Comparator.comparing(m -> m.getMethodInfo().getRawFullId()));
					JsonObject result = new JsonObject();
					result.add("methods", pageList(result, list, args, DEFAULT_LIMIT,
							m -> NodeJson.methodSummary(resolver.toJava(m))));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("search_field_by_name")
				.title("Search fields")
				.description("Search fields by name (current or original) across all classes. Fast, no decompilation.")
				.input(memberSearchInput())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("fields", Schema.array(NodeJson.fieldSummarySchema(), "Matched fields"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					Matcher matcher = buildMatcher(args, "keyword");
					String clsFilter = args.getOptString("class_filter");
					List<FieldNode> list = new ArrayList<>();
					for (ClassNode cls : filteredClasses(lease, clsFilter)) {
						for (FieldNode fld : cls.getFields()) {
							FieldInfo info = fld.getFieldInfo();
							if (find(matcher, info.getAlias()) || find(matcher, info.getName())) {
								list.add(fld);
							}
						}
					}
					list.sort(Comparator.comparing(f -> f.getFieldInfo().getRawFullId()));
					JsonObject result = new JsonObject();
					result.add("fields", pageList(result, list, args, DEFAULT_LIMIT,
							f -> NodeJson.fieldSummary(resolver.toJava(f))));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("search_code")
				.title("Search in code")
				.description("Full text search in decompiled Java code (strings, calls, identifiers). "
						+ "Classes are decompiled on demand (cached), so first search on big apps is slow: "
						+ "the call stops on results limit or time budget and returns 'next_cursor' to continue. "
						+ "Narrow scope with 'package' to speed up.")
				.input(Schema.object()
						.required("query", Schema.nonEmptyString("Text or regex to find"))
						.optional("regex", Schema.bool("Treat query as regular expression", false))
						.optional("case_sensitive", Schema.bool("Case sensitive match", false))
						.optional("package", Schema.string("Search only classes in this package (current or original name)"))
						.optional("max_results", limitParam(50, 500))
						.optional("cursor", Schema.string("Cursor from previous call to continue search"))
						.optional("time_budget_ms", Schema.integer("Max time for this call", 1000, 120_000, DEFAULT_TIME_BUDGET_MS))
						.build())
				.output(searchOutputSchema(Schema.object().open()
						.required("class_id", Schema.string("Top-level class id"))
						.required("class_name", Schema.string("Current class name"))
						.required("line", Schema.integer("Line in class code (1-based)"))
						.required("text", Schema.string("Line text (trimmed)"))
						.required("node", NodeJson.nullableRefSchema("Enclosing method/class"))
						.build()))
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> searchCode(ctx, lease, args)))
				.build());

		registry.register(McpTool.builder("search_resources")
				.title("Search in resources")
				.description("Full text search in decoded text resources: AndroidManifest.xml, XML layouts, "
						+ "values from resources.arsc, JSON/text files. Returns 'next_cursor' if not finished.")
				.input(Schema.object()
						.required("query", Schema.nonEmptyString("Text or regex to find"))
						.optional("regex", Schema.bool("Treat query as regular expression", false))
						.optional("case_sensitive", Schema.bool("Case sensitive match", false))
						.optional("file_filter", Schema.string("Case-insensitive substring of resource file name"))
						.optional("max_results", limitParam(50, 500))
						.optional("cursor", Schema.string("Cursor from previous call to continue search"))
						.optional("time_budget_ms", Schema.integer("Max time for this call", 1000, 120_000, DEFAULT_TIME_BUDGET_MS))
						.build())
				.output(searchOutputSchema(Schema.object().open()
						.required("file", Schema.string("Resource file name"))
						.required("line", Schema.integer("Line (1-based)"))
						.required("text", Schema.string("Line text (trimmed)"))
						.build()))
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> searchResources(ctx, lease, args)))
				.build());
	}

	private static JsonObject memberSearchInput() {
		return Schema.object()
				.required("keyword", Schema.nonEmptyString("Substring or regex of member name"))
				.optional("regex", Schema.bool("Treat keyword as regular expression", false))
				.optional("case_sensitive", Schema.bool("Case sensitive match", false))
				.optional("exact", Schema.bool("Name must match exactly (ignored for regex)", false))
				.optional("class_filter", Schema.string("Only classes with full name starting with this prefix (package or class)"))
				.optional("offset", offsetParam())
				.optional("limit", limitParam(DEFAULT_LIMIT, MAX_LIMIT))
				.build();
	}

	private static JsonObject searchOutputSchema(JsonObject matchSchema) {
		return Schema.object().open()
				.required("matches", Schema.array(matchSchema, "Matches"))
				.required("complete", Schema.bool("Search finished (no more results)"))
				.required("next_cursor", Schema.nullableString("Pass as 'cursor' to continue search"))
				.required("scanned", Schema.integer("Items scanned so far (classes or files)"))
				.required("total", Schema.integer("Total items to scan"))
				.optional("stop_reason", Schema.string("Why search stopped: 'complete', 'max_results' or 'time_budget'"))
				.build();
	}

	private static List<ClassNode> filteredClasses(ProjectLease lease, @Nullable String clsFilter) {
		List<ClassNode> list = new ArrayList<>();
		for (ClassNode cls : lease.getDecompiler().getRoot().getClasses()) {
			if (cls.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			if (clsFilter != null
					&& !cls.getClassInfo().getAliasFullName().startsWith(clsFilter)
					&& !cls.getClassInfo().getFullName().startsWith(clsFilter)) {
				continue;
			}
			list.add(cls);
		}
		return list;
	}

	/**
	 * Matcher for name/text search: substring (default), exact or regex
	 */
	private static Matcher buildMatcher(ToolArgs args, String paramName) throws ToolException {
		String query = args.getRequiredString(paramName);
		boolean regex = args.getBool("regex", false);
		boolean caseSensitive = args.getBool("case_sensitive", false);
		boolean exact = args.getBool("exact", false);
		int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
		try {
			String pattern;
			if (regex) {
				pattern = query;
			} else if (exact) {
				pattern = '^' + Pattern.quote(query) + '$';
			} else {
				pattern = Pattern.quote(query);
			}
			return Pattern.compile(pattern, flags).matcher("");
		} catch (PatternSyntaxException e) {
			throw ToolException.invalidArg("Invalid regex: " + e.getDescription());
		}
	}

	private static boolean find(Matcher matcher, @Nullable String str) {
		return str != null && matcher.reset(str).find();
	}

	private static long timeBudget(ToolCallContext ctx, ToolArgs args) {
		long budget = args.getInt("time_budget_ms", (int) DEFAULT_TIME_BUDGET_MS);
		long remaining = ctx.remainingMs();
		if (remaining != Long.MAX_VALUE) {
			budget = Math.min(budget, Math.max(1000, remaining - TIME_RESERVE_MS));
		}
		return budget;
	}

	/**
	 * Search cursor: project generation, query hash, item index and matches to skip in item
	 */
	static final class Cursor {
		final long generation;
		final int queryHash;
		final int index;
		final int skip;

		Cursor(long generation, int queryHash, int index, int skip) {
			this.generation = generation;
			this.queryHash = queryHash;
			this.index = index;
			this.skip = skip;
		}

		String encode() {
			String str = "v1:" + generation + ':' + queryHash + ':' + index + ':' + skip;
			return Base64.getUrlEncoder().withoutPadding().encodeToString(str.getBytes(StandardCharsets.UTF_8));
		}

		static Cursor decode(@Nullable String cursor, long generation, int queryHash) throws ToolException {
			if (cursor == null) {
				return new Cursor(generation, queryHash, 0, 0);
			}
			try {
				String str = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
				String[] parts = str.split(":");
				if (parts.length != 5 || !parts[0].equals("v1")) {
					throw new IllegalArgumentException();
				}
				Cursor c = new Cursor(Long.parseLong(parts[1]), Integer.parseInt(parts[2]),
						Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
				if (c.generation != generation) {
					throw new ToolException(ToolErrorCode.INVALID_CURSOR,
							"Project was reloaded since cursor was created, restart search without cursor");
				}
				if (c.queryHash != queryHash) {
					throw new ToolException(ToolErrorCode.INVALID_CURSOR,
							"Cursor belongs to a search with different parameters");
				}
				return c;
			} catch (ToolException e) {
				throw e;
			} catch (Exception e) {
				throw new ToolException(ToolErrorCode.INVALID_CURSOR, "Invalid cursor");
			}
		}
	}

	private static int queryHash(ToolArgs args, String... names) {
		StringBuilder sb = new StringBuilder();
		for (String name : names) {
			sb.append(name).append('=').append(args.getRaw().get(name)).append(';');
		}
		return sb.toString().hashCode();
	}

	private static JsonObject searchCode(ToolCallContext ctx, ProjectLease lease, ToolArgs args) throws ToolException {
		Matcher matcher = buildMatcher(args, "query");
		String pkg = args.getOptString("package");
		int maxResults = args.getInt("max_results", 50);
		int queryHash = queryHash(args, "query", "regex", "case_sensitive", "package");
		Cursor cursor = Cursor.decode(args.getOptString("cursor"), lease.getGeneration(), queryHash);
		long deadline = System.currentTimeMillis() + timeBudget(ctx, args);

		JadxDecompiler decompiler = lease.getDecompiler();
		List<JavaClass> classes = new ArrayList<>();
		for (JavaClass cls : decompiler.getClasses()) {
			ClassInfo info = cls.getClassNode().getClassInfo();
			if (pkg == null || ClassTools.inPackage(info.getAliasPkg(), pkg) || ClassTools.inPackage(info.getPackage(), pkg)) {
				classes.add(cls);
			}
		}
		classes.sort(Comparator.comparing(c -> c.getClassNode().getClassInfo().getRawName()));

		JsonArray matches = new JsonArray();
		int index = cursor.index;
		int skip = cursor.skip;
		String stopReason = "complete";
		Cursor next = null;
		while (index < classes.size()) {
			lease.check();
			if (System.currentTimeMillis() > deadline) {
				stopReason = "time_budget";
				next = new Cursor(lease.getGeneration(), queryHash, index, 0);
				break;
			}
			JavaClass cls = classes.get(index);
			ICodeInfo codeInfo = cls.getCodeInfo();
			String code = codeInfo.getCodeStr();
			CodeText codeText = null;
			int found = 0;
			int lastLine = -1;
			matcher.reset(new CancellableCharSequence(code, lease.getToken()));
			while (matcher.find()) {
				int line;
				if (codeText == null) {
					codeText = new CodeText(code);
				}
				line = codeText.lineForPos(matcher.start());
				if (line == lastLine) {
					continue;
				}
				lastLine = line;
				found++;
				if (found <= skip) {
					continue;
				}
				if (matches.size() >= maxResults) {
					next = new Cursor(lease.getGeneration(), queryHash, index, found - 1);
					break;
				}
				JsonObject match = new JsonObject();
				match.addProperty("class_id", NodeIds.classId(cls));
				match.addProperty("class_name", cls.getFullName());
				match.addProperty("line", line);
				match.addProperty("text", lineText(codeText, line));
				JavaNode enclosing = safeEnclosing(decompiler, codeInfo, matcher.start());
				match.add("node", enclosing == null ? null : NodeJson.nodeRef(enclosing));
				matches.add(match);
			}
			if (next != null) {
				stopReason = "max_results";
				break;
			}
			skip = 0;
			index++;
		}
		JsonObject result = new JsonObject();
		result.add("matches", matches);
		result.addProperty("complete", next == null);
		result.addProperty("next_cursor", next == null ? null : next.encode());
		result.addProperty("scanned", Math.min(index, classes.size()));
		result.addProperty("total", classes.size());
		result.addProperty("stop_reason", stopReason);
		return result;
	}

	private static @Nullable JavaNode safeEnclosing(JadxDecompiler decompiler, ICodeInfo codeInfo, int pos) {
		try {
			if (!codeInfo.hasMetadata()) {
				return null;
			}
			return decompiler.getEnclosingNode(codeInfo, pos);
		} catch (Exception e) {
			return null;
		}
	}

	private static String lineText(CodeText codeText, int line) {
		String text = codeText.getLine(line).trim();
		if (text.length() > MAX_LINE_LEN) {
			return text.substring(0, MAX_LINE_LEN) + "...";
		}
		return text;
	}

	private static JsonObject searchResources(ToolCallContext ctx, ProjectLease lease, ToolArgs args) throws ToolException {
		Matcher matcher = buildMatcher(args, "query");
		String fileFilter = args.getOptString("file_filter");
		String fileFilterLower = fileFilter == null ? null : fileFilter.toLowerCase(Locale.ROOT);
		int maxResults = args.getInt("max_results", 50);
		int queryHash = queryHash(args, "query", "regex", "case_sensitive", "file_filter");
		Cursor cursor = Cursor.decode(args.getOptString("cursor"), lease.getGeneration(), queryHash);
		long deadline = System.currentTimeMillis() + timeBudget(ctx, args);

		List<String> files = new ArrayList<>();
		for (String name : ResourceTools.listTextResourceNames(lease)) {
			if (fileFilterLower == null || name.toLowerCase(Locale.ROOT).contains(fileFilterLower)) {
				files.add(name);
			}
		}
		files.sort(String::compareTo);

		JsonArray matches = new JsonArray();
		int index = cursor.index;
		int skip = cursor.skip;
		String stopReason = "complete";
		Cursor next = null;
		while (index < files.size()) {
			lease.check();
			if (System.currentTimeMillis() > deadline) {
				stopReason = "time_budget";
				next = new Cursor(lease.getGeneration(), queryHash, index, 0);
				break;
			}
			String fileName = files.get(index);
			String text = ResourceTools.loadTextByName(lease, fileName);
			if (text != null) {
				CodeText codeText = new CodeText(text);
				int found = 0;
				for (int line = 1; line <= codeText.getLinesCount(); line++) {
					if (!matcher.reset(new CancellableCharSequence(codeText.getLine(line), lease.getToken())).find()) {
						continue;
					}
					found++;
					if (found <= skip) {
						continue;
					}
					if (matches.size() >= maxResults) {
						next = new Cursor(lease.getGeneration(), queryHash, index, found - 1);
						break;
					}
					JsonObject match = new JsonObject();
					match.addProperty("file", fileName);
					match.addProperty("line", line);
					match.addProperty("text", lineText(codeText, line));
					matches.add(match);
				}
			}
			if (next != null) {
				stopReason = "max_results";
				break;
			}
			skip = 0;
			index++;
		}
		JsonObject result = new JsonObject();
		result.add("matches", matches);
		result.addProperty("complete", next == null);
		result.addProperty("next_cursor", next == null ? null : next.encode());
		result.addProperty("scanned", Math.min(index, files.size()));
		result.addProperty("total", files.size());
		result.addProperty("stop_reason", stopReason);
		return result;
	}
}
