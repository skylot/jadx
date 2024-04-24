package jadx.plugins.input.smali;

import java.io.IOException;
import java.io.Reader;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.smali.LexerErrorInterface;
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
	public static byte[] assemble(Reader reader, SmaliOptions options) throws IOException, RecognitionException {
		LexerErrorInterface lexer = new smaliFlexLexer(reader, options.apiLevel);
		CommonTokenStream tokens = new CommonTokenStream((TokenSource) lexer);
		smaliParser parser = new smaliParser(tokens);
		parser.setVerboseErrors(options.verboseErrors);
		parser.setAllowOdex(options.allowOdexOpcodes);
		parser.setApiLevel(options.apiLevel);
		smaliParser.smali_file_return parseResult = parser.smali_file();
		if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
			throw new RuntimeException("Parse error");
		}
		CommonTreeNodeStream treeStream = new CommonTreeNodeStream(parseResult.getTree());
		treeStream.setTokenStream(tokens);

		DexBuilder dexBuilder = new DexBuilder(Opcodes.forApi(options.apiLevel));
		smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
		dexGen.setApiLevel(options.apiLevel);
		dexGen.setVerboseErrors(options.verboseErrors);
		dexGen.setDexBuilder(dexBuilder);
		dexGen.smali_file();
		if (dexGen.getNumberOfSyntaxErrors() > 0) {
			throw new RuntimeException("Compile error");
		}
		MemoryDataStore dataStore = new MemoryDataStore();
		dexBuilder.writeTo(dataStore);
		return dataStore.getData();
	}
}
