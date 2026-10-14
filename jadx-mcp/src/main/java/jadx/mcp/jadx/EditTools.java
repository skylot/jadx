package jadx.mcp.jadx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.api.ICodeInfo;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.JavaVariable;
import jadx.api.data.CommentStyle;
import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxNodeRef;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.mcp.project.CommentsUpdate;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.project.RenameRequest;
import jadx.mcp.project.SaveStatus;
import jadx.mcp.project.WriteResult;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolArgs;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;
import jadx.mcp.tools.ToolRegistry;

import static jadx.mcp.jadx.ToolSupport.classNameParam;
import static jadx.mcp.jadx.ToolSupport.limitParam;
import static jadx.mcp.jadx.ToolSupport.offsetParam;
import static jadx.mcp.jadx.ToolSupport.optClassNameParam;
import static jadx.mcp.jadx.ToolSupport.pageList;
import static jadx.mcp.jadx.ToolSupport.withProject;

/**
 * Project modification tools: renames and comments.
 * Changes are applied to jadx project data (shown in GUI and saved in .jadx project file),
 * input files are never modified. Calls are serialized.
 */
final class EditTools {
	private static final int MAX_UPDATED_CLASSES_IN_RESULT = 50;
	private static final Pattern PACKAGE_RENAME_PATTERN = Pattern.compile(
			"(\\.)?PKG(\\.PKG)*".replace("PKG", NameMapper.VALID_JAVA_IDENTIFIER.pattern()));

	private final McpBackend backend;
	private final ReentrantLock writeLock = new ReentrantLock(true);

	private EditTools(McpBackend backend) {
		this.backend = backend;
	}

	static void register(ToolRegistry registry, McpBackend backend) {
		new EditTools(backend).registerTools(registry);
	}

