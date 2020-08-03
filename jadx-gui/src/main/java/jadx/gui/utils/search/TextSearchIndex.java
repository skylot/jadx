package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.codegen.CodeWriter;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.SearchDialog;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.UiUtils;

import static jadx.gui.ui.SearchDialog.SearchOptions.CLASS;
import static jadx.gui.ui.SearchDialog.SearchOptions.CODE;
import static jadx.gui.ui.SearchDialog.SearchOptions.FIELD;
import static jadx.gui.ui.SearchDialog.SearchOptions.IGNORE_CASE;
import static jadx.gui.ui.SearchDialog.SearchOptions.METHOD;

public class TextSearchIndex {

	private static final Logger LOG = LoggerFactory.getLogger(TextSearchIndex.class);

	private final JNodeCache nodeCache;

	private final SimpleIndex clsNamesIndex;
	private final SimpleIndex mthSignaturesIndex;
	private final SimpleIndex fldSignaturesIndex;
	private final CodeIndex codeIndex;

	private final List<JavaClass> skippedClasses = new ArrayList<>();

	public TextSearchIndex(JNodeCache nodeCache) {
		this.nodeCache = nodeCache;
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
				JNode nodeAtLine = nodeCache.makeFrom(node == null ? cls : node);
				codeIndex.put(new CodeNode(nodeAtLine, lineNum, line));
			}
		} catch (Exception e) {
			LOG.warn("Failed to index class: {}", cls, e);
		}
	}

	public void remove(JavaClass cls) {
		this.clsNamesIndex.removeForCls(cls);
		this.mthSignaturesIndex.removeForCls(cls);
		this.fldSignaturesIndex.removeForCls(cls);
		this.codeIndex.removeForCls(cls);
	}

	public Flowable<JNode> buildSearch(String text, Set<SearchDialog.SearchOptions> options) {
		boolean ignoreCase = options.contains(IGNORE_CASE);
		LOG.debug("Building search, ignoreCase: {}", ignoreCase);

		Flowable<JNode> result = Flowable.empty();
		if (options.contains(CLASS)) {
			result = Flowable.concat(result, clsNamesIndex.search(text, ignoreCase));
		}
		if (options.contains(METHOD)) {
			result = Flowable.concat(result, mthSignaturesIndex.search(text, ignoreCase));
		}
		if (options.contains(FIELD)) {
			result = Flowable.concat(result, fldSignaturesIndex.search(text, ignoreCase));
		}
		if (options.contains(CODE)) {
			if (codeIndex.size() > 0) {
				result = Flowable.concat(result, codeIndex.search(text, ignoreCase));
			}
			if (!skippedClasses.isEmpty()) {
				result = Flowable.concat(result, searchInSkippedClasses(text, ignoreCase));
			}
		}
		return result;
	}

	public Flowable<CodeNode> searchInSkippedClasses(final String searchStr, final boolean caseInsensitive) {
		return Flowable.create(emitter -> {
			LOG.debug("Skipped code search started: {} ...", searchStr);
			for (JavaClass javaClass : skippedClasses) {
				String code = javaClass.getCode();
				int pos = 0;
				while (pos != -1) {
					pos = searchNext(emitter, searchStr, javaClass, code, pos, caseInsensitive);
					if (emitter.isCancelled()) {
						LOG.debug("Skipped Code search canceled: {}", searchStr);
						return;
					}
				}
				if (!UiUtils.isFreeMemoryAvailable()) {
					LOG.warn("Skipped code search stopped due to memory limit: {}", UiUtils.memoryInfo());
					emitter.onComplete();
					return;
				}
			}
			LOG.debug("Skipped code search complete: {}, memory usage: {}", searchStr, UiUtils.memoryInfo());
			emitter.onComplete();
		}, BackpressureStrategy.LATEST);
	}

	private int searchNext(FlowableEmitter<CodeNode> emitter, String text, JavaNode javaClass, String code,
			int startPos, boolean ignoreCase) {
		int pos;
		if (ignoreCase) {
			pos = StringUtils.indexOfIgnoreCase(code, text, startPos);
		} else {
			pos = code.indexOf(text, startPos);
		}
		if (pos == -1) {
			return -1;
		}
		int lineStart = 1 + code.lastIndexOf(CodeWriter.NL, pos);
		int lineEnd = code.indexOf(CodeWriter.NL, pos + text.length());
		StringRef line = StringRef.subString(code, lineStart, lineEnd == -1 ? code.length() : lineEnd);
		emitter.onNext(new CodeNode(nodeCache.makeFrom(javaClass), -pos, line.trim()));
		return lineEnd;
	}

	public void classCodeIndexSkipped(JavaClass cls) {
		this.skippedClasses.add(cls);
	}

	public int getSkippedCount() {
		return skippedClasses.size();
	}
}
