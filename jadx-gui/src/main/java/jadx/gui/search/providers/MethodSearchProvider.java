package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.core.dex.info.MethodInfo;
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
			List<JavaMethod> methods = cls.getMethods();
			if (mthNum < methods.size()) {
				JavaMethod mth = methods.get(mthNum++);
				if (checkMth(mth)) {
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

	private boolean checkMth(JavaMethod mth) {
		MethodInfo mthInfo = mth.getMethodNode().getMethodInfo();
		return isMatch(mthInfo.getShortId())
				|| isMatch(mthInfo.getAlias());
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
