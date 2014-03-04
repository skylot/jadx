package jadx.core.dex.nodes.parser;

import jadx.core.dex.instructions.args.ArgType;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSignatureParser {

	private SignatureParser p(String str) {
		return new SignatureParser(str);
	}

	@Test
	public void testType() {
		assertEquals(p("").consumeType(), null);
		assertEquals(p("I").consumeType(), ArgType.INT);
		assertEquals(p("[I").consumeType(), ArgType.array(ArgType.INT));
		assertEquals(p("Ljava/lang/Object;").consumeType(), ArgType.OBJECT);
		assertEquals(p("[Ljava/lang/Object;").consumeType(), ArgType.array(ArgType.OBJECT));
		assertEquals(p("[[I").consumeType(), ArgType.array(ArgType.array(ArgType.INT)));
	}

	@Test
	public void testType2() {
		assertEquals(p("TD;").consumeType(), ArgType.genericType("D"));
	}

	@Test
	public void testGenericType() {
		assertEquals(p("La<TV;Lb;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.genericType("V"), ArgType.object("b")}));

		assertEquals(p("La<Lb<Lc;>;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{
						ArgType.generic("Lb;", new ArgType[]{
								ArgType.object("Lc;")})}));
	}

	@Test
	public void testGenericInnerType() {
		assertEquals(p("La<TD;>.c;").consumeType(),
				ArgType.genericInner(ArgType.generic("La;", new ArgType[]{ArgType.genericType("D")}), "c", null));

		assertEquals(p("La<Lb;>.c<TV;>;").consumeType(),
				ArgType.genericInner(ArgType.generic("La;", new ArgType[]{ArgType.object("Lb;")}),
						"c", new ArgType[]{ArgType.genericType("V")}));

		assertEquals(p("La<TV;>.LinkedHashIterator<Lb$c<Ls;TV;>;>;").consumeType().getObject(),
				"a$LinkedHashIterator");

	}

	@Test
	public void testWildCards() {
		assertEquals(p("La<*>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard()}));

		assertEquals(p("La<+Lb;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard(ArgType.object("b"), 1)}));

		assertEquals(p("La<-Lb;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard(ArgType.object("b"), -1)}));

		assertEquals(p("La<+TV;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard(ArgType.genericType("V"), 1)}));

		assertEquals(p("La<-TV;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard(ArgType.genericType("V"), -1)}));
	}

	@Test
	public void testWildCards2() {
		assertEquals(p("La<*>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard()}));

		assertEquals(p("La<**>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard(), ArgType.wildcard()}));

		assertEquals(p("La<*Lb;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard(), ArgType.object("b")}));

		assertEquals(p("La<*TV;>;").consumeType(),
				ArgType.generic("La;", new ArgType[]{ArgType.wildcard(), ArgType.genericType("V")}));
	}

	@Test
	public void testGenericMap() {
		assertEquals(p("<T:Ljava/lang/Object;>").consumeGenericMap().toString(),
				"{T=[]}");

		assertEquals(p("<K:Ljava/lang/Object;LongGenericType:Ljava/lang/Object;>").consumeGenericMap().toString(),
				"{K=[], LongGenericType=[]}");

		assertEquals(p("<ResultT:Ljava/lang/Exception;:Ljava/lang/Object;>").consumeGenericMap().toString(),
				"{ResultT=[java.lang.Exception]}");
	}

	@Test
	public void testMethodsArgs() {
		List<ArgType> argTypes = p("(Ljava/util/List<*>;)V").consumeMethodArgs();
		assertEquals(argTypes.size(), 1);
		assertEquals(argTypes.get(0), ArgType.generic("Ljava/util/List;", new ArgType[]{ArgType.wildcard()}));
	}
}