	private void registerTools(ToolRegistry registry) {
		String renameNote = " Changes jadx project data only (input APK/DEX is not modified); "
				+ "waits until code is updated. Set 'reset' to restore original name.";
		registry.register(McpTool.builder("rename_class")
				.title("Rename class")
				.description("Rename class. New name: short name ('MainActivity') or full name with package "
						+ "('com.app.ui.MainActivity', moves class; not allowed for inner classes)." + renameNote)
				.input(renameInput(Schema.object().required("class_name", classNameParam())))
				.output(renameOutputSchema())
				.write(false, true)
				.handler((ctx, args) -> rename(ctx, args,
						resolver -> resolver.resolveClass(args.getRequiredString("class_name"))))
				.build());

		registry.register(McpTool.builder("rename_method")
				.title("Rename method")
				.description("Rename method (override-related methods renamed together). Constructors can't be renamed."
						+ renameNote)
				.input(renameInput(Schema.object()
						.required("method_name", Schema.nonEmptyString("Method id, name or descriptor"))
						.optional("class_name", optClassNameParam())))
				.output(renameOutputSchema())
				.write(false, true)
				.handler((ctx, args) -> rename(ctx, args,
						resolver -> resolver.resolveMethod(args.getOptString("class_name"), args.getRequiredString("method_name"))))
				.build());

		registry.register(McpTool.builder("rename_field")
				.title("Rename field")
				.description("Rename field." + renameNote)
				.input(renameInput(Schema.object()
						.required("field_name", Schema.nonEmptyString("Field id or name"))
						.optional("class_name", optClassNameParam())))
				.output(renameOutputSchema())
				.write(false, true)
				.handler((ctx, args) -> rename(ctx, args,
						resolver -> resolver.resolveField(args.getOptString("class_name"), args.getRequiredString("field_name"))))
				.build());

		registry.register(McpTool.builder("rename_package")
				.title("Rename package")
				.description("Rename package (all classes moved). New name: full package name, e.g. 'com.app.network'."
						+ renameNote)
				.input(renameInput(Schema.object()
						.required("package_name", Schema.nonEmptyString("Package original or current full name"))))
				.output(renameOutputSchema())
				.write(false, true)
				.handler((ctx, args) -> rename(ctx, args,
						resolver -> resolver.resolvePackage(args.getRequiredString("package_name"))))
				.build());

		registry.register(McpTool.builder("rename_variable")
				.title("Rename variable")
				.description("Rename method parameter or local variable. Get variable ids from 'get_method_variables'."
						+ renameNote)
				.input(renameInput(Schema.object()
						.required("method_name", Schema.nonEmptyString("Method id, name or descriptor"))
						.optional("class_name", optClassNameParam())
						.required("variable", Schema.nonEmptyString("Variable id 'rNvM', full variable id or current name"))))
				.output(renameOutputSchema())
				.write(false, true)
				.handler((ctx, args) -> rename(ctx, args, resolver -> {
					JavaMethod mth = resolver.resolveMethod(args.getOptString("class_name"), args.getRequiredString("method_name"));
					return resolver.resolveVariable(mth, args.getRequiredString("variable"));
				}))
				.build());

		registry.register(McpTool.builder("get_comments")
				.title("List comments")
				.description("List user comments stored in jadx project (paged)")
				.input(Schema.object()
						.optional("class_name", Schema.string("Only comments in this class and its inner classes"))
						.optional("offset", offsetParam())
						.optional("limit", limitParam(100, 1000))
						.build())
				.output(NodeJson.pagingSchema(Schema.object().open())
						.required("comments", Schema.array(commentSchema(), "Comments"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					String classFilter = null;
					if (args.getOptString("class_name") != null) {
						classFilter = NodeIds.classId(resolver.resolveClass(args.getRequiredString("class_name")));
					}
					List<ICodeComment> list = new ArrayList<>();
					for (ICodeComment comment : backend.getComments(lease)) {
						String declCls = comment.getNodeRef().getDeclaringClass();
						if (classFilter == null || declCls.equals(classFilter) || declCls.startsWith(classFilter + '$')) {
							list.add(comment);
						}
					}
					JsonObject result = new JsonObject();
					result.add("comments", pageList(result, list, args, 100, EditTools::commentJson));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("set_comment")
				.title("Set comment")
				.description("Add or replace a comment in decompiled code. Target: class, method or field declaration "
						+ "('node_id'), or a code line inside a method ('line' in code of the top-level class, "
						+ "comment attached to instruction on that line). Stored in jadx project data.")
				.input(commentTargetInput(Schema.object()
						.required("comment", Schema.nonEmptyString("Comment text (multi-line allowed)"))
						.optional("style", Schema.enumOf("Comment style", CommentStyle.LINE.name(), commentStyles()))))
				.output(commentResultSchema())
				.write(false, true)
				.handler(this::setComment)
				.build());

		registry.register(McpTool.builder("remove_comment")
				.title("Remove comment")
				.description("Remove a comment at declaration ('node_id') or code line ('line')")
				.input(commentTargetInput(Schema.object()))
				.output(commentResultSchema())
				.write(true, true)
				.handler(this::removeComment)
				.build());

		registry.register(McpTool.builder("save_project")
				.title("Save project")
				.description("Save jadx project file (.jadx) with renames and comments. "
						+ "Works only if project already has a file (saved once from GUI); otherwise reports persisted=false.")
				.input(Schema.object().build())
				.output(Schema.object().open()
						.required("save", saveSchema())
						.build())
				.write(false, true)
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					lockWrite(ctx);
					try {
						SaveStatus status = backend.saveProject(lease, ctx);
						JsonObject result = new JsonObject();
						result.add("save", status.toJson());
						return result;
					} finally {
						writeLock.unlock();
					}
				}))
				.build());
	}

	@FunctionalInterface
	private interface NodeSupplier {
		JavaNode get(NodeResolver resolver) throws ToolException;
	}

	private static JsonObject renameInput(Schema.ObjectBuilder builder) {
		return builder
				.optional("new_name", Schema.string("New name (not required for reset)"))
				.optional("reset", Schema.bool("Remove user rename and restore original name", false))
				.build();
	}

	private static JsonObject renameOutputSchema() {
		return Schema.object().open()
				.required("success", Schema.bool("Rename applied and code refreshed"))
				.required("node", NodeJson.nodeRefSchema())
				.required("old_name", Schema.string("Name before rename"))
				.required("new_name", Schema.string("Current name after rename"))
				.required("reset", Schema.bool("Reset to original name requested"))
				.required("updated_classes_count", Schema.integer("Top-level classes refreshed"))
				.required("updated_classes", Schema.stringArray("Refreshed class ids (up to 50)"))
				.required("save", saveSchema())
				.optional("warning", Schema.string("Warning about partial result"))
				.build();
	}

	private static JsonObject saveSchema() {
		return Schema.object().open()
				.required("persisted", Schema.bool("Changes written to project file"))
				.required("project_file", Schema.nullableString("Project file path"))
				.required("auto_save", Schema.string("Project auto save mode"))
				.required("message", Schema.nullableString("Explanation if not persisted"))
				.build();
	}

