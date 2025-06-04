package jadx.gui.search.providers;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.api.resources.ResourceContentType;
import jadx.api.utils.CodeUtils;
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
	private final SearchDialog searchDialog;
	private final ResourceFilter resourceFilter;
	private final int sizeLimit;

	/**
	 * Resources queue for process. Using UI nodes to reuse loading cache
	 */
	private final Deque<JResource> resQueue;
	private int pos;

	private int loadErrors = 0;
	private int skipBySize = 0;

	public ResourceSearchProvider(MainWindow mw, SearchSettings searchSettings, SearchDialog searchDialog) {
		this.searchSettings = searchSettings;
		this.resourceFilter = searchSettings.getResourceFilter();
		this.sizeLimit = searchSettings.getResSizeLimit() * 1024 * 1024;
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
		if (resNode.getContentType() == ResourceContentType.CONTENT_TEXT) {
			int lineStart = 1 + CodeUtils.getNewLinePosBefore(content, newPos);
			int lineEnd = CodeUtils.getNewLinePosAfter(content, newPos);
			int end = lineEnd == -1 ? content.length() : lineEnd;
			String line = content.substring(lineStart, end);
			this.pos = end;
			return new JResSearchNode(resNode, line.trim(), newPos);
		} else {
			int start = Math.max(0, newPos - 30);
			int end = Math.min(newPos + 50, content.length());
			String line = content.substring(start, end);
			this.pos = newPos + searchString.length() + 1;
			return new JResSearchNode(resNode, line, newPos);
		}
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

	private boolean shouldProcess(JResource resNode) {
		if (resNode.getResFile().getType() == ResourceType.ARSC) {
			// don't check the size of generated resource table, it will also skip all subfiles
			return resourceFilter.isAnyFile()
					|| resourceFilter.getContentTypes().contains(ResourceContentType.CONTENT_TEXT)
					|| resourceFilter.getExtSet().contains("xml");
		}
		if (!isAllowedFileType(resNode)) {
			return false;
		}
		return isAllowedFileSize(resNode);
	}

	private boolean isAllowedFileType(JResource resNode) {
		ResourceFile resFile = resNode.getResFile();
		if (resourceFilter.isAnyFile()) {
			return true;
		}
		ResourceContentType resContentType = resNode.getContentType();
		if (resourceFilter.getContentTypes().contains(resContentType)) {
			return true;
		}
		String fileExt = CommonFileUtils.getFileExtension(resFile.getOriginalName());
		if (fileExt != null && resourceFilter.getExtSet().contains(fileExt)) {
			return true;
		}
		if (resContentType == ResourceContentType.CONTENT_UNKNOWN
				&& resourceFilter.getContentTypes().contains(ResourceContentType.CONTENT_BINARY)) {
			// treat unknown file type as binary
			return true;
		}
		return false;
	}

	private boolean isAllowedFileSize(JResource resNode) {
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
