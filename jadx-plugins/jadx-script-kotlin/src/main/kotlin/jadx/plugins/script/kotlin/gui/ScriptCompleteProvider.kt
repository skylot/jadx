package jadx.plugins.script.kotlin.gui

import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.api.plugins.JadxPluginContext
import jadx.core.utils.exceptions.JadxRuntimeException
import jadx.gui.ui.codearea.AbstractCodeArea
import jadx.gui.utils.Icons
import jadx.plugins.script.kotlin.ScriptCompletionResult
import jadx.plugins.script.kotlin.ScriptServices
import jadx.plugins.script.kotlin.ScriptServices.Companion.AUTO_COMPLETE_INSERT_STR
import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.CompletionProviderBase
import org.fife.ui.autocomplete.ParameterizedCompletion
import java.awt.Point
import javax.swing.Icon
import javax.swing.text.BadLocationException
import javax.swing.text.JTextComponent
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCodeCompletionVariant

private val log = KotlinLogging.logger {}

private val ICONS_MAP = mapOf<String, Icon>(
	"class" to Icons.CLASS,
	"method" to Icons.METHOD,
	"field" to Icons.FIELD,
	"property" to Icons.PROPERTY,
	"parameter" to Icons.PARAMETER,
	"package" to Icons.PACKAGE,
)

class ScriptCompleteProvider(
	private val codeArea: AbstractCodeArea,
	private val pluginContext: JadxPluginContext,
) : CompletionProviderBase() {

	private val completions: List<Completion>
		get() {
			try {
				val code = codeArea.getText()
				val caretPos = codeArea.caretPosition
				val scriptServices = ScriptServices(pluginContext)
				val scriptName = codeArea.getNode().getName()
				val result = scriptServices.complete(scriptName, code, caretPos)
				if (result.completions.isEmpty()) {
					return listOf()
				}
				val replacePos = getReplacePos(caretPos, result)
				if (!result.reports.isEmpty()) {
					log.debug { "Script completion reports: ${result.reports}" }
				}
				log.debug { "Completions:\n${result.completions.joinToString(separator = "\n")}" }
				return convertCompletions(result.completions, code, replacePos)
			} catch (e: Exception) {
				log.error(e) { "Code completion failed" }
				return listOf()
			}
		}

	private fun convertCompletions(
		completions: List<SourceCodeCompletionVariant>,
		code: String,
		replacePos: Int,
	): List<Completion> {
		val count = completions.size
		val list = ArrayList<Completion>(count)
		for (i in 0..<count) {
			val c = completions[i]
			if (c.icon == "keyword") {
				// too many, not very useful
				continue
			}
			val summary = if (c.icon == "method" && c.text != c.displayText) {
				// add method args details for methods
				"${c.displayText} ${c.tail}"
			} else {
				c.tail
			}
			list += ScriptCompletionData(
				provider = this,
				input = c.text,
				code = code,
				relevance = count - i,
				replacePos = replacePos,
				summary = summary,
				toolTip = c.displayText,
				icon = ICONS_MAP[c.icon] ?: Icons.FILE,
			)
		}
		return list
	}

	@Throws(BadLocationException::class)
	private fun getReplacePos(caretPos: Int, result: ScriptCompletionResult): Int {
		val lineRaw = codeArea.getLineOfOffset(caretPos)
		val lineStart = codeArea.getLineStartOffset(lineRaw)
		val line = lineRaw + 1

		val completeReport = result.reports.find { report ->
			if (report.severity == ScriptDiagnostic.Severity.ERROR) {
				report.location?.let { location ->
					location.start.line == line && report.message.endsWith(AUTO_COMPLETE_INSERT_STR)
				} ?: false
			} else {
				false
			}
		}
		if (completeReport == null) {
			log.warn { "Failed to find completion report in: ${result.reports}" }
			return caretPos
		}
		result.reports.remove(completeReport)
		val col = caretPos - lineStart + 1
		return caretPos - (col - completeReport.location!!.start.col)
	}

	override fun getAlreadyEnteredText(comp: JTextComponent?): String? {
		try {
			val pos = codeArea.caretPosition
			return codeArea.getText(0, pos)
		} catch (e: Exception) {
			throw JadxRuntimeException("Failed to get text before caret", e)
		}
	}

	override fun getCompletionsAt(comp: JTextComponent, p: Point): List<Completion> {
		return this.completions
	}

	override fun getCompletionsImpl(comp: JTextComponent): List<Completion> {
		return this.completions
	}

	override fun getParameterizedCompletions(tc: JTextComponent): List<ParameterizedCompletion>? {
		return null
	}
}
