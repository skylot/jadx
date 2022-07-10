package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.core.dex.info.FieldInfo;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

public final class FieldSearchProvider extends BaseSearchProvider {

	private int clsNum = 0;
	private int fldNum = 0;

	public FieldSearchProvider(MainWindow mw, SearchSettings searchSettings, List<JavaClass> classes) {
		super(mw, searchSettings, classes);
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		while (true) {
			if (cancelable.isCanceled()) {
				return null;
			}
			JavaClass cls = classes.get(clsNum);
			List<JavaField> fields = cls.getFields();
			if (fldNum < fields.size()) {
				JavaField fld = fields.get(fldNum++);
				if (checkField(fld)) {
					return convert(fld);
				}
			} else {
				clsNum++;
				fldNum = 0;
				if (clsNum >= classes.size()) {
					return null;
				}
			}
		}
	}

	private boolean checkField(JavaField field) {
		FieldInfo fieldInfo = field.getFieldNode().getFieldInfo();
		return isMatch(fieldInfo.getName()) || isMatch(fieldInfo.getAlias());
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
