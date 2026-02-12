package jadx.plugins.script.kotlin.gui

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice
import org.fife.ui.rsyntaxtextarea.parser.ParseResult
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.script.experimental.api.ScriptDiagnostic

class ScriptErrorService(private val scriptArea: ScriptCodeArea) : AbstractParser() {
	private val result: DefaultParseResult = DefaultParseResult(this)

	override fun parse(doc: RSyntaxDocument?, style: String?): ParseResult {
		return result
	}

	fun clearErrors() {
		result.clearNotices()
		scriptArea.removeParser(this)
	}

	fun apply() {
		scriptArea.removeParser(this)
		scriptArea.addParser(this)
		scriptArea.addNotify()
		scriptArea.requestFocus()
		jumpCaretToFirstError()
	}

	private fun jumpCaretToFirstError() {
		val parserNotices = result.notices
		if (parserNotices.isEmpty()) {
			return
		}
		val notice = parserNotices.get(0)
		var offset = notice.offset
		if (offset == -1) {
			try {
				offset = scriptArea.getLineStartOffset(notice.line)
			} catch (e: Exception) {
				LOG.error("Failed to jump to first error", e)
				return
			}
		}
		scriptArea.scrollToPos(offset)
	}

	fun addCompilerIssues(issues: List<ScriptDiagnostic>) {
		for (issue in issues) {
			if (issue.severity == ScriptDiagnostic.Severity.DEBUG) {
				continue
			}
			val notice: DefaultParserNotice?
			val loc = issue.location
			if (loc == null) {
				notice = DefaultParserNotice(this, issue.message, 0)
			} else {
				try {
					val line = loc.start.line
					val offset = scriptArea.getLineStartOffset(line - 1) + loc.start.col
					val len = if (loc.end == null) -1 else loc.end!!.col - loc.start.col
					notice = DefaultParserNotice(this, issue.message, line, offset - 1, len)
					notice.setLevel(convertLevel(issue.severity))
				} catch (e: Exception) {
					LOG.error("Failed to convert script issue", e)
					continue
				}
			}
			addNotice(notice)
		}
	}

	fun addLintErrors(errors: List<JadxLintError>) {
		for (error in errors) {
			try {
				val line = error.line
				val offset = scriptArea.getLineStartOffset(line - 1) + error.col - 1
				val word = scriptArea.getWordByPosition(offset)
				val len = word?.length ?: -1
				val notice = DefaultParserNotice(this, error.detail, line, offset, len)
				notice.setLevel(ParserNotice.Level.WARNING)
				addNotice(notice)
			} catch (e: Exception) {
				LOG.error("Failed to convert lint error", e)
			}
		}
	}

	private fun addNotice(notice: DefaultParserNotice) {
		LOG.debug("Add notice: {}:{}:{} - {}", notice.line, notice.offset, notice.length, notice.message)
		result.addNotice(notice)
	}

	companion object {
		private val LOG: Logger = LoggerFactory.getLogger(ScriptErrorService::class.java)

		private fun convertLevel(severity: ScriptDiagnostic.Severity): ParserNotice.Level {
			return when (severity) {
				ScriptDiagnostic.Severity.FATAL, ScriptDiagnostic.Severity.ERROR -> ParserNotice.Level.ERROR
				ScriptDiagnostic.Severity.WARNING -> ParserNotice.Level.WARNING
				ScriptDiagnostic.Severity.INFO, ScriptDiagnostic.Severity.DEBUG -> ParserNotice.Level.INFO
			}
		}
	}
}
