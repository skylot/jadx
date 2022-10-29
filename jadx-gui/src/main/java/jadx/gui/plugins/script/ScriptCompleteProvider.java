package jadx.gui.plugins.script;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProviderBase;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kotlin.script.experimental.api.ScriptDiagnostic;
import kotlin.script.experimental.api.SourceCode;
import kotlin.script.experimental.api.SourceCodeCompletionVariant;

import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.utils.Icons;
import jadx.plugins.script.ide.ScriptCompiler;
import jadx.plugins.script.ide.ScriptCompletionResult;

import static jadx.plugins.script.ide.ScriptCompilerKt.AUTO_COMPLETE_INSERT_STR;

public class ScriptCompleteProvider extends CompletionProviderBase {
	private static final Logger LOG = LoggerFactory.getLogger(ScriptCompleteProvider.class);

	private static final Map<String, Icon> ICONS_MAP = buildIconsMap();

	private static Map<String, Icon> buildIconsMap() {
		Map<String, Icon> map = new HashMap<>();
		map.put("class", Icons.CLASS);
		map.put("method", Icons.METHOD);
		map.put("field", Icons.FIELD);
		map.put("property", Icons.PROPERTY);
		map.put("parameter", Icons.PARAMETER);
		map.put("package", Icons.PACKAGE);
		return map;
	}

	private final AbstractCodeArea codeArea;
	private ScriptCompiler scriptComplete;

	public ScriptCompleteProvider(AbstractCodeArea codeArea) {
		this.codeArea = codeArea;
		// this.scriptComplete = new ScriptCompiler(codeArea.getNode().getName());
	}

	private List<Completion> getCompletions() {
		try {
			String code = codeArea.getText();
			int caretPos = codeArea.getCaretPosition();
			// TODO: resolve error after reusing ScriptCompiler
			scriptComplete = new ScriptCompiler(codeArea.getNode().getName());
			ScriptCompletionResult result = scriptComplete.complete(code, caretPos);
			int replacePos = getReplacePos(caretPos, result);
			if (!result.getReports().isEmpty()) {
				LOG.debug("Script completion reports: {}", result.getReports());
			}
			return convertCompletions(result.getCompletions(), code, replacePos);
		} catch (Exception e) {
			LOG.error("Code completion failed", e);
			return Collections.emptyList();
		}
	}

	private List<Completion> convertCompletions(List<SourceCodeCompletionVariant> completions, String code, int replacePos) {
		if (completions.isEmpty()) {
			return Collections.emptyList();
		}
		if (LOG.isDebugEnabled()) {
			String cmplStr = completions.stream().map(SourceCodeCompletionVariant::toString).collect(Collectors.joining("\n"));
			LOG.debug("Completions:\n{}", cmplStr);
		}
		int count = completions.size();
		List<Completion> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			SourceCodeCompletionVariant c = completions.get(i);
			if (Objects.equals(c.getIcon(), "keyword")) {
				// too many, not very useful
				continue;
			}

			ScriptCompletionData cmpl = new ScriptCompletionData(this, count - i);
			cmpl.setData(c.getText(), code, replacePos);
			if (Objects.equals(c.getIcon(), "method") && !Objects.equals(c.getText(), c.getDisplayText())) {
				// add method args details for methods
				cmpl.setSummary(c.getDisplayText() + " " + c.getTail());
			} else {
				cmpl.setSummary(c.getTail());
			}
			cmpl.setToolTip(c.getDisplayText());
			Icon icon = ICONS_MAP.get(c.getIcon());
			cmpl.setIcon(icon != null ? icon : Icons.FILE);
			list.add(cmpl);
		}
		return list;
	}

	private int getReplacePos(int caretPos, ScriptCompletionResult result) throws BadLocationException {
		int lineRaw = codeArea.getLineOfOffset(caretPos);
		int lineStart = codeArea.getLineStartOffset(lineRaw);
		int line = lineRaw + 1;
		int col = caretPos - lineStart + 1;

		List<ScriptDiagnostic> reports = result.getReports();
		ScriptDiagnostic cmplReport = ListUtils.filterOnlyOne(reports, r -> {
			if (r.getSeverity() == ScriptDiagnostic.Severity.ERROR && r.getLocation() != null) {
				SourceCode.Position start = r.getLocation().getStart();
				return start.getLine() == line && r.getMessage().endsWith(AUTO_COMPLETE_INSERT_STR);
			}
			return false;
		});
		if (cmplReport == null) {
			LOG.warn("Failed to find completion report in: {}", reports);
			return caretPos;
		}
		reports.remove(cmplReport);
		int reportCol = Objects.requireNonNull(cmplReport.getLocation()).getStart().getCol();
		return caretPos - (col - reportCol);
	}

	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {
		try {
			int pos = codeArea.getCaretPosition();
			return codeArea.getText(0, pos);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to get text before caret", e);
		}
	}

	@Override
	public List<Completion> getCompletionsAt(JTextComponent comp, Point p) {
		return getCompletions();
	}

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {
		return getCompletions();
	}

	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent tc) {
		return null;
	}
}
