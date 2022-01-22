package jadx.tests.functional;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.ArgType.WildcardBound;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.instructions.args.ArgType.INT;
import static jadx.core.dex.instructions.args.ArgType.OBJECT;
import static jadx.core.dex.instructions.args.ArgType.array;
import static jadx.core.dex.instructions.args.ArgType.generic;
import static jadx.core.dex.instructions.args.ArgType.genericType;
import static jadx.core.dex.instructions.args.ArgType.object;
import static jadx.core.dex.instructions.args.ArgType.outerGeneric;
import static jadx.core.dex.instructions.args.ArgType.wildcard;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class SignatureParserTest {

	@Test
	public void testSimpleTypes() {
		checkType("", null);
		checkType("I", INT);
		checkType("[I", array(INT));
		checkType("Ljava/lang/Object;", OBJECT);
		checkType("[Ljava/lang/Object;", array(OBJECT));
		checkType("[[I", array(array(INT)));
	}

	private static void checkType(String str, ArgType type) {
		assertThat(new SignatureParser(str).consumeType(), is(type));
	}

	@Test
	public void testGenerics() {
		checkType("TD;", genericType("D"));
		checkType("La<TV;Lb;>;", generic("La;", genericType("V"), object("b")));
		checkType("La<Lb<Lc;>;>;", generic("La;", generic("Lb;", object("Lc;"))));
		checkType("La/b/C<Ld/E<Lf/G;>;>;", generic("La/b/C;", generic("Ld/E;", object("Lf/G;"))));
		checkType("La<TD;>.c;", outerGeneric(generic("La;", genericType("D")), ArgType.object("c")));
		checkType("La<TD;>.c/d;", outerGeneric(generic("La;", genericType("D")), ArgType.object("c.d")));
		checkType("La<Lb;>.c<TV;>;", outerGeneric(generic("La;", object("Lb;")), ArgType.generic("c", genericType("V"))));
	}

	@Test
	public void testInnerGeneric() {
		String signature = "La<TV;>.LinkedHashIterator<Lb$c<Ls;TV;>;>;";
		String objectStr = new SignatureParser(signature).consumeType().getObject();
		assertThat(objectStr, is("a$LinkedHashIterator"));
	}

	@Test
	public void testNestedInnerGeneric() {
		String signature = "La<TV;>.I.X;";
		ArgType result = new SignatureParser(signature).consumeType();
		assertThat(result.getObject(), is("a$I$X"));
		// nested 'outerGeneric' objects
		ArgType obj = generic("La;", genericType("V"));
		assertThat(result, equalTo(outerGeneric(outerGeneric(obj, object("I")), object("X"))));
	}

	@Test
	public void testNestedInnerGeneric2() {
		// full name in inner class
		String signature = "Lsome/long/pkg/ba<Lsome/pkg/s;>.some/long/pkg/bb<Lsome/pkg/p;Lsome/pkg/n;>;";
		ArgType result = new SignatureParser(signature).consumeType();
		System.out.println(result);
		assertThat(result.getObject(), is("some.long.pkg.ba$some.long.pkg.bb"));
		ArgType baseObj = generic("Lsome/long/pkg/ba;", object("Lsome/pkg/s;"));
		ArgType innerObj = generic("Lsome/long/pkg/bb;", object("Lsome/pkg/p;"), object("Lsome/pkg/n;"));
		ArgType obj = outerGeneric(baseObj, innerObj);
		assertThat(result, equalTo(obj));
	}

	@Test
	public void testWildcards() {
		checkWildcards("*", wildcard());
		checkWildcards("+Lb;", wildcard(object("b"), WildcardBound.EXTENDS));
		checkWildcards("-Lb;", wildcard(object("b"), WildcardBound.SUPER));
		checkWildcards("+TV;", wildcard(genericType("V"), WildcardBound.EXTENDS));
		checkWildcards("-TV;", wildcard(genericType("V"), WildcardBound.SUPER));

		checkWildcards("**", wildcard(), wildcard());
		checkWildcards("*Lb;", wildcard(), object("b"));
		checkWildcards("*TV;", wildcard(), genericType("V"));
		checkWildcards("TV;*", genericType("V"), wildcard());
		checkWildcards("Lb;*", object("b"), wildcard());

		checkWildcards("***", wildcard(), wildcard(), wildcard());
		checkWildcards("*Lb;*", wildcard(), object("b"), wildcard());
	}

	private static void checkWildcards(String w, ArgType... types) {
		ArgType parsedType = new SignatureParser("La<" + w + ">;").consumeType();
		ArgType expectedType = generic("La;", types);
		assertThat(parsedType, is(expectedType));
	}

	@Test
	public void testGenericMap() {
		checkGenerics("");
		checkGenerics("<T:Ljava/lang/Object;>", "T", emptyList());
		checkGenerics("<K:Ljava/lang/Object;LongType:Ljava/lang/Object;>", "K", emptyList(), "LongType", emptyList());
		checkGenerics("<ResultT:Ljava/lang/Exception;:Ljava/lang/Object;>", "ResultT", singletonList(object("java.lang.Exception")));
	}

	@SuppressWarnings("unchecked")
	private static void checkGenerics(String g, Object... objs) {
		List<ArgType> genericsList = new SignatureParser(g).consumeGenericTypeParameters();
		List<ArgType> expectedList = new ArrayList<>();
		for (int i = 0; i < objs.length; i += 2) {
			String typeVar = (String) objs[i];
			List<ArgType> list = (List<ArgType>) objs[i + 1];
			expectedList.add(ArgType.genericType(typeVar, list));
		}
		assertThat(genericsList, is(expectedList));
	}

	@Test
	public void testMethodArgs() {
		List<ArgType> argTypes = new SignatureParser("(Ljava/util/List<*>;)V").consumeMethodArgs(1);

		assertThat(argTypes, hasSize(1));
		assertThat(argTypes.get(0), is(generic("Ljava/util/List;", wildcard())));
	}

	@Test
	public void testMethodArgs2() {
		List<ArgType> argTypes = new SignatureParser("(La/b/C<TT;>.d/E;)V").consumeMethodArgs(1);

		assertThat(argTypes, hasSize(1));
		ArgType argType = argTypes.get(0);
		assertThat(argType.getObject().indexOf('/'), is(-1));
		assertThat(argType, is(outerGeneric(generic("La/b/C;", genericType("T")), object("d.E"))));
	}

	@Test
	public void testBadGenericMap() {
		assertThatExceptionOfType(JadxRuntimeException.class)
				.isThrownBy(() -> new SignatureParser("<A:Ljava/lang/Object;B").consumeGenericTypeParameters());
	}

	@Test
	public void testBadArgs() {
		assertThatExceptionOfType(JadxRuntimeException.class)
				.isThrownBy(() -> new SignatureParser("(TCONTENT)Lpkg/Cls;").consumeMethodArgs(1));
	}
}
