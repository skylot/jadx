package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.core.dex.info.ClassInfo;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.MatchingPositions;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

public final class ClassSearchProvider extends BaseSearchProvider {

	private int clsNum = 0;

	public ClassSearchProvider(MainWindow mw, SearchSettings searchSettings, List<JavaClass> classes) {
		super(mw, searchSettings, classes);
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		while (true) {
			if (cancelable.isCanceled() || clsNum >= classes.size()) {
				return null;
			}
			JavaClass curCls = classes.get(clsNum++);
			MatchingPositions matchingPositions = checkCls(curCls);
			if (matchingPositions != null) {
				JNode node = convert(curCls);
				MatchingPositions highlightPositions = isMatch(curCls.getFullName());
				node.setHasHighlight(true);
				node.setStart(highlightPositions.getStartMath());
				node.setEnd(highlightPositions.getEndMath());
				return node;
			}
		}
	}

	private MatchingPositions checkCls(JavaClass cls) {
		ClassInfo clsInfo = cls.getClassNode().getClassInfo();
		MatchingPositions shortMatch = isMatch(clsInfo.getShortName());
		MatchingPositions fullMatch = isMatch(clsInfo.getFullName());
		MatchingPositions aliasMatch = isMatch(clsInfo.getAliasFullName());
		MatchingPositions rawMatch = isMatch(clsInfo.getRawName());
		if (shortMatch != null) {
			return shortMatch;
		}
		if (fullMatch != null) {
			return fullMatch;
		}
		if (aliasMatch != null) {
			return aliasMatch;
		}
		return rawMatch;
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
