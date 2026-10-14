package jadx.mcp.jadx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.JavaVariable;
import jadx.api.data.ICodeComment;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.mcp.project.CommentsUpdate;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectGate;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.project.RenameRequest;
import jadx.mcp.project.SaveStatus;
import jadx.mcp.project.UiState;
import jadx.mcp.project.WriteResult;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;

/**
 * Backend without GUI: works directly with decompiler instance.
 * Project data changes are kept in memory (decompiler args code data) and not persisted.
 */
public class HeadlessBackend implements McpBackend {
	private static final SaveStatus NOT_PERSISTED = new SaveStatus(false, null, "none",
			"Headless mode: changes kept in memory only");

	private final ProjectGate gate = new ProjectGate();

	public void open(JadxDecompiler decompiler) {
		gate.setReady(decompiler);
	}

	public void close() {
		gate.close(10_000);
	}

	@Override
	public ProjectGate getGate() {
		return gate;
	}

	@Override
	public void fillProjectInfo(ProjectLease lease, JsonObject info) {
		info.addProperty("name", "headless");
		info.add("project_file", null);
	}

	@Override
	public boolean hasUi() {
		return false;
	}

	@Override
	public UiState getUiState(ProjectLease lease, ToolCallContext ctx) throws ToolException {
		throw new ToolException(ToolErrorCode.UNSUPPORTED, "GUI is not available");
	}

	@Override
	public void openInUi(ProjectLease lease, JavaNode node, @Nullable Integer line, ToolCallContext ctx) throws ToolException {
		throw new ToolException(ToolErrorCode.UNSUPPORTED, "GUI is not available");
	}

	@Override
	public synchronized WriteResult rename(ProjectLease lease, RenameRequest request, ToolCallContext ctx) {
		JadxDecompiler decompiler = lease.getDecompiler();
		JadxCodeData codeData = getCodeData(decompiler);
		JavaNode node = request.getNode();
		Set<ICodeRename> renames = new HashSet<>(codeData.getRenames());
		ICodeRename rename = buildCodeRename(node, request.getNewName(), renames);
		renames.remove(rename);
		if (request.isReset()) {
			node.removeAlias();
		} else {
			renames.add(rename);
		}
		List<ICodeRename> list = new ArrayList<>(renames);
		Collections.sort(list);
		codeData.setRenames(list);
		List<JavaNode> toUpdate = new ArrayList<>();
		collectUpdateNodes(node, toUpdate);
		decompiler.reloadCodeData();
		return new WriteResult(unloadTopClasses(toUpdate), NOT_PERSISTED, null);
	}

	@Override
	public synchronized WriteResult updateComments(ProjectLease lease, CommentsUpdate update, ToolCallContext ctx) {
		JadxDecompiler decompiler = lease.getDecompiler();
		JadxCodeData codeData = getCodeData(decompiler);
		List<ICodeComment> list = new ArrayList<>(codeData.getComments());
		update.getUpdater().accept(list);
		Collections.sort(list);
		codeData.setComments(list);
		decompiler.reloadCodeData();
		return new WriteResult(unloadTopClasses(Collections.singletonList(update.getTopClass())), NOT_PERSISTED, null);
	}

	@Override
	public List<ICodeComment> getComments(ProjectLease lease) {
		return new ArrayList<>(getCodeData(lease.getDecompiler()).getComments());
	}

	@Override
	public SaveStatus getSaveStatus() {
		return NOT_PERSISTED;
	}

	@Override
	public SaveStatus saveProject(ProjectLease lease, ToolCallContext ctx) {
		return NOT_PERSISTED;
	}

	private static synchronized JadxCodeData getCodeData(JadxDecompiler decompiler) {
		JadxCodeData codeData = (JadxCodeData) decompiler.getArgs().getCodeData();
		if (codeData == null) {
			codeData = new JadxCodeData();
			decompiler.getArgs().setCodeData(codeData);
		}
		return codeData;
	}

	/**
	 * Unload code of changed classes, code regenerated with new names on next access
	 */
	private static List<String> unloadTopClasses(List<JavaNode> nodes) {
		Map<String, JavaClass> topClasses = new TreeMap<>();
		for (JavaNode node : nodes) {
			JavaClass top = node.getTopParentClass();
			if (top != null) {
				topClasses.put(NodeIds.classId(top), top);
			}
		}
		for (JavaClass cls : topClasses.values()) {
			cls.unload();
		}
		return new ArrayList<>(topClasses.keySet());
	}

	/**
	 * Same rename entries as created by jadx-gui
	 */
	static ICodeRename buildCodeRename(JavaNode node, String newName, Set<ICodeRename> renames) {
		if (node instanceof JavaClass) {
			return new JadxCodeRename(JadxNodeRef.forCls((JavaClass) node), newName);
		}
		if (node instanceof JavaMethod) {
			JavaMethod mth = (JavaMethod) node;
			for (JavaMethod relatedMethod : mth.getOverrideRelatedMethods()) {
				renames.remove(new JadxCodeRename(JadxNodeRef.forMth(relatedMethod), ""));
			}
			return new JadxCodeRename(JadxNodeRef.forMth(mth), newName);
		}
		if (node instanceof JavaField) {
			return new JadxCodeRename(JadxNodeRef.forFld((JavaField) node), newName);
		}
		if (node instanceof JavaPackage) {
			return new JadxCodeRename(JadxNodeRef.forPkg(((JavaPackage) node).getRawFullName()), newName);
		}
		if (node instanceof JavaVariable) {
			JavaVariable var = (JavaVariable) node;
			return new JadxCodeRename(JadxNodeRef.forMth(var.getMth()), JadxCodeRef.forVar(var), newName);
		}
		throw new IllegalArgumentException("Unsupported node for rename: " + node);
	}

	static void collectUpdateNodes(JavaNode node, List<JavaNode> toUpdate) {
		if (node instanceof JavaClass) {
			toUpdate.add(node);
			toUpdate.addAll(node.getUseIn());
		} else if (node instanceof JavaMethod) {
			JavaMethod mth = (JavaMethod) node;
			toUpdate.add(mth);
			toUpdate.addAll(mth.getUseIn());
			for (JavaMethod ovrdMth : mth.getOverrideRelatedMethods()) {
				toUpdate.add(ovrdMth);
				toUpdate.addAll(ovrdMth.getUseIn());
			}
		} else if (node instanceof JavaField) {
			toUpdate.add(node);
			toUpdate.addAll(node.getUseIn());
		} else if (node instanceof JavaPackage) {
			((JavaPackage) node).addUseIn(toUpdate);
		} else if (node instanceof JavaVariable) {
			toUpdate.add(((JavaVariable) node).getMth());
		}
		toUpdate.removeIf(Objects::isNull);
	}
}
