package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.core.dex.info.ClassInfo;
import jadx.gui.jobs.Cancelable;
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
			if (checkCls(curCls)) {
				return convert(curCls);
			}
		}
	}

	private boolean checkCls(JavaClass cls) {
		ClassInfo clsInfo = cls.getClassNode().getClassInfo();
		return isMatch(clsInfo.getShortName())
				|| isMatch(clsInfo.getFullName())
				|| isMatch(clsInfo.getAliasFullName());
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