	private void lockWrite(ToolCallContext ctx) throws ToolException {
		try {
			long wait = Math.min(ctx.remainingMs(), 60_000);
			if (!writeLock.tryLock(wait, TimeUnit.MILLISECONDS)) {
				throw new ToolException(ToolErrorCode.BUSY, "Another modification is in progress, retry later");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ToolException(ToolErrorCode.CANCELLED, "Interrupted");
		}
	}

	private JsonObject rename(ToolCallContext ctx, ToolArgs args, NodeSupplier nodeSupplier) throws ToolException {
		boolean reset = args.getBool("reset", false);
		String newName = args.getOptString("new_name");
		if (!reset && newName == null) {
			throw ToolException.invalidArg("'new_name' is required (or set 'reset': true)");
		}
		return withProject(backend, ctx, (lease, resolver) -> {
			lockWrite(ctx);
			try {
				JavaNode node = nodeSupplier.get(resolver);
				String name = reset ? "" : Objects.requireNonNull(newName);
				if (!reset) {
					checkRename(node, name);
				} else {
					checkCanRename(node);
				}
				String oldName = currentName(node);
				if (!reset && name.equals(oldName)) {
					throw ToolException.invalidArg("New name is the same as current name: " + name);
				}
				WriteResult writeResult = backend.rename(lease, new RenameRequest(node, name, reset), ctx);
				JavaNode updatedNode = refreshNode(resolver, node);
				String currentName = currentName(updatedNode);
				JsonObject result = new JsonObject();
				boolean applied = reset || name.equals(currentName) || currentName.endsWith('.' + name);
				result.addProperty("success", applied);
				result.add("node", NodeJson.nodeRef(updatedNode));
				result.addProperty("old_name", oldName);
				result.addProperty("new_name", currentName);
				result.addProperty("reset", reset);
				addUpdatedClasses(result, writeResult);
				if (!applied) {
					result.addProperty("warning", "Rename stored in project, but current name is '" + currentName
							+ "'. Check name validity or conflicts.");
				} else if (writeResult.getWarning() != null) {
					result.addProperty("warning", writeResult.getWarning());
				}
				return result;
			} finally {
				writeLock.unlock();
			}
		});
	}

	private static void addUpdatedClasses(JsonObject result, WriteResult writeResult) {
		List<String> updated = writeResult.getUpdatedClasses();
		result.addProperty("updated_classes_count", updated.size());
		JsonArray arr = new JsonArray();
		updated.stream().limit(MAX_UPDATED_CLASSES_IN_RESULT).forEach(arr::add);
		result.add("updated_classes", arr);
		result.add("save", writeResult.getSaveStatus().toJson());
	}

	/**
	 * Same rules as GUI rename dialog
	 */
	static void checkRename(JavaNode node, String newName) throws ToolException {
		checkCanRename(node);
		boolean valid;
		if (node instanceof JavaClass) {
			JavaClass cls = (JavaClass) node;
			if (NameMapper.isValidIdentifier(newName)) {
				valid = true;
			} else if (cls.isInner()) {
				throw ToolException.invalidArg("Invalid name for inner class (package change not allowed): " + newName);
			} else {
				valid = NameMapper.isValidFullIdentifier(newName)
						|| newName.startsWith(".") && NameMapper.isValidIdentifier(newName.substring(1));
			}
		} else if (node instanceof JavaPackage) {
			valid = isValidPackageName(newName);
		} else {
			valid = NameMapper.isValidIdentifier(newName);
		}
		if (!valid) {
			throw ToolException.invalidArg("Invalid Java identifier for " + NodeIds.kind(node) + ": '" + newName + "'");
		}
	}

	static void checkCanRename(JavaNode node) throws ToolException {
		if (node instanceof JavaMethod) {
			JavaMethod mth = (JavaMethod) node;
			if (mth.isConstructor() || mth.isClassInit()) {
				throw new ToolException(ToolErrorCode.UNSUPPORTED, "Constructors can't be renamed, rename the class instead");
			}
			if (mth.getMethodNode().contains(AFlag.DONT_RENAME)) {
				throw new ToolException(ToolErrorCode.UNSUPPORTED,
						"Method can't be renamed (overrides library method or marked as not renamable)");
			}
		} else if (node instanceof JavaVariable) {
			if (((JavaVariable) node).getName() == null) {
				throw new ToolException(ToolErrorCode.UNSUPPORTED, "Variable can't be renamed");
			}
		} else if (node instanceof JavaPackage) {
			if (((JavaPackage) node).isDefault()) {
				throw new ToolException(ToolErrorCode.UNSUPPORTED, "Default package can't be renamed");
			}
		}
	}

	static boolean isValidPackageName(String newName) {
		if (newName.isEmpty() || NameMapper.isReserved(newName) || !PACKAGE_RENAME_PATTERN.matcher(newName).matches()) {
			return false;
		}
		for (String part : newName.split("\\.")) {
			if (NameMapper.isReserved(part)) {
				return false;
			}
		}
		return true;
	}

	static String currentName(JavaNode node) {
		if (node instanceof JavaClass) {
			return ((JavaClass) node).getClassNode().getClassInfo().getAliasFullName();
		}
		if (node instanceof JavaPackage) {
			return ((JavaPackage) node).getFullName();
		}
		String name = node.getName();
		return name == null ? "" : name;
	}

	/**
	 * Get node after code reload (variables are recreated on decompilation)
	 */
	private static JavaNode refreshNode(NodeResolver resolver, JavaNode node) throws ToolException {
		if (node instanceof JavaVariable) {
			JavaVariable var = (JavaVariable) node;
			return resolver.resolveVariable(var.getMth(), NodeIds.varShortId(var.getReg(), var.getSsa()));
		}
		if (node instanceof JavaPackage) {
			return resolver.resolvePackage(((JavaPackage) node).getRawFullName());
		}
		return node;
	}

	private static String[] commentStyles() {
		CommentStyle[] values = CommentStyle.values();
		String[] names = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			names[i] = values[i].name();
		}
		return names;
	}

