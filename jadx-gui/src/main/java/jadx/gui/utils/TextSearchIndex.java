package jadx.gui.utils;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.codegen.CodeWriter;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.CommonSearchDialog;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSearchIndex {

	private static final Logger LOG = LoggerFactory.getLogger(TextSearchIndex.class);

	private SuffixTree<JNode> clsNamesTree;
	private SuffixTree<JNode> mthNamesTree;
	private SuffixTree<JNode> fldNamesTree;
	private SuffixTree<CodeNode> codeTree;

	private List<JavaClass> skippedClasses = new ArrayList<JavaClass>();

	public TextSearchIndex() {
		clsNamesTree = new SuffixTree<JNode>();
		mthNamesTree = new SuffixTree<JNode>();
		fldNamesTree = new SuffixTree<JNode>();
		codeTree = new SuffixTree<CodeNode>();
	}

	public void indexNames(JavaClass cls) {
		clsNamesTree.put(cls.getFullName(), JNode.makeFrom(cls));
		for (JavaMethod mth : cls.getMethods()) {
			mthNamesTree.put(mth.getFullName(), JNode.makeFrom(mth));
		}
		for (JavaField fld : cls.getFields()) {
			fldNamesTree.put(fld.getFullName(), JNode.makeFrom(fld));
		}
		for (JavaClass innerCls : cls.getInnerClasses()) {
			indexNames(innerCls);
		}
	}

	public void indexCode(JavaClass cls, CodeLinesInfo linesInfo, String[] lines) {
		try {
			int count = lines.length;
			for (int i = 0; i < count; i++) {
				String line = lines[i];
				if (!line.isEmpty()) {
					int lineNum = i + 1;
					JavaNode node = linesInfo.getJavaNodeByLine(lineNum);
					codeTree.put(line, new CodeNode(node == null ? cls : node, lineNum, line));
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to index class: {}", cls, e);
		}
	}

	public Iterable<JNode> searchClsName(String text) {
		return clsNamesTree.getValuesForKeysContaining(text);
	}

	public Iterable<JNode> searchMthName(String text) {
		return mthNamesTree.getValuesForKeysContaining(text);
	}

	public Iterable<JNode> searchFldName(String text) {
		return fldNamesTree.getValuesForKeysContaining(text);
	}

	public Iterable<CodeNode> searchCode(String text) {
		Iterable<CodeNode> items;
		if (codeTree.size() > 0) {
			items = codeTree.getValuesForKeysContaining(text);
			if (skippedClasses.isEmpty()) {
				return items;
			}
		} else {
			items = null;
		}
		List<CodeNode> list = new ArrayList<CodeNode>();
		if (items != null) {
			for (CodeNode item : items) {
				list.add(item);
			}
		}
		addSkippedClasses(list, text);
		return list;
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
		String line = code.substring(lineStart, lineEnd == -1 ? code.length() : lineEnd);
		list.add(new CodeNode(javaClass, -pos, line.trim()));
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
