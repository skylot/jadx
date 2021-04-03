package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;

import jadx.api.ICodeWriter;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.SearchDialog;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.UiUtils;

import static jadx.gui.ui.SearchDialog.SearchOptions.ACTIVE_TAB;
import static jadx.gui.ui.SearchDialog.SearchOptions.CLASS;
import static jadx.gui.ui.SearchDialog.SearchOptions.CODE;
import static jadx.gui.ui.SearchDialog.SearchOptions.COMMENT;
import static jadx.gui.ui.SearchDialog.SearchOptions.FIELD;
import static jadx.gui.ui.SearchDialog.SearchOptions.IGNORE_CASE;
import static jadx.gui.ui.SearchDialog.SearchOptions.METHOD;
import static jadx.gui.ui.SearchDialog.SearchOptions.RESOURCE;
import static jadx.gui.ui.SearchDialog.SearchOptions.USE_REGEX;

public class TextSearchIndex {

	private static final Logger LOG = LoggerFactory.getLogger(TextSearchIndex.class);

	private final CacheObject cache;
	private final MainWindow mainWindow;
	private final JNodeCache nodeCache;

	private final SimpleIndex clsNamesIndex;
	private final SimpleIndex mthSignaturesIndex;
	private final SimpleIndex fldSignaturesIndex;
	private final CodeIndex codeIndex;
	private final ResourceIndex resIndex;

	private final List<JavaClass> skippedClasses = new ArrayList<>();

	public TextSearchIndex(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.cache = mainWindow.getCacheObject();
		this.nodeCache = cache.getNodeCache();
		this.resIndex = new ResourceIndex(cache);
		this.clsNamesIndex = new SimpleIndex();
		this.mthSignaturesIndex = new SimpleIndex();
		this.fldSignaturesIndex = new SimpleIndex();
		this.codeIndex = new CodeIndex();
	}

	public void indexNames(JavaClass cls) {
		clsNamesIndex.put(cls.getFullName(), nodeCache.makeFrom(cls));
		for (JavaMethod mth : cls.getMethods()) {
			JNode mthNode = nodeCache.makeFrom(mth);
			mthSignaturesIndex.put(mthNode.makeDescString(), mthNode);
		}
		for (JavaField fld : cls.getFields()) {
			JNode fldNode = nodeCache.makeFrom(fld);
			fldSignaturesIndex.put(fldNode.makeDescString(), fldNode);
		}
		for (JavaClass innerCls : cls.getInnerClasses()) {
			indexNames(innerCls);
		}
	}

	public void indexCode(JavaClass cls, CodeLinesInfo linesInfo, List<StringRef> lines) {
		try {
			int count = lines.size();
			for (int i = 0; i < count; i++) {
				StringRef line = lines.get(i);
				int lineLength = line.length();
				if (lineLength == 0 || (lineLength == 1 && line.charAt(0) == '}')) {
					continue;
				}
				int lineNum = i + 1;
				JavaNode node = linesInfo.getJavaNodeByLine(lineNum);
				JavaNode javaNode = node == null ? cls : node;
				JNode nodeAtLine = nodeCache.makeFrom(javaNode);
				codeIndex.put(new CodeNode(nodeAtLine, line, lineNum, line.getOffset()));
			}
		} catch (Exception e) {
			LOG.warn("Failed to index class: {}", cls, e);
		}
	}

	public void indexResource() {
		resIndex.index();
	}

	public void remove(JavaClass cls) {
		this.clsNamesIndex.removeForCls(cls);
		this.mthSignaturesIndex.removeForCls(cls);
		this.fldSignaturesIndex.removeForCls(cls);
		this.codeIndex.removeForCls(cls);
	}

