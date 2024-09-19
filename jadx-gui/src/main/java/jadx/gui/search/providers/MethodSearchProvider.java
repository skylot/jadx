package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.jobs.Cancelable;
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
				if (checkMth(mth.getMethodInfo())) {
					return convert(mth);
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

	private boolean checkMth(MethodInfo mthInfo) {
		return isMatch(mthInfo.getShortId())
				|| isMatch(mthInfo.getAlias())
				|| isMatch(mthInfo.getFullId())
				|| isMatch(mthInfo.getAliasFullName());
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
