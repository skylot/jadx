/*
 * Generated on 11/22/21, 8:58 PM
 */
package jadx.gui.ui.codeearea;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * 用于Smali代码高亮
 * MartinKay@qq.com
 */
%%

%public
%class SmaliTokenMaker
%extends AbstractJFlexCTokenMaker
%unicode
/* Case sensitive */
%type org.fife.ui.rsyntaxtextarea.Token


%{


	/**
	 * Constructor.  This must be here because JFlex does not generate a
	 * no-parameter constructor.
	 */
	public SmaliTokenMaker() {
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addToken(int, int, int)
	 */
	private void addHyperlinkToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, true);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 */
	private void addToken(int tokenType) {
		addToken(zzStartRead, zzMarkedPos-1, tokenType);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addHyperlinkToken(int, int, int)
	 */
	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, false);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array.
	 * @param start The starting offset in the array.
	 * @param end The ending offset in the array.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *        occurs.
	 * @param hyperlink Whether this token is a hyperlink.
	 */
	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset, boolean hyperlink) {
		super.addToken(array, start,end, tokenType, startOffset, hyperlink);
		zzStartRead = zzMarkedPos;
	}


	/**
	 * {@inheritDoc}
	 */
	public String[] getLineCommentStartAndEnd(int languageIndex) {
		return new String[] { "#", null };
	}


	/**
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = Token.NULL;
		switch (initialTokenType) {
			/* No multi-line comments */
			/* No documentation comments */
			default:
				state = Token.NULL;
		}

		s = text;
		try {
			yyreset(zzReader);
			yybegin(state);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new TokenImpl();
		}

	}


	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>true</code> if EOF was reached, otherwise
	 *              <code>false</code>.
	 */
	private boolean zzRefill() {
		return zzCurrentPos>=s.offset+s.count;
	}


	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>YY_INITIAL</tt>.
	 *
	 * @param reader   the new input stream
	 */
	public final void yyreset(Reader reader) {
		// 's' has been updated.
		zzBuffer = s.array;
		/*
		 * We replaced the line below with the two below it because zzRefill
		 * no longer "refills" the buffer (since the way we do it, it's always
		 * "full" the first time through, since it points to the segment's
		 * array).  So, we assign zzEndRead here.
		 */
		//zzStartRead = zzEndRead = s.offset;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}


%}