	public Flowable<JNode> buildSearch(String text, Set<SearchDialog.SearchOptions> options)
			throws SearchSettings.InvalidSearchTermException {
		boolean ignoreCase = options.contains(IGNORE_CASE);
		boolean useRegex = options.contains(USE_REGEX);

		LOG.debug("Building search, ignoreCase: {}, useRegex: {}", ignoreCase, useRegex);
		Flowable<JNode> result = Flowable.empty();

		SearchSettings searchSettings = new SearchSettings(text, options.contains(IGNORE_CASE), options.contains(USE_REGEX));
		if (options.contains(ACTIVE_TAB)) {
			JumpPosition activeNode = mainWindow.getTabbedPane().getCurrentPosition();
			if (activeNode != null) {
				searchSettings.setActiveCls(activeNode.getNode().getRootClass());
			}
			if (searchSettings.getActiveCls() == null) {
				return result;
			}
		}
		if (!searchSettings.preCompile()) {
			return result;
		}

		if (options.contains(COMMENT)) {
			CommentsIndex commentsIndex = cache.getCommentsIndex();
			result = Flowable.concat(result, commentsIndex.search(searchSettings));
			if (text.isEmpty()) {
				// return all comments on empty search string
				// other searches don't support empty string, so return immediately
				return result;
			}
		}

		if (options.contains(CLASS)) {
			result = Flowable.concat(result, clsNamesIndex.search(searchSettings));
		}
		if (options.contains(METHOD)) {
			result = Flowable.concat(result, mthSignaturesIndex.search(searchSettings));
		}
		if (options.contains(FIELD)) {
			result = Flowable.concat(result, fldSignaturesIndex.search(searchSettings));
		}
		if (options.contains(CODE)) {
			if (codeIndex.size() > 0) {
				result = Flowable.concat(result, codeIndex.search(searchSettings));
			}
			if (!skippedClasses.isEmpty()) {
				result = Flowable.concat(result, searchInSkippedClasses(searchSettings));
			}
		}
		if (options.contains(RESOURCE)) {
			result = Flowable.concat(result, resIndex.search(searchSettings));
		}
		return result;
	}

	public Flowable<CodeNode> searchInSkippedClasses(final SearchSettings searchSettings) {
		return Flowable.create(emitter -> {
			LOG.debug("Skipped code search started: {} ...", searchSettings.getSearchString());
			for (JavaClass javaClass : skippedClasses) {
				String code = javaClass.getCode();
				int pos = 0;
				while (pos != -1) {
					searchSettings.setStartPos(pos);
					pos = searchNext(emitter, javaClass, code, searchSettings);
					if (emitter.isCancelled()) {
						LOG.debug("Skipped Code search canceled: {}", searchSettings.getSearchString());
						return;
					}
				}
				if (!UiUtils.isFreeMemoryAvailable()) {
					LOG.warn("Skipped code search stopped due to memory limit: {}", UiUtils.memoryInfo());
					emitter.onComplete();
					return;
				}
			}
			LOG.debug("Skipped code search complete: {}, memory usage: {}", searchSettings.getSearchString(), UiUtils.memoryInfo());
			emitter.onComplete();
		}, BackpressureStrategy.BUFFER);
	}

	private int searchNext(FlowableEmitter<CodeNode> emitter, JavaNode javaClass, String code, final SearchSettings searchSettings) {
		int pos = searchSettings.find(code);
		if (pos == -1) {
			return -1;
		}
		int lineStart = 1 + code.lastIndexOf(ICodeWriter.NL, pos);
		int lineEnd = code.indexOf(ICodeWriter.NL, pos + searchSettings.getSearchString().length());
		StringRef line = StringRef.subString(code, lineStart, lineEnd == -1 ? code.length() : lineEnd);
		emitter.onNext(new CodeNode(nodeCache.makeFrom(javaClass), line.trim(), -1, pos));
		return lineEnd;
	}

	public void classCodeIndexSkipped(JavaClass cls) {
		this.skippedClasses.add(cls);
	}

	public int getSkippedCount() {
		return skippedClasses.size();
	}
}
