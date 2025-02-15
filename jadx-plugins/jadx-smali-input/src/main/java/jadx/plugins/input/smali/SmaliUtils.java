package jadx.plugins.input.smali;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeNodeStream;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.smali.SmaliOptions;
import com.android.tools.smali.smali.smaliFlexLexer;
import com.android.tools.smali.smali.smaliParser;
import com.android.tools.smali.smali.smaliTreeWalker;

/**
 * Utility methods to assemble smali to in-memory buffer.
 * This implementation uses smali library internal classes.
 */
public class SmaliUtils {

	@SuppressWarnings("ExtractMethodRecommender")
	public static byte[] assemble(File smaliFile, SmaliOptions options) throws IOException {
		StringBuilder errors = new StringBuilder();
		try (FileInputStream fis = new FileInputStream(smaliFile);
				InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

			smaliFlexLexer lexer = new smaliFlexLexer(reader, options.apiLevel);
			lexer.setSourceFile(smaliFile);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			ParserWrapper parser = new ParserWrapper(tokens, errors);
			parser.setVerboseErrors(options.verboseErrors);
			parser.setAllowOdex(options.allowOdexOpcodes);
			parser.setApiLevel(options.apiLevel);
			ParserWrapper.smali_file_return parseResult = parser.smali_file();
			if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
				throw new RuntimeException("Smali parse error: " + errors);
			}
			CommonTreeNodeStream treeStream = new CommonTreeNodeStream(parseResult.getTree());
			treeStream.setTokenStream(tokens);

			DexBuilder dexBuilder = new DexBuilder(Opcodes.forApi(options.apiLevel));
			TreeWalkerWrapper dexGen = new TreeWalkerWrapper(treeStream, errors);
			dexGen.setApiLevel(options.apiLevel);
			dexGen.setVerboseErrors(options.verboseErrors);
			dexGen.setDexBuilder(dexBuilder);
			dexGen.smali_file();
			if (dexGen.getNumberOfSyntaxErrors() > 0) {
				throw new RuntimeException("Smali compile error: " + errors);
			}
			MemoryDataStore dataStore = new MemoryDataStore();
			dexBuilder.writeTo(dataStore);
			return dataStore.getData();
		} catch (RecognitionException e) {
			throw new RuntimeException("Smali process error: " + errors, e);
		}
	}

	private static final class ParserWrapper extends smaliParser {
		private final StringBuilder errors;

		public ParserWrapper(TokenStream input, StringBuilder errors) {
			super(input);
			this.errors = errors;
		}

		@Override
		public void emitErrorMessage(String msg) {
			errors.append('\n').append(msg);
		}
	}

	private static final class TreeWalkerWrapper extends smaliTreeWalker {
		private final StringBuilder errors;

		public TreeWalkerWrapper(TreeNodeStream input, StringBuilder errors) {
			super(input);
			this.errors = errors;
		}

		@Override
		public void emitErrorMessage(String msg) {
			errors.append('\n').append(msg);
		}
	}
}
