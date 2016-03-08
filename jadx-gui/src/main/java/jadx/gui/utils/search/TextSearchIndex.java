package jadx.gui.utils.search;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.codegen.CodeWriter;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.CommonSearchDialog;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.JNodeCache;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSearchIndex {

	private static final Logger LOG = LoggerFactory.getLogger(TextSearchIndex.class);

	private final JNodeCache nodeCache;

	private SearchIndex<JNode> clsNamesIndex;
	private SearchIndex<JNode> mthNamesIndex;
	private SearchIndex<JNode> fldNamesIndex;
	private SearchIndex<CodeNode> codeIndex;

	private List<JavaClass> skippedClasses = new ArrayList<JavaClass>();

	public TextSearchIndex(JNodeCache nodeCache) {
		this.nodeCache = nodeCache;
		this.clsNamesIndex = new SimpleIndex<JNode>();
		this.mthNamesIndex = new SimpleIndex<JNode>();
		this.fldNamesIndex = new SimpleIndex<JNode>();
		this.codeIndex = new CodeIndex<CodeNode>();
	}

	public void indexNames(JavaClass cls) {
		clsNamesIndex.put(cls.getFullName(), nodeCache.makeFrom(cls));
		for (JavaMethod mth : cls.getMethods()) {
			mthNamesIndex.put(mth.getFullName(), this.nodeCache.makeFrom(mth));
		}
		for (JavaField fld : cls.getFields()) {
			fldNamesIndex.put(fld.getFullName(), nodeCache.makeFrom(fld));
		}
		for (JavaClass innerCls : cls.getInnerClasses()) {
			indexNames(innerCls);
		}
	}

	public void indexCode(JavaClass cls, CodeLinesInfo linesInfo, List<StringRef> lines) {
		try {
			boolean strRefSupported = codeIndex.isStringRefSupported();
			int count = lines.size();
			for (int i = 0; i < count; i++) {
				StringRef line = lines.get(i);
				if (line.length() != 0 && line.charAt(0) != '}') {
					int lineNum = i + 1;
					JavaNode node = linesInfo.getJavaNodeByLine(lineNum);
					CodeNode codeNode = new CodeNode(nodeCache.makeFrom(node == null ? cls : node), lineNum, line);
					if (strRefSupported) {
						codeIndex.put(line, codeNode);
					} else {
						codeIndex.put(line.toString(), codeNode);
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to index class: {}", cls, e);
		}
	}

	public List<JNode> searchClsName(String text) {
		return clsNamesIndex.getValuesForKeysContaining(text);
	}

	public List<JNode> searchMthName(String text) {
		return mthNamesIndex.getValuesForKeysContaining(text);
	}

	public List<JNode> searchFldName(String text) {
		return fldNamesIndex.getValuesForKeysContaining(text);
	}

	public List<CodeNode> searchCode(String text) {
		List<CodeNode> items;
		if (codeIndex.size() > 0) {
			items = codeIndex.getValuesForKeysContaining(text);
			if (skippedClasses.isEmpty()) {
				return items;
			}
		} else {
			items = new ArrayList<CodeNode>();
		}
		addSkippedClasses(items, text);
		return items;
	}

	private void addSkippedClasses(List<CodeNode> list, String text) {
		for (JavaClass javaClass : skippedClasses) {
			String code = javaClass.getCode();
			int pos = 0;
			while (pos != -1) {
				pos = searchNext(list, text, javaClass, code, pos);
			}
			if (list.size() > CommonSearchDialog.MAX_RESULTS_COUNT) {
				return;
			}
		}
	}

	private int searchNext(List<CodeNode> list, String text, JavaNode javaClass, String code, int startPos) {
		int pos = code.indexOf(text, startPos);
		if (pos == -1) {
			return -1;
		}
		int lineStart = 1 + code.lastIndexOf(CodeWriter.NL, pos);
		int lineEnd = code.indexOf(CodeWriter.NL, pos + text.length());
		StringRef line = StringRef.subString(code, lineStart, lineEnd == -1 ? code.length() : lineEnd);
		list.add(new CodeNode(nodeCache.makeFrom(javaClass), -pos, line.trim()));
		return lineEnd;
	}

	public void classCodeIndexSkipped(JavaClass cls) {
		this.skippedClasses.add(cls);
	}

	public List<JavaClass> getSkippedClasses() {
		return skippedClasses;
	}

	public int getSkippedCount() {
		return skippedClasses.size();
	}
}
