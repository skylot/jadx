package jadx.core.dex.nodes.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;

import static jadx.core.dex.instructions.args.ArgType.EXCEPTION;
import static jadx.core.dex.instructions.args.ArgType.STRING;
import static jadx.core.dex.instructions.args.ArgType.array;
import static jadx.core.dex.instructions.args.ArgType.generic;
import static jadx.core.dex.instructions.args.ArgType.genericType;
import static jadx.core.dex.instructions.args.ArgType.object;
import static jadx.core.dex.instructions.args.ArgType.outerGeneric;
import static org.assertj.core.api.Assertions.assertThat;

class TypeUtilsTest {
	private TypeUtils typeUtils;

	@BeforeEach
	public void init() {
		typeUtils = new TypeUtils(new RootNode(new JadxArgs()));
	}

	@Test
	void replaceTypeVariablesUsingMap() {
		ArgType typeVar = genericType("T");
		ArgType listCls = object("java.util.List");
		Map<ArgType, ArgType> typeMap = Collections.singletonMap(typeVar, STRING);

		replaceTypeVar(typeVar, typeMap, STRING);
		replaceTypeVar(generic(listCls, typeVar), typeMap, generic(listCls, STRING));
		replaceTypeVar(array(typeVar), typeMap, array(STRING));
	}

	@Test
	void replaceTypeVariablesUsingMap2() {
		ArgType kVar = genericType("K");
		ArgType vVar = genericType("V");
		ArgType mapCls = object("java.util.Map");
		ArgType entryCls = object("Entry");
		ArgType typedMap = generic(mapCls, kVar, vVar);
		ArgType typedEntry = generic(entryCls, kVar, vVar);

		Map<ArgType, ArgType> typeMap = new HashMap<>();
		typeMap.put(kVar, STRING);
		typeMap.put(vVar, EXCEPTION);

		ArgType replacedMap = typeUtils.replaceTypeVariablesUsingMap(typedMap, typeMap);
		ArgType replacedEntry = typeUtils.replaceTypeVariablesUsingMap(typedEntry, typeMap);

		replaceTypeVar(outerGeneric(typedMap, entryCls), typeMap, outerGeneric(replacedMap, entryCls));
		replaceTypeVar(outerGeneric(typedMap, typedEntry), typeMap, outerGeneric(replacedMap, replacedEntry));
	}

	private void replaceTypeVar(ArgType typeVar, Map<ArgType, ArgType> typeMap, ArgType expected) {
		ArgType resultType = typeUtils.replaceTypeVariablesUsingMap(typeVar, typeMap);
		assertThat(resultType)
				.as("Replace %s using map %s", typeVar, typeMap)
				.isEqualTo(expected);
	}
}