Letter							= [A-Za-z]
LetterOrUnderscore				= ({Letter}|"_")
NonzeroDigit						= [1-9]
Digit							= ("0"|{NonzeroDigit})
HexDigit							= ({Digit}|[A-Fa-f])
OctalDigit						= ([0-7])
AnyCharacterButApostropheOrBackSlash	= ([^\\'])
AnyCharacterButDoubleQuoteOrBackSlash	= ([^\\\"\n])
EscapedSourceCharacter				= ("u"{HexDigit}{HexDigit}{HexDigit}{HexDigit})
Escape							= ("\\"(([btnfr\"'\\])|([0123]{OctalDigit}?{OctalDigit}?)|({OctalDigit}{OctalDigit}?)|{EscapedSourceCharacter}))
NonSeparator						= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\']|"#"|"\\")
IdentifierStart					= ({LetterOrUnderscore}|"$")
IdentifierPart						= ({IdentifierStart}|{Digit}|("\\"{EscapedSourceCharacter}))

LineTerminator				= (\n)
WhiteSpace				= ([ \t\f]+)

CharLiteral	= ([\']({AnyCharacterButApostropheOrBackSlash}|{Escape})[\'])
UnclosedCharLiteral			= ([\'][^\'\n]*)
ErrorCharLiteral			= ({UnclosedCharLiteral}[\'])
StringLiteral				= ([\"]({AnyCharacterButDoubleQuoteOrBackSlash}|{Escape})*[\"])
UnclosedStringLiteral		= ([\"]([\\].|[^\\\"])*[^\"]?)
ErrorStringLiteral			= ({UnclosedStringLiteral}[\"])

/* No multi-line comments */
/* No documentation comments */
LineCommentBegin			= "#"

IntegerLiteral			= ({Digit}+)
HexLiteral			= (0x{HexDigit}+)
FloatLiteral			= (({Digit}+)("."{Digit}+)?(e[+-]?{Digit}+)? | ({Digit}+)?("."{Digit}+)(e[+-]?{Digit}+)?)
ErrorNumberFormat			= (({IntegerLiteral}|{HexLiteral}|{FloatLiteral}){NonSeparator}+)
BooleanLiteral				= ("true"|"false")

Separator					= ([\(\)\{\}\[\]])
Separator2				= ([\;,.])

Identifier				= ({IdentifierStart}{IdentifierPart}*)

URLGenDelim				= ([:\/\?#\[\]@])
URLSubDelim				= ([\!\$&'\(\)\*\+,;=])
URLUnreserved			= ({LetterOrUnderscore}|{Digit}|[\-\.\~])
URLCharacter			= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters			= ({URLCharacter}*)
URLEndCharacter			= ([\/\$]|{Letter}|{Digit})
URL						= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


/*  Custom Regex  */
/* fully-qualified name Rules  */
SimpleName = ([a-zA-Z0-9_$]*)
QUALIFIED_TYPE_NAME = ("L"({SimpleName}{SLASH})*{SimpleName}*";")
/* Types */
VOID_TYPE = ("V")
BOOLEAN_TYPE = ("Z")
BYTE_TYPE = ("B")
SHORT_TYPE = ("S")
CHAR_TYPE = ("C")
INT_TYPE = ("I")
LONG_TYPE = ("J")
FLOAT_TYPE = ("F")
DOUBLE_TYPE = ("D")
/* Multi Args Types Highlight  */
MULTI_ARGS_TYPES = (({BOOLEAN_TYPE}|{BYTE_TYPE}|{SHORT_TYPE}|{CHAR_TYPE}|{INT_TYPE}|{LONG_TYPE}|{FLOAT_TYPE}|{DOUBLE_TYPE})+);

/* Types fully-qualified name */
COMPOUND_METHOD_ARG_LITERAL = (({BOOLEAN_TYPE}|{BYTE_TYPE}|{SHORT_TYPE}|{CHAR_TYPE}|{INT_TYPE}|{LONG_TYPE}|{FLOAT_TYPE}|{DOUBLE_TYPE})+{QUALIFIED_TYPE_NAME})


LBRACK = ("[")
RBRACK = ("]")
LPAREN = ("(")
RPAREN = (")")
LBRACE = ("{")
RBRACE = ("}")
COLON = (":")
ASSIGN = ("=")
DOT = (".")
SUB = ("-")
COMMA = (",")
SLASH = ("/")
LT = ("<")
GT = (">")
ARROW = ("->")
SEMI = (";")
ARROW_FUNCTION = ({ARROW}[a-zA-Z_$<>]*)
CustomSeparator = ({Separator}|{ARROW_FUNCTION})

/* Register */
VREGISTER = ("v"("0"|[1-9])*)
PREGISTER = ("p"("0"|[1-9])*)

/* Flags  */
FLAG_PSWITCH = (":pswitch_"{SimpleName})
FLAG_PSWITCH_DATA = (":pswitch_data_"{SimpleName})
FLAG_GOTO = (":goto_"{SimpleName})
FLAG_COND = (":cond_"{SimpleName})
FLAG_TRY_START = (":try_start_"{SimpleName})
FLAG_TRY_END = (":try_end_"{SimpleName})
FLAG_CATCH = (":catch_"{SimpleName})
FLAG_CATCHALL = (":catchall_"{SimpleName})
FLAG_ARRAY = (":array_"{SimpleName})

/* No string state */
/* No char state */
/* No MLC state */
/* No documentation comment state */
%state EOL_COMMENT

%%

<YYINITIAL> {

	/* Keywords Instructions Highlight */
"nop" |
"move" |
"move/from16" |
"move/16" |
"move-wide" |
"move-wide/from16" |
"move-wide/16" |
"move-object" |
"move-object/from16" |
"move-object/16" |
"move-result" |
"move-result-wide" |
"move-result-object" |
"move-exception" |
"return-void" |
"return" |
"return-wide" |
"return-object" |
"const/4" |
"const/16" |
"const" |
"const/high16" |
"const-wide/16" |
"const-wide/32" |
"const-wide" |
"const-wide/high16" |
"const-string" |
"const-string/jumbo" |
"const-class" |
"monitor-enter" |
"monitor-exit" |
"check-cast" |
"instance-of" |
"array-length" |
"new-instance" |
"new-array" |
"filled-new-array" |
"filled-new-array/range" |
"fill-array-data" |
"throw" |
"goto" |
"goto/16" |
"goto/32" |
"cmpl-float" |
"cmpg-float" |
"cmpl-double" |
"cmpg-double" |
"cmp-long" |
"if-eq" |
"if-ne" |
"if-lt" |
"if-ge" |
"if-gt" |
"if-le" |
"if-eqz" |
"if-nez" |
"if-ltz" |
"if-gez" |
"if-gtz" |
"if-lez" |
"aget" |
"aget-wide" |
"aget-object" |
"aget-boolean" |
"aget-byte" |
"aget-char" |
"aget-short" |
"aput" |
"aput-wide" |
"aput-object" |
"aput-boolean" |
"aput-byte" |
"aput-char" |
"aput-short" |
"iget" |
"iget-wide" |
"iget-object" |
"iget-boolean" |
"iget-byte" |
"iget-char" |
"iget-short" |
"iput" |
"iput-wide" |
"iput-object" |
"iput-boolean" |
"iput-byte" |
"iput-char" |
"iput-short" |
"sget" |
"sget-wide" |
"sget-object" |
"sget-boolean" |
"sget-byte" |
"sget-char" |
"sget-short" |
"sput" |
"sput-wide" |
"sput-object" |
"sput-boolean" |
"sput-byte" |
"sput-char" |
"sput-short" |
"invoke-virtual" |
"invoke-super" |
"invoke-direct" |
"invoke-static" |
"invoke-interface" |
"invoke-virtual/range" |
"invoke-super/range" |
"invoke-direct/range" |
"invoke-static/range" |
"invoke-interface/range" |
"neg-int" |
"not-int" |
"neg-long" |
"not-long" |
"neg-float" |
"neg-double" |
"int-to-long" |
"int-to-float" |
"int-to-double" |
"long-to-int" |
"long-to-float" |
"long-to-double" |
"float-to-int" |
"float-to-long" |
"float-to-double" |
"double-to-int" |
"double-to-long" |
"double-to-float" |
"int-to-byte" |
"int-to-char" |
"int-to-short" |
"add-int" |
"sub-int" |
"mul-int" |
"div-int" |
"rem-int" |
"and-int" |
"or-int" |
"xor-int" |
"shl-int" |
"shr-int" |
"ushr-int" |
"add-long" |
"sub-long" |
"mul-long" |
"div-long" |
"rem-long" |
"and-long" |
"or-long" |
"xor-long" |
"shl-long" |
"shr-long" |
"ushr-long" |
"add-float" |
"sub-float" |
"mul-float" |
"div-float" |
"rem-float" |
"add-double" |
"sub-double" |
"mul-double" |
"div-double" |
"rem-double" |
"add-int/2addr" |
"sub-int/2addr" |
"mul-int/2addr" |
"div-int/2addr" |
"rem-int/2addr" |
"and-int/2addr" |
"or-int/2addr" |
"xor-int/2addr" |
"shl-int/2addr" |
"shr-int/2addr" |
"ushr-int/2addr" |
"add-long/2addr" |
"sub-long/2addr" |
"mul-long/2addr" |
"div-long/2addr" |
"rem-long/2addr" |
"and-long/2addr" |
"or-long/2addr" |
"xor-long/2addr" |
"shl-long/2addr" |
"shr-long/2addr" |
"ushr-long/2addr" |
"add-float/2addr" |
"sub-float/2addr" |
"mul-float/2addr" |
"div-float/2addr" |
"rem-float/2addr" |
"add-double/2addr" |
"sub-double/2addr" |
"mul-double/2addr" |
"div-double/2addr" |
"rem-double/2addr" |
"add-int/lit16" |
"rsub-int" |
"mul-int/lit16" |
"div-int/lit16" |
"rem-int/lit16" |
"and-int/lit16" |
"or-int/lit16" |
"xor-int/lit16" |
"add-int/lit8" |
"rsub-int/lit8" |
"mul-int/lit8" |
"div-int/lit8" |
"rem-int/lit8" |
"and-int/lit8" |
"or-int/lit8" |
"xor-int/lit8" |
"shl-int/lit8" |
"shr-int/lit8" |
"ushr-int/lit8" |
"invoke-polymorphic" |
"invoke-polymorphic/range" |
"invoke-custom" |
"invoke-custom/range" |
"const-method-handle" |
"const-method-type" |
"packed-switch" |
"sparse-switch"		{ addToken(Token.FUNCTION); }

	/* Keywords Modifiers(IDENTIFIER标识符、修饰符) Highlight */
"public" |
"private" |
"protected" |
"final" |
"annotation" |
"static" |
"synthetic" |
"constructor" |
"abstract" |
"enum" |
"interface" |
"transient" |
"bridge" |
"declared-synchronized" |
"volatile" |
"strictfp" |
"varargs" |
"native"		{ addToken(Token.RESERVED_WORD); }




	/* Keywords Directives Highlight */
".method" |
".end method" |
".implements" |
".class" |
".prologue" |
".source" |
".super" |
".field" |
".end field" |
".registers" |
".locals" |
".param" |
".line" |
".catch" |
".catchall" |
".annotation" |
".end annotation" |
".local" |
".end local" |
".restart local" |
".packed-switch" |
".end packed-switch" |
".array-data" |
".end array-data" |
".sparse-switch" |
".end sparse-switch" |
".end param"		{ addToken(Token.RESERVED_WORD_2); }


	/* VARIABLE Register Highlight */
{VREGISTER} |
{PREGISTER}		{ addToken(Token.VARIABLE); }



	/* Data types Highlight */
{QUALIFIED_TYPE_NAME} |
{COMPOUND_METHOD_ARG_LITERAL} |
{MULTI_ARGS_TYPES} |
{QUALIFIED_TYPE_NAME} |
{VOID_TYPE} |
{BOOLEAN_TYPE} |
{BYTE_TYPE} |
{SHORT_TYPE} |
{CHAR_TYPE} |
{INT_TYPE} |
{LONG_TYPE} |
{FLOAT_TYPE} |
{DOUBLE_TYPE} 	{ addToken(Token.DATA_TYPE); }

 /* FLAGS */
{FLAG_PSWITCH} |
{FLAG_PSWITCH_DATA} |
{FLAG_GOTO} |
{FLAG_COND} |
{FLAG_TRY_START} |
{FLAG_TRY_END} |
{FLAG_CATCHALL} |
{FLAG_ARRAY} |
{FLAG_CATCH}  	{ addToken(Token.MARKUP_TAG_NAME); }

	/* Functions */
	/* No functions */

	{BooleanLiteral}			{ addToken(Token.LITERAL_BOOLEAN); }

	{LineTerminator}				{ addNullToken(); return firstToken; }

	{Identifier}					{ addToken(Token.IDENTIFIER); }

	{WhiteSpace}					{ addToken(Token.WHITESPACE); }

	/* String/Character literals. */
	{CharLiteral}				{ addToken(Token.LITERAL_CHAR); }
{UnclosedCharLiteral}		{ addToken(Token.ERROR_CHAR); addNullToken(); return firstToken; }
{ErrorCharLiteral}			{ addToken(Token.ERROR_CHAR); }
	{StringLiteral}				{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
{UnclosedStringLiteral}		{ addToken(Token.ERROR_STRING_DOUBLE); addNullToken(); return firstToken; }
{ErrorStringLiteral}			{ addToken(Token.ERROR_STRING_DOUBLE); }

	/* Comment literals. */
	/* No multi-line comments */
	/* No documentation comments */
	{LineCommentBegin}			{ start = zzMarkedPos-1; yybegin(EOL_COMMENT); }

	/* Separators. */
	{CustomSeparator}					{ addToken(Token.SEPARATOR); }
	{Separator2}					{ addToken(Token.IDENTIFIER); }

	/* Operators. */
"!" |
";" |
"." |
"=" |
"/" |
"'" |
"(" |
")" |
"," |
"->" |
";->" |
"<" |
">" |
"@" |
"[" |
"]" |
"{" |
"}"		{ addToken(Token.OPERATOR); }

	/* Numbers */
	{IntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{HexLiteral}					{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{FloatLiteral}					{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{ErrorNumberFormat}			{ addToken(Token.ERROR_NUMBER_FORMAT); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters. */
	.							{ addToken(Token.IDENTIFIER); }

}


/* No char state */

/* No string state */

/* No multi-line comment state */

/* No documentation comment state */

<EOL_COMMENT> {
	[^hwf\n]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_EOL); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_EOL); start = zzMarkedPos; }
	[hwf]					{}
	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
}

