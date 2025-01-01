package jadx.gui.search.providers;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.nodes.FieldNode;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.MatchingPositions;
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
			List<FieldNode> fields = cls.getClassNode().getFields();
			if (fldNum < fields.size()) {
				FieldNode fld = fields.get(fldNum++);
				MatchingPositions matchingPositions = checkField(fld.getFieldInfo());
				if (matchingPositions != null) {
					JNode node = convert(fld);
					MatchingPositions highlightPositions = isMatch(node.makeLongString());
					node.setHasHighlight(true);
					node.setStart(highlightPositions.getStartMath());
					node.setEnd(highlightPositions.getEndMath());
					return node;
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

	private MatchingPositions checkField(FieldInfo fieldInfo) {
		MatchingPositions nameMatch = isMatch(fieldInfo.getName());
		MatchingPositions aliasMatch = isMatch(fieldInfo.getAlias());
		MatchingPositions fullIdMatch = isMatch(fieldInfo.getFullId());
		if (nameMatch != null) {
			return nameMatch;
		}
		if (aliasMatch != null) {
			return aliasMatch;
		}
		return fullIdMatch;
	}

	@Override
	public int progress() {
		return clsNum;
	}
}
