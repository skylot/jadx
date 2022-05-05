package jadx.gui.search.providers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.tree.TreeNode;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeWriter;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.utils.files.FileUtils;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.ISearchProvider;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResSearchNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CacheObject;

public class ResourceSearchProvider implements ISearchProvider {
	private static final Logger LOG = LoggerFactory.getLogger(ResourceSearchProvider.class);

	private final CacheObject cache;
	private final SearchSettings searchSettings;
	private final Set<String> extSet = new HashSet<>();

	private List<JResource> resNodes;
	private String fileExts;
	private boolean anyExt;
	private int sizeLimit;

	private int progress;
	private int pos;

	public ResourceSearchProvider(MainWindow mw, SearchSettings searchSettings) {
		this.cache = mw.getCacheObject();
		this.searchSettings = searchSettings;
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		if (resNodes == null) {
			load();
		}
		if (resNodes.isEmpty()) {
			return null;
		}
		while (true) {
			if (cancelable.isCanceled()) {
				return null;
			}
			JResource resNode = resNodes.get(progress);
			JNode newResult = search(resNode);
			if (newResult != null) {
				return newResult;
			}
			progress++;
			pos = 0;
			if (progress >= resNodes.size()) {
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

	private synchronized void load() {
		resNodes = new ArrayList<>();
		sizeLimit = cache.getJadxSettings().getSrhResourceSkipSize() * 1048576;
		fileExts = cache.getJadxSettings().getSrhResourceFileExt();
		for (String extStr : fileExts.split("\\|")) {
			String ext = extStr.trim();
			if (!ext.isEmpty()) {
				anyExt = ext.equals("*");
				if (anyExt) {
					break;
				}
				extSet.add(ext);
			}
		}
		try (ZipFile zipFile = getZipFile(cache.getJRoot())) {
			traverseTree(cache.getJRoot(), zipFile); // reindex
		} catch (Exception e) {
			LOG.error("Failed to apply settings to resource index", e);
		}
	}

	private void traverseTree(TreeNode root, @Nullable ZipFile zip) {
		for (int i = 0; i < root.getChildCount(); i++) {
			TreeNode node = root.getChildAt(i);
			if (node instanceof JResource) {
				JResource resNode = (JResource) node;
				try {
					resNode.loadNode();
				} catch (Exception e) {
					LOG.error("Error load resource node: {}", resNode, e);
					return;
				}
				ResourceFile resFile = resNode.getResFile();
				if (resFile == null) {
					traverseTree(node, zip);
				} else {
					if (resFile.getType() == ResourceType.ARSC && shouldSearchXML()) {
						resFile.loadContent();
						resNode.getFiles().forEach(t -> traverseTree(t, null));
					} else {
						filter(resNode, zip);
					}
				}
			}
		}
	}

	private boolean shouldSearchXML() {
		return anyExt || fileExts.contains(".xml");
	}

	@Nullable
	private ZipFile getZipFile(TreeNode res) {
		for (int i = 0; i < res.getChildCount(); i++) {
			TreeNode node = res.getChildAt(i);
			if (node instanceof JResource) {
				JResource resNode = (JResource) node;
				try {
					resNode.loadNode();
				} catch (Exception e) {
					LOG.error("Error load resource node: {}", resNode, e);
					return null;
				}
				ResourceFile file = resNode.getResFile();
				if (file == null) {
					ZipFile zip = getZipFile(resNode);
					if (zip != null) {
						return zip;
					}
				} else {
					ResourceFile.ZipRef zipRef = file.getZipRef();
					if (zipRef != null) {
						File zfile = zipRef.getZipFile();
						if (FileUtils.isZipFile(zfile)) {
							try {
								return new ZipFile(zfile);
							} catch (IOException ignore) {
							}
						}
					}
				}
			}
		}
		return null;
	}

	private void filter(JResource resNode, ZipFile zip) {
		ResourceFile resFile = resNode.getResFile();
		if (JResource.isSupportedForView(resFile.getType())) {
			long size = -1;
			if (zip != null) {
				ZipEntry entry = zip.getEntry(resFile.getOriginalName());
				if (entry != null) {
					size = entry.getSize();
				}
			}
			if (size == -1) { // resource from ARSC is unknown size
				try {
					size = resNode.getCodeInfo().getCodeStr().length();
				} catch (Exception ignore) {
					return;
				}
			}
			if (size <= sizeLimit) {
				if (!anyExt) {
					for (String ext : extSet) {
						if (resFile.getOriginalName().endsWith(ext)) {
							resNodes.add(resNode);
							break;
						}
					}
				} else {
					resNodes.add(resNode);
				}
			} else {
				LOG.debug("Resource index skipped because of size limit: {} res size {} bytes", resNode, size);
			}
		}
	}

	@Override
	public int progress() {
		return progress;
	}

	@Override
	public int total() {
		return resNodes == null ? 0 : resNodes.size();
	}
}
