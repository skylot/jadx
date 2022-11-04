package jadx.gui.plugins.script

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.ec4j.core.model.PropertyType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KtLintUtils {

	val LOG: Logger = LoggerFactory.getLogger(KtLintUtils::class.java)

	val rules = lazy { StandardRuleSetProvider().getRuleProviders() }

	val configOverride = lazy {
		EditorConfigOverride.from(
			indentStyleProperty to PropertyType.IndentStyleValue.tab
		)
	}

	fun format(code: String, fileName: String): String {
		val params = KtLint.ExperimentalParams(
			text = code,
			fileName = fileName,
			ruleProviders = rules.value,
			editorConfigOverride = configOverride.value,
			script = true,
			cb = { e: LintError, corrected ->
				if (!corrected) {
					LOG.warn("Lint error: {}", e)
				}
			}
		)
		return KtLint.format(params)
	}

	fun lint(code: String, fileName: String): List<LintError> {
		val errors = mutableListOf<LintError>()
		val params = KtLint.ExperimentalParams(
			text = code,
			fileName = fileName,
			ruleProviders = rules.value,
			editorConfigOverride = configOverride.value,
			script = true,
			cb = { e: LintError, corrected ->
				if (!corrected) {
					errors.add(e)
				}
			}
		)
		KtLint.lint(params)
		return errors
	}
}