	private static JsonObject commentTargetInput(Schema.ObjectBuilder builder) {
		return builder
				.required("node_id", Schema.nonEmptyString("Class, method or field id (or class name). "
						+ "With 'line': any node of the class containing the line"))
				.optional("line", Schema.integer("Line in decompiled code of top-level class (1-based) "
						+ "to attach comment to instruction on this line", 1, Integer.MAX_VALUE, null))
				.build();
	}

	private static JsonObject commentResultSchema() {
		return Schema.object().open()
				.required("success", Schema.bool("Change applied and code refreshed"))
				.required("target", commentSchema())
				.required("action", Schema.enumOf("Performed action", null, "added", "replaced", "removed"))
				.required("updated_classes_count", Schema.integer("Top-level classes refreshed"))
				.required("updated_classes", Schema.stringArray("Refreshed class ids"))
				.required("save", saveSchema())
				.build();
	}

	private static JsonObject commentSchema() {
		return Schema.object().open()
				.required("target_kind", Schema.string("CLASS, METHOD, FIELD or PKG"))
				.required("class_id", Schema.string("Declaring class id"))
				.required("node_id", Schema.string("Commented node id"))
				.required("code_ref", Schema.nullableString("Instruction reference inside method ('INSN:offset'), null for declaration"))
				.required("comment", Schema.string("Comment text"))
				.required("style", Schema.string("Comment style"))
				.build();
	}

	static JsonObject commentJson(ICodeComment comment) {
		IJavaNodeRef nodeRef = comment.getNodeRef();
		JsonObject obj = new JsonObject();
		obj.addProperty("target_kind", nodeRef.getType().name());
		obj.addProperty("class_id", nodeRef.getDeclaringClass());
		String shortId = nodeRef.getShortId();
		obj.addProperty("node_id", shortId == null || shortId.isEmpty()
				? nodeRef.getDeclaringClass()
				: nodeRef.getDeclaringClass() + NodeIds.MEMBER_SEPARATOR + shortId);
		IJavaCodeRef codeRef = comment.getCodeRef();
		obj.addProperty("code_ref", codeRef == null ? null : codeRef.getAttachType().name() + ':' + codeRef.getIndex());
		obj.addProperty("comment", comment.getComment());
		obj.addProperty("style", comment.getStyle().name());
		return obj;
	}

	/**
	 * Comment target: node ref and optional instruction ref
	 */
	private static final class CommentTarget {
		final JadxNodeRef nodeRef;
		final @Nullable IJavaCodeRef codeRef;
		final JavaClass topClass;

		CommentTarget(JadxNodeRef nodeRef, @Nullable IJavaCodeRef codeRef, JavaClass topClass) {
			this.nodeRef = nodeRef;
			this.codeRef = codeRef;
			this.topClass = topClass;
		}

		boolean matches(ICodeComment comment) {
			return Objects.equals(comment.getNodeRef(), nodeRef) && Objects.equals(comment.getCodeRef(), codeRef);
		}
	}

