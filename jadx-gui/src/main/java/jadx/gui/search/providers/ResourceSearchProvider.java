package jadx.gui.search.providers;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeWriter;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.ISearchProvider;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResSearchNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.SearchDialog;
import jadx.gui.utils.NLS;

public class ResourceSearchProvider implements ISearchProvider {
	private static final Logger LOG = LoggerFactory.getLogger(ResourceSearchProvider.class);

	private final SearchSettings searchSettings;
	private final Set<String> extSet;
	private final SearchDialog searchDialog;
	private final int sizeLimit;
	private boolean anyExt;

	/**
	 * Resources queue for process. Using UI nodes to reuse loading cache
	 */
	private final Deque<JResource> resQueue;
	private int pos;

	private int loadErrors = 0;
	private int skipBySize = 0;

	public ResourceSearchProvider(MainWindow mw, SearchSettings searchSettings, SearchDialog searchDialog) {
		this.searchSettings = searchSettings;
		this.sizeLimit = mw.getSettings().getSrhResourceSkipSize() * 1048576;
		this.extSet = buildAllowedFilesExtensions(mw.getSettings().getSrhResourceFileExt());
		this.searchDialog = searchDialog;
		JResource activeResource = searchSettings.getActiveResource();
		if (activeResource != null) {
			this.resQueue = new ArrayDeque<>(Collections.singleton(activeResource));
		} else {
			this.resQueue = initResQueue(mw);
		}
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		while (true) {
			if (cancelable.isCanceled()) {
				return null;
			}
			JResource resNode = getNextResFile(cancelable);
			if (resNode == null) {
				return null;
			}
			JNode newResult = search(resNode);
			if (newResult != null) {
				return newResult;
			}
			pos = 0;
			resQueue.removeLast();
			addChildren(resNode);
			if (resQueue.isEmpty()) {
				return null;
			}
		}
	}

	private JNode search(JResource resNode) {
		String content;
		try {
			content = resNode.getCodeInfo().getCodeStr();
		} catch (Exception e) {
			LOG.error("Failed to load resource node content", e);
			return null;
		}
		String searchString = searchSettings.getSearchString();
		int newPos = searchSettings.getSearchMethod().find(content, searchString, pos);
		if (newPos == -1) {
			return null;
		}
		int lineStart = content.lastIndexOf(ICodeWriter.NL, newPos) + ICodeWriter.NL.length();
		int lineEnd = content.indexOf(ICodeWriter.NL, newPos + searchString.length());
		int end = lineEnd == -1 ? content.length() : lineEnd;
		String line = content.substring(lineStart, end);
		this.pos = end;
		return new JResSearchNode(resNode, line.trim(), newPos);
	}

	private @Nullable JResource getNextResFile(Cancelable cancelable) {
		while (true) {
			JResource node = resQueue.peekLast();
			if (node == null || cancelable.isCanceled()) {
				return null;
			}
			if (node.getType() == JResource.JResType.FILE) {
				if (shouldProcess(node) && loadResNode(node)) {
					return node;
				}
				resQueue.removeLast();
			} else {
				// dir
				resQueue.removeLast();
				loadResNode(node);
				addChildren(node);
			}
		}
	}

	private void updateProgressInfo() {
		StringBuilder sb = new StringBuilder();
		if (loadErrors != 0) {
			sb.append("  ").append(NLS.str("search_dialog.resources_load_errors", loadErrors));
		}
		if (skipBySize != 0) {
			sb.append("  ").append(NLS.str("search_dialog.resources_skip_by_size", skipBySize));
		}
		if (sb.length() != 0) {
			sb.append("  ").append(NLS.str("search_dialog.resources_check_logs"));
		}
		searchDialog.updateProgressLabel(sb.toString());
	}

	private boolean loadResNode(JResource node) {
		try {
			node.loadNode();
			return true;
		} catch (Exception e) {
			LOG.error("Error load resource node: {}", node, e);
			loadErrors++;
			updateProgressInfo();
			return false;
		}
	}

	private void addChildren(JResource resNode) {
		resQueue.addAll(resNode.getSubNodes());
	}

	private static Deque<JResource> initResQueue(MainWindow mw) {
		JRoot jRoot = mw.getTreeRoot();
		Deque<JResource> deque = new ArrayDeque<>(jRoot.getChildCount());
		Enumeration<TreeNode> children = jRoot.children();
		while (children.hasMoreElements()) {
			TreeNode node = children.nextElement();
			if (node instanceof JResource) {
				JResource resNode = (JResource) node;
				deque.add(resNode);
			}
		}
		return deque;
	}

	private Set<String> buildAllowedFilesExtensions(String srhResourceFileExt) {
		Set<String> set = new HashSet<>();
		for (String extStr : srhResourceFileExt.split("[|.]")) {
			String ext = extStr.trim();
			if (!ext.isEmpty()) {
				anyExt = ext.equals("*");
				if (anyExt) {
					break;
				}
				set.add(ext);
			}
		}
		return set;
	}

	private boolean shouldProcess(JResource resNode) {
		ResourceFile resFile = resNode.getResFile();
		if (resFile.getType() == ResourceType.ARSC) {
			// don't check size of generated resource table, it will also skip all sub files
			return anyExt || extSet.contains("xml");
		}
		if (!anyExt) {
			String fileExt = CommonFileUtils.getFileExtension(resFile.getOriginalName());
			if (fileExt == null) {
				return false;
			}
			if (!extSet.contains(fileExt)) {
				return false;
			}
		}
		if (sizeLimit <= 0) {
			return true;
		}
		try {
			int charsCount = resNode.getCodeInfo().getCodeStr().length();
			long size = charsCount * 8L;
			if (size > sizeLimit) {
				LOG.info("Resource search skipped because of size limit. Resource '{}' size {} bytes, limit: {}",
						resNode.getName(), size, sizeLimit);
				skipBySize++;
				updateProgressInfo();
				return false;
			}
			return true;
		} catch (Exception e) {
			LOG.warn("Resource load error: {}", resNode, e);
			loadErrors++;
			updateProgressInfo();
			return false;
		}
	}

	@Override
	public int progress() {
		return 0;
	}

	@Override
	public int total() {
		return 0;
	}
}
