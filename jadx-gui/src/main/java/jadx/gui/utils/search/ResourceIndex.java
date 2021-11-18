package jadx.gui.utils.search;

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

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.utils.files.FileUtils;
import jadx.gui.treemodel.JResSearchNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.utils.CacheObject;

import static jadx.core.utils.StringUtils.countLinesByPos;
import static jadx.core.utils.StringUtils.getLine;

public class ResourceIndex {
	private static final Logger LOG = LoggerFactory.getLogger(ResourceIndex.class);

	private final List<JResource> resNodes = new ArrayList<>();
	private final Set<String> extSet = new HashSet<>();
	private final CacheObject cache;

	private String fileExts;
	private boolean anyExt;
	private int sizeLimit;

	public ResourceIndex(CacheObject cache) {
		this.cache = cache;
	}

	private void search(final JResource resNode,
			FlowableEmitter<JResSearchNode> emitter,
			SearchSettings searchSettings) {
		int line = 0;
		int lastPos = 0;
		int lastLineOccurred = -1;
		JResSearchNode lastNode = null;
		int searchStrLen = searchSettings.getSearchString().length();
		String content;
		try {
			content = resNode.getContent();
		} catch (Exception e) {
			LOG.error("Error load resource node content", e);
			return;
		}
		do {
			searchSettings.setStartPos(lastPos);
			int pos = searchSettings.find(content);
			if (pos > -1) {
				line += countLinesByPos(content, pos, lastPos);
				lastPos = pos + searchStrLen;
				String lineText = getLine(content, pos, lastPos);
				if (lastLineOccurred != line) {
					lastLineOccurred = line;
					if (lastNode != null) {
						emitter.onNext(lastNode);
					}
					lastNode = new JResSearchNode(resNode, lineText.trim(), line + 1, pos);
				}
			} else {
				if (lastNode != null) { // commit the final result node.
					emitter.onNext(lastNode);
				}
				break;
			}
		} while (!emitter.isCancelled() && lastPos < content.length());
	}

	public Flowable<JResSearchNode> search(SearchSettings settings) {
		refreshSettings();
		if (resNodes.size() == 0) {
			return Flowable.empty();
		}
		return Flowable.create(emitter -> {
			for (JResource resNode : resNodes) {
				if (!emitter.isCancelled()) {
					search(resNode, emitter, settings);
				}
			}
			emitter.onComplete();
		}, BackpressureStrategy.BUFFER);
	}

	public void index() {
		refreshSettings();
	}

	private void clear() {
		anyExt = false;
		sizeLimit = -1;
		fileExts = "";
		extSet.clear();
		resNodes.clear();
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
					size = resNode.getContent().length();
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
				LOG.debug("Resource skipped because of size limit: {} res size {} bytes", resNode, size);
			}
		}
	}

	private void refreshSettings() {
		int size = cache.getJadxSettings().getSrhResourceSkipSize() * 1048576;
		if (size != sizeLimit
				|| !cache.getJadxSettings().getSrhResourceFileExt().equals(fileExts)) {
			clear();
			sizeLimit = size;
			fileExts = cache.getJadxSettings().getSrhResourceFileExt();
			String[] exts = fileExts.split("\\|");
			for (String ext : exts) {
				ext = ext.trim();
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
	}
}
