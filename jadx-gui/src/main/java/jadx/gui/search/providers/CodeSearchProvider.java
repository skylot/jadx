package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.ICodeWriter;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

import static jadx.core.utils.Utils.getOrElse;

public final class CodeSearchProvider extends BaseSearchProvider {
	private static final Logger LOG = LoggerFactory.getLogger(CodeSearchProvider.class);

	private final ICodeCache codeCache;
	private final JadxWrapper wrapper;

	private @Nullable String code;
	private int clsNum = 0;
	private int pos = 0;

	public CodeSearchProvider(MainWindow mw, SearchSettings searchSettings, List<JavaClass> classes) {
		super(mw, searchSettings, classes);
		this.codeCache = mw.getWrapper().getArgs().getCodeCache();
		this.wrapper = mw.getWrapper();
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		while (true) {
			if (cancelable.isCanceled() || clsNum >= classes.size()) {
				return null;
			}
			JavaClass cls = classes.get(clsNum);
			if (!cls.getClassNode().isInner()) {
				if (code == null) {
					code = getClassCode(cls, codeCache);
				}
				JNode newResult = searchNext(cls, code);
				if (newResult != null) {
					return newResult;
				}
			}
			clsNum++;
			pos = 0;
			code = null;
		}
	}

	@Nullable
	private JNode searchNext(JavaClass javaClass, String clsCode) {
		int newPos = searchMth.find(clsCode, searchStr, pos);
		if (newPos == -1) {
			return null;
		}
		int lineStart = 1 + clsCode.lastIndexOf(ICodeWriter.NL, newPos);
		int lineEnd = clsCode.indexOf(ICodeWriter.NL, newPos);
		int end = lineEnd == -1 ? clsCode.length() : lineEnd;
		String line = clsCode.substring(lineStart, end);
		this.pos = end;
		JClass rootCls = convert(javaClass);
		JNode enclosingNode = getOrElse(getEnclosingNode(javaClass, end), rootCls);
		return new CodeNode(rootCls, enclosingNode, line.trim(), newPos);
	}

	private @Nullable JNode getEnclosingNode(JavaClass javaCls, int pos) {
		try {
			ICodeMetadata metadata = javaCls.getCodeInfo().getCodeMetadata();
			ICodeNodeRef nodeRef = metadata.getNodeAt(pos);
			JavaNode encNode = wrapper.getJavaNodeByRef(nodeRef);
			if (encNode != null) {
				return convert(encNode);
			}
		} catch (Exception e) {
			LOG.debug("Failed to resolve enclosing node", e);
		}
		return null;
	}

	private String getClassCode(JavaClass javaClass, ICodeCache codeCache) {
		try {
			// quick check for if code already in cache
			String code = codeCache.getCode(javaClass.getRawName());
			if (code != null) {
				return code;
			}
			return javaClass.getCode();
		} catch (Exception e) {
			LOG.warn("Failed to get class code: " + javaClass, e);
			return "";
		}
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
