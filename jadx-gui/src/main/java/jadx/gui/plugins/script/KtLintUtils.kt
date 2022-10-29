package jadx.gui.plugins.script

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KtLintUtils {

	val LOG: Logger = LoggerFactory.getLogger(KtLintUtils::class.java)

	val rules = lazy { StandardRuleSetProvider().getRuleProviders() }

	fun format(code: String, fileName: String): String {
		val params = KtLint.ExperimentalParams(
			text = code,
			fileName = fileName,
			ruleProviders = rules.value,
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
