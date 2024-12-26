package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.MatchingPositions;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

public final class MethodSearchProvider extends BaseSearchProvider {

	private int clsNum = 0;
	private int mthNum = 0;

	public MethodSearchProvider(MainWindow mw, SearchSettings searchSettings, List<JavaClass> classes) {
		super(mw, searchSettings, classes);
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		while (true) {
			if (cancelable.isCanceled()) {
				return null;
			}
			JavaClass cls = classes.get(clsNum);
			List<MethodNode> methods = cls.getClassNode().getMethods();
			if (mthNum < methods.size()) {
				MethodNode mth = methods.get(mthNum++);
				MatchingPositions matchingPositions = checkMth(mth.getMethodInfo());
				if (matchingPositions != null) {
					JNode node = convert(mth);
					MatchingPositions highlightPositions = isMatch(node.makeLongString());
					node.setHasHighlight(true);
					node.setStart(highlightPositions.getStartMath());
					node.setEnd(highlightPositions.getEndMath());
					return node;
				}
			} else {
				clsNum++;
				mthNum = 0;
				if (clsNum >= classes.size()) {
					return null;
				}
			}
		}
	}

	private MatchingPositions checkMth(MethodInfo mthInfo) {
		MatchingPositions shortIdMatch = isMatch(mthInfo.getShortId());
		MatchingPositions aliasMatch = isMatch(mthInfo.getAlias());
		MatchingPositions fullIdMatch = isMatch(mthInfo.getFullId());
		MatchingPositions aliasFullMatch = isMatch(mthInfo.getAliasFullName());
		if (shortIdMatch != null) {
			return shortIdMatch;
		}
		if (aliasMatch != null) {
			return aliasMatch;
		}
		if (fullIdMatch != null) {
			return fullIdMatch;
		}
		return aliasFullMatch;
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
