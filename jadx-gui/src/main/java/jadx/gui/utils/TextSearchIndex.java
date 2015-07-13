package jadx.gui.utils;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;

import java.io.BufferedReader;
import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;
import com.googlecode.concurrenttrees.suffix.SuffixTree;

public class TextSearchIndex {

	private static final Logger LOG = LoggerFactory.getLogger(TextSearchIndex.class);

	private SuffixTree<JNode> clsNamesTree;
	private SuffixTree<JNode> mthNamesTree;
	private SuffixTree<JNode> fldNamesTree;
	private SuffixTree<CodeNode> codeTree;

	public TextSearchIndex() {
		clsNamesTree = new ConcurrentSuffixTree<JNode>(new DefaultCharArrayNodeFactory());
		mthNamesTree = new ConcurrentSuffixTree<JNode>(new DefaultCharArrayNodeFactory());
		fldNamesTree = new ConcurrentSuffixTree<JNode>(new DefaultCharArrayNodeFactory());
		codeTree = new ConcurrentSuffixTree<CodeNode>(new DefaultCharArrayNodeFactory());
	}

	public void indexNames(JavaClass cls) {
		cls.decompile();
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

	public void indexCode(JavaClass cls) {
		try {
			String code = cls.getCode();
			BufferedReader bufReader = new BufferedReader(new StringReader(code));
			String line;
			int lineNum = 0;
			while ((line = bufReader.readLine()) != null) {
				lineNum++;
				line = line.trim();
				if (!line.isEmpty()) {
					CodeNode node = new CodeNode(cls, lineNum, line);
					codeTree.put(line, node);
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
		return codeTree.getValuesForKeysContaining(text);
	}
}