	private static CommentTarget resolveCommentTarget(ProjectLease lease, NodeResolver resolver, ToolArgs args)
			throws ToolException {
		JavaNode node = resolver.resolveById(args.getRequiredString("node_id"));
		Integer line = args.getOptInt("line");
		if (line == null) {
			JadxNodeRef nodeRef = JadxNodeRef.forJavaNode(node);
			if (nodeRef == null) {
				throw ToolException.invalidArg("Comments supported only for class, method or field, got: " + NodeIds.kind(node));
			}
			return new CommentTarget(nodeRef, null, node.getTopParentClass());
		}
		JavaClass topCls = node.getTopParentClass();
		if (topCls == null) {
			throw ToolException.invalidArg("Can't find class code for node: " + NodeIds.id(node));
		}
		JadxDecompiler decompiler = lease.getDecompiler();
		ICodeInfo codeInfo = topCls.getCodeInfo();
		lease.check();
		CodeText code = new CodeText(codeInfo.getCodeStr());
		if (line > code.getLinesCount()) {
			throw ToolException.invalidArg("Line " + line + " is out of range, class code has " + code.getLinesCount() + " lines");
		}
		ICodeMetadata metadata = codeInfo.getCodeMetadata();
		int lineStart = code.lineStart(line);
		int lineEnd = Math.max(lineStart, code.lineEnd(line) - 1);
		// instruction on this line
		ICodeAnnotation offsetAnn = metadata.searchUp(lineEnd, lineStart, ICodeAnnotation.AnnType.OFFSET);
		if (offsetAnn instanceof InsnCodeOffset) {
			JavaNode enclosing = decompiler.getJavaNodeByRef(metadata.getNodeAt(lineEnd));
			if (enclosing instanceof JavaMethod) {
				int rawOffset = ((InsnCodeOffset) offsetAnn).getOffset();
				return new CommentTarget(JadxNodeRef.forMth((JavaMethod) enclosing), JadxCodeRef.forInsn(rawOffset), topCls);
			}
		}
		// declaration on this line
		ICodeNodeRef[] declared = new ICodeNodeRef[1];
		metadata.searchDown(lineStart, (pos, ann) -> {
			if (pos > lineEnd) {
				return Boolean.TRUE;
			}
			if (ann.getAnnType() == ICodeAnnotation.AnnType.DECLARATION) {
				ICodeNodeRef declNode = ((NodeDeclareRef) ann).getNode();
				if (declNode.getAnnType() != ICodeAnnotation.AnnType.VAR) {
					declared[0] = declNode;
					return Boolean.TRUE;
				}
			}
			return null;
		});
		if (declared[0] != null) {
			JadxNodeRef nodeRef = JadxNodeRef.forJavaNode(decompiler.getJavaNodeByRef(declared[0]));
			if (nodeRef != null) {
				return new CommentTarget(nodeRef, null, topCls);
			}
		}
		throw ToolException.invalidArg("No instruction or declaration at line " + line + ": '"
				+ code.getLine(line).trim() + "'. Comment can be attached to code lines inside methods or to declarations.");
	}

	private JsonObject setComment(ToolCallContext ctx, ToolArgs args) throws ToolException {
		return withProject(backend, ctx, (lease, resolver) -> {
			lockWrite(ctx);
			try {
				CommentTarget target = resolveCommentTarget(lease, resolver, args);
				String text = args.getRequiredString("comment");
				CommentStyle style = CommentStyle.valueOf(args.getString("style"));
				JadxCodeComment newComment = new JadxCodeComment(target.nodeRef, target.codeRef, text, style);
				boolean replaced = backend.getComments(lease).stream().anyMatch(target::matches);
				WriteResult writeResult = backend.updateComments(lease, new CommentsUpdate(target.topClass, list -> {
					list.removeIf(target::matches);
					list.add(newComment);
				}), ctx);
				JsonObject result = new JsonObject();
				result.addProperty("success", true);
				result.add("target", commentJson(newComment));
				result.addProperty("action", replaced ? "replaced" : "added");
				addUpdatedClasses(result, writeResult);
				return result;
			} finally {
				writeLock.unlock();
			}
		});
	}

	private JsonObject removeComment(ToolCallContext ctx, ToolArgs args) throws ToolException {
		return withProject(backend, ctx, (lease, resolver) -> {
			lockWrite(ctx);
			try {
				CommentTarget target = resolveCommentTarget(lease, resolver, args);
				ICodeComment existing = backend.getComments(lease).stream()
						.filter(target::matches)
						.findFirst()
						.orElseThrow(() -> ToolException.notFound("No comment at this target"));
				WriteResult writeResult = backend.updateComments(lease,
						new CommentsUpdate(target.topClass, list -> list.removeIf(target::matches)), ctx);
				JsonObject result = new JsonObject();
				result.addProperty("success", true);
				result.add("target", commentJson(existing));
				result.addProperty("action", "removed");
				addUpdatedClasses(result, writeResult);
				return result;
			} finally {
				writeLock.unlock();
			}
		});
	}
}
