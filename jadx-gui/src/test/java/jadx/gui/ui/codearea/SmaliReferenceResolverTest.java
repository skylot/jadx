package jadx.gui.ui.codearea;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.ui.codearea.SmaliReferenceResolver.RefKind;
import jadx.gui.ui.codearea.SmaliReferenceResolver.SmaliRef;
import jadx.tests.api.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class SmaliReferenceResolverTest extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class Sample {
		public static int staticCount;
		private int count;
		private String[][] matrix;

		static {
			staticCount = 42;
		}

		public Sample() {
			this.count = 1;
		}

		public Sample(int initial, String[] tags) {
			this.count = initial;
			this.matrix = new String[][] { tags };
		}

		public void overloaded(int value) {
			this.count += value;
		}

		public String overloaded(int value, String[] arr, boolean flag) {
			return flag ? arr[value] : "";
		}

		public boolean[] primitives(boolean z, byte b, char c, short s, int i, long j, float f, double d) {
			return new boolean[] { z };
		}

		public int callCollaborators() {
			Child child = new Child();
			child.childMethod();
			child.baseMethod();
			Inner inner = new Inner("hi");
			new Inner.Deep().ping();
			return child.baseField + inner.getLabel().length() + staticCount + count + matrix.length;
		}

		public static class Base {
			protected int baseField = 7;

			public void baseMethod() {
			}
		}

		public static class Child extends Base {
			public void childMethod() {
			}
		}

		public static class Inner {
			private String label;

			public Inner(String label) {
				this.label = label;
			}

			public String getLabel() {
				return label;
			}

			public static class Deep {
				public void ping() {
				}
			}
		}
	}

	// --- Parsing tests: method references ---

	@Test
	void parseMethodReferenceClassVsMethodCursorPositions() {
		String line = "    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;";
		int clsStart = line.indexOf("Ljava/lang/StringBuilder;");
		int clsEnd = clsStart + "Ljava/lang/StringBuilder;".length();
		int arrowIdx = line.indexOf("->");
		int mthEnd = line.length();

		SmaliRef expectedClass = new SmaliRef(RefKind.CLASS, "Ljava/lang/StringBuilder;", null);
		SmaliRef expectedMethod = new SmaliRef(RefKind.METHOD, "Ljava/lang/StringBuilder;",
				"append(Ljava/lang/String;)Ljava/lang/StringBuilder;");

		// Before reference
		assertThat(SmaliReferenceResolver.parse(line, 0)).isNull();
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("invoke-virtual"))).isNull();
		assertThat(SmaliReferenceResolver.parse(line, clsStart - 1)).isNull();

		// Anywhere on class descriptor before ->
		assertThat(SmaliReferenceResolver.parse(line, clsStart)).isEqualTo(expectedClass);
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("StringBuilder"))).isEqualTo(expectedClass);
		assertThat(SmaliReferenceResolver.parse(line, clsEnd - 1)).isEqualTo(expectedClass);

		// Anywhere on -> or method name or method descriptor
		assertThat(SmaliReferenceResolver.parse(line, arrowIdx)).isEqualTo(expectedMethod);
		assertThat(SmaliReferenceResolver.parse(line, arrowIdx + 1)).isEqualTo(expectedMethod);
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("append"))).isEqualTo(expectedMethod);
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("(Ljava/lang/String;)"))).isEqualTo(expectedMethod);
		assertThat(SmaliReferenceResolver.parse(line, line.lastIndexOf("StringBuilder"))).isEqualTo(expectedMethod);
		assertThat(SmaliReferenceResolver.parse(line, mthEnd - 1)).isEqualTo(expectedMethod);

		// Past end of line
		assertThat(SmaliReferenceResolver.parse(line, mthEnd)).isNull();
	}

	@Test
	void parseConstructorAndClassInitMethodReferences() {
		String initLine = "    invoke-direct {v0, v1}, Lcom/example/Foo$Bar;-><init>(I[Ljava/lang/String;)V";
		assertThat(SmaliReferenceResolver.parse(initLine, initLine.indexOf("Foo$Bar")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Foo$Bar;", null));
		assertThat(SmaliReferenceResolver.parse(initLine, initLine.indexOf("<init>")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lcom/example/Foo$Bar;", "<init>(I[Ljava/lang/String;)V"));
		assertThat(SmaliReferenceResolver.parse(initLine, initLine.length() - 1))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lcom/example/Foo$Bar;", "<init>(I[Ljava/lang/String;)V"));

		String clinitLine = "    invoke-static {}, Lcom/example/Foo;-><clinit>()V";
		assertThat(SmaliReferenceResolver.parse(clinitLine, clinitLine.indexOf("->")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lcom/example/Foo;", "<clinit>()V"));
		assertThat(SmaliReferenceResolver.parse(clinitLine, clinitLine.indexOf("<clinit>")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lcom/example/Foo;", "<clinit>()V"));
	}

	@Test
	void parseMethodWithAllPrimitivesArraysAndInnerClasses() {
		String line = "    invoke-virtual {v0}, Lpkg/Outer$Inner$Deep;->all(ZBCSIJFD[[I[Lpkg/Item;)[[Ljava/lang/String;";
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("Outer$Inner$Deep")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lpkg/Outer$Inner$Deep;", null));
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("all(")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lpkg/Outer$Inner$Deep;",
						"all(ZBCSIJFD[[I[Lpkg/Item;)[[Ljava/lang/String;"));
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("ZBCSIJFD")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lpkg/Outer$Inner$Deep;",
						"all(ZBCSIJFD[[I[Lpkg/Item;)[[Ljava/lang/String;"));

		// Default package class (no '/')
		String defaultPkg = "    invoke-static {}, LTopDefault;->run()I";
		assertThat(SmaliReferenceResolver.parse(defaultPkg, defaultPkg.indexOf("TopDefault")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "LTopDefault;", null));
		assertThat(SmaliReferenceResolver.parse(defaultPkg, defaultPkg.indexOf("run")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "LTopDefault;", "run()I"));
	}

	@Test
	void parseArrayReceiverMethodReference() {
		String objArrLine = "    invoke-virtual {v0}, [Lcom/example/Item;->clone()Ljava/lang/Object;";
		assertThat(SmaliReferenceResolver.parse(objArrLine, objArrLine.indexOf("[Lcom")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Item;", null));
		assertThat(SmaliReferenceResolver.parse(objArrLine, objArrLine.indexOf("Item")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Item;", null));
		assertThat(SmaliReferenceResolver.parse(objArrLine, objArrLine.indexOf("clone")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lcom/example/Item;", "clone()Ljava/lang/Object;"));

		// Primitive array receiver must not misparse return type Ljava/lang/Object; as standalone class
		String primArrLine = "    invoke-virtual {v0}, [I->clone()Ljava/lang/Object;";
		for (int col = 0; col < primArrLine.length(); col++) {
			assertThat(SmaliReferenceResolver.parse(primArrLine, col))
					.as("column %d in %s", col, primArrLine)
					.isNull();
		}
	}

	@Test
	void parseDebugModelAndPolymorphicAndMethodHandleLines() {
		String dbgLine = "0000001a: 7110 0200 0000     0000: invoke-static      {v0}, Lcom/example/Helper;->work(I)V # method@0002";
		assertThat(SmaliReferenceResolver.parse(dbgLine, dbgLine.indexOf("0000001a:"))).isNull();
		assertThat(SmaliReferenceResolver.parse(dbgLine, dbgLine.indexOf("Helper")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Helper;", null));
		assertThat(SmaliReferenceResolver.parse(dbgLine, dbgLine.indexOf("work")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lcom/example/Helper;", "work(I)V"));
		assertThat(SmaliReferenceResolver.parse(dbgLine, dbgLine.indexOf("method@0002"))).isNull();

		String polyLine =
				"    invoke-polymorphic {v0}, Ljava/lang/invoke/MethodHandle;->invoke([Ljava/lang/Object;)Ljava/lang/Object;, (Lcom/example/Arg;)Lcom/example/Ret;";
		assertThat(SmaliReferenceResolver.parse(polyLine, polyLine.indexOf("invoke(")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Ljava/lang/invoke/MethodHandle;",
						"invoke([Ljava/lang/Object;)Ljava/lang/Object;"));
		assertThat(SmaliReferenceResolver.parse(polyLine, polyLine.indexOf("Arg")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Arg;", null));
		assertThat(SmaliReferenceResolver.parse(polyLine, polyLine.indexOf("Ret")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Ret;", null));

		String mhLine = "    const-method-handle v0, invoke-static@Lcom/example/Foo;->handle(I)V";
		assertThat(SmaliReferenceResolver.parse(mhLine, mhLine.indexOf("->handle")))
				.isEqualTo(new SmaliRef(RefKind.METHOD, "Lcom/example/Foo;", "handle(I)V"));
	}

	// --- Parsing tests: field references ---

	@Test
	void parseFieldReferenceCursorPositions() {
		String primField = "    iget v0, p0, Lcom/example/State;->count:I";
		assertThat(SmaliReferenceResolver.parse(primField, primField.indexOf("State")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/State;", null));
		assertThat(SmaliReferenceResolver.parse(primField, primField.indexOf("->")))
				.isEqualTo(new SmaliRef(RefKind.FIELD, "Lcom/example/State;", "count:I"));
		assertThat(SmaliReferenceResolver.parse(primField, primField.indexOf("count")))
				.isEqualTo(new SmaliRef(RefKind.FIELD, "Lcom/example/State;", "count:I"));
		assertThat(SmaliReferenceResolver.parse(primField, primField.indexOf(":I")))
				.isEqualTo(new SmaliRef(RefKind.FIELD, "Lcom/example/State;", "count:I"));
		assertThat(SmaliReferenceResolver.parse(primField, primField.length() - 1))
				.isEqualTo(new SmaliRef(RefKind.FIELD, "Lcom/example/State;", "count:I"));

		String arrObjField = "    sget-object v0, Lcom/example/Outer$Inner;->names:[[Ljava/lang/String;";
		assertThat(SmaliReferenceResolver.parse(arrObjField, arrObjField.indexOf("Outer$Inner")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Outer$Inner;", null));
		assertThat(SmaliReferenceResolver.parse(arrObjField, arrObjField.indexOf("names")))
				.isEqualTo(new SmaliRef(RefKind.FIELD, "Lcom/example/Outer$Inner;", "names:[[Ljava/lang/String;"));
		assertThat(SmaliReferenceResolver.parse(arrObjField, arrObjField.indexOf("String")))
				.isEqualTo(new SmaliRef(RefKind.FIELD, "Lcom/example/Outer$Inner;", "names:[[Ljava/lang/String;"));

		String enumLine = "    value = .enum Lcom/example/Mode;->FAST:Lcom/example/Mode;";
		assertThat(SmaliReferenceResolver.parse(enumLine, enumLine.indexOf("Mode")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Mode;", null));
		assertThat(SmaliReferenceResolver.parse(enumLine, enumLine.indexOf("FAST")))
				.isEqualTo(new SmaliRef(RefKind.FIELD, "Lcom/example/Mode;", "FAST:Lcom/example/Mode;"));
	}

	// --- Parsing tests: standalone class/type descriptors ---

	@Test
	void parseStandaloneClassDescriptors() {
		String superLine = ".super Lcom/example/BaseActivity;";
		assertThat(SmaliReferenceResolver.parse(superLine, superLine.indexOf(".super"))).isNull();
		assertThat(SmaliReferenceResolver.parse(superLine, superLine.indexOf("BaseActivity")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/BaseActivity;", null));

		String implLine = ".implements Lcom/example/ClickListener;";
		assertThat(SmaliReferenceResolver.parse(implLine, implLine.indexOf("ClickListener")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/ClickListener;", null));

		String newInstLine = "    new-instance v0, Lcom/example/Outer$Inner;";
		assertThat(SmaliReferenceResolver.parse(newInstLine, newInstLine.indexOf("Outer$Inner")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Outer$Inner;", null));

		String checkCastLine = "    check-cast v0, [Lcom/example/Item;";
		assertThat(SmaliReferenceResolver.parse(checkCastLine, checkCastLine.indexOf("[Lcom")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Item;", null));
		assertThat(SmaliReferenceResolver.parse(checkCastLine, checkCastLine.indexOf("Item")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Item;", null));

		String constClassLine = "    const-class v0, Lcom/example/Target;";
		assertThat(SmaliReferenceResolver.parse(constClassLine, constClassLine.indexOf("Target")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Target;", null));

		String fieldDecl = ".field private helper:Lcom/example/Helper;";
		assertThat(SmaliReferenceResolver.parse(fieldDecl, fieldDecl.indexOf("helper"))).isNull();
		assertThat(SmaliReferenceResolver.parse(fieldDecl, fieldDecl.indexOf("Helper")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Helper;", null));

		String catchLine = "    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0";
		assertThat(SmaliReferenceResolver.parse(catchLine, catchLine.indexOf("IOException")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Ljava/io/IOException;", null));

		String dbgInterfaces = "Interfaces: [Lcom/example/IfaceA;, Lcom/example/IfaceB;]";
		assertThat(SmaliReferenceResolver.parse(dbgInterfaces, dbgInterfaces.indexOf("IfaceA")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/IfaceA;", null));
		assertThat(SmaliReferenceResolver.parse(dbgInterfaces, dbgInterfaces.indexOf("IfaceB")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/IfaceB;", null));
	}

	@Test
	void parseMethodDeclarationParameterAndReturnTypes() {
		String line = ".method public Load(ILcom/example/Arg1;Z[Lcom/example/Arg2;)Lcom/example/Ret;";
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("Load"))).isNull();
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("ILcom"))).isNull();
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("Arg1")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Arg1;", null));
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf(";Z[") + 1)).isNull();
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("[Lcom/example/Arg2;")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Arg2;", null));
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("Arg2")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Arg2;", null));
		assertThat(SmaliReferenceResolver.parse(line, line.indexOf("Ret")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Ret;", null));
	}

	@Test
	void parseNonReferenceAndMalformedLines() {
		assertThat(SmaliReferenceResolver.parse(null, 0)).isNull();
		assertThat(SmaliReferenceResolver.parse("", 0)).isNull();
		assertThat(SmaliReferenceResolver.parse(".super Lcom/example/A;", -1)).isNull();
		assertThat(SmaliReferenceResolver.parse(".super Lcom/example/A;", 100)).isNull();

		String[] nonRefLines = {
				"    return-void",
				"    const/4 v0, 0x1",
				"    .registers 5",
				"    :goto_0",
				"    new-array v0, v1, [I",
				"# Lcom/example/Foo;->bar()V",
				"    return-void # Lcom/example/Foo;",
				"    const-string v0, \"Lcom/example/Foo;->bar()V\"",
				"    const-string v0, \"escaped \\\" Lcom/example/Foo;\"",
				"    \"Landroid/widget/AdapterView<*>;\",",
				"    L;",
				"    L/Bad;",
				"    Lpkg//Bad;",
				"    Lpkg/Incomplete"
		};
		for (String line : nonRefLines) {
			for (int col = 0; col < line.length(); col++) {
				assertThat(SmaliReferenceResolver.parse(line, col))
						.as("column %d in %s", col, line)
						.isNull();
			}
		}

		// Malformed member after -> must not resolve as a member reference
		String[] malformedMemberLines = {
				"    Lpkg/Bad;->",
				"    Lpkg/Bad;->broken",
				"    Lpkg/Bad;->bad(V)V",
				"    Lpkg/Bad;->bad()Void",
				"    Lpkg/Bad;->badField:V",
				"    Lpkg/Bad;->badField:Invalid"
		};
		for (String line : malformedMemberLines) {
			int arrowIdx = line.indexOf("->");
			assertThat(SmaliReferenceResolver.parse(line, line.indexOf("Bad")))
					.isEqualTo(new SmaliRef(RefKind.CLASS, "Lpkg/Bad;", null));
			for (int col = arrowIdx; col < line.length(); col++) {
				assertThat(SmaliReferenceResolver.parse(line, col))
						.as("column %d in %s", col, line)
						.isNull();
			}
		}

		// Local directive: quoted var name ignored, actual type resolves, quoted generic signature ignored
		String localLine = "    .local v0, \"val\":Lcom/example/Val;, \"Lcom/example/Val<Lcom/example/T;>;\"";
		assertThat(SmaliReferenceResolver.parse(localLine, localLine.indexOf("val"))).isNull();
		assertThat(SmaliReferenceResolver.parse(localLine, localLine.indexOf("Val;,")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/Val;", null));
		assertThat(SmaliReferenceResolver.parse(localLine, localLine.lastIndexOf("Val<"))).isNull();

		// Escaped backslash before closing quote: reference after string still resolves
		String afterQuotedBackslash = "    const-string v0, \"path\\\\\" Lcom/example/After;";
		assertThat(SmaliReferenceResolver.parse(afterQuotedBackslash, afterQuotedBackslash.indexOf("path"))).isNull();
		assertThat(SmaliReferenceResolver.parse(afterQuotedBackslash, afterQuotedBackslash.indexOf("After")))
				.isEqualTo(new SmaliRef(RefKind.CLASS, "Lcom/example/After;", null));
	}

	// --- Resolution tests against real JadxDecompiler ---

	@Test
	void resolveAgainstRealJadxDecompiler() {
		disableCompilation();
		ClassNode sampleCls = getClassNode(Sample.class);
		String sampleDesc = 'L' + sampleCls.getRawName().replace('.', '/') + ';';
		String innerDesc = 'L' + sampleCls.getRawName().replace('.', '/') + "$Inner;";
		String deepDesc = 'L' + sampleCls.getRawName().replace('.', '/') + "$Inner$Deep;";
		String baseDesc = 'L' + sampleCls.getRawName().replace('.', '/') + "$Base;";
		String childDesc = 'L' + sampleCls.getRawName().replace('.', '/') + "$Child;";

		// 1. Standalone class descriptors (top-level & nested inner classes)
		String newInstLine = "    new-instance v0, " + deepDesc;
		JavaNode deepClsNode = SmaliReferenceResolver.resolve(jadxDecompiler, newInstLine, newInstLine.indexOf("Deep"));
		assertThat(deepClsNode).isInstanceOf(JavaClass.class);
		assertThat(((JavaClass) deepClsNode).getRawName()).endsWith("Sample$Inner$Deep");

		// 2. Method references: overloaded, primitives, arrays, <init>, <clinit>
		String overloadLine = "    invoke-virtual {v0, v1, v2, v3}, " + sampleDesc
				+ "->overloaded(I[Ljava/lang/String;Z)Ljava/lang/String;";
		JavaNode onCls = SmaliReferenceResolver.resolve(jadxDecompiler, overloadLine, overloadLine.indexOf("Sample"));
		assertThat(onCls).isInstanceOf(JavaClass.class);
		assertThat(((JavaClass) onCls).getClassNode()).isEqualTo(sampleCls);

		JavaNode onOverload = SmaliReferenceResolver.resolve(jadxDecompiler, overloadLine, overloadLine.indexOf("overloaded"));
		assertThat(onOverload).isInstanceOf(JavaMethod.class);
		assertThat(((JavaMethod) onOverload).getShortId()).isEqualTo("overloaded(I[Ljava/lang/String;Z)Ljava/lang/String;");

		String primsLine = "    invoke-virtual/range {v0 .. v8}, " + sampleDesc
				+ "->primitives(ZBCSIJFD)[Z";
		JavaNode onPrims = SmaliReferenceResolver.resolve(jadxDecompiler, primsLine, primsLine.indexOf("ZBCSIJFD"));
		assertThat(onPrims).isInstanceOf(JavaMethod.class);
		assertThat(((JavaMethod) onPrims).getShortId()).isEqualTo("primitives(ZBCSIJFD)[Z");

		String initLine = "    invoke-direct {v0, v1, v2}, " + sampleDesc + "-><init>(I[Ljava/lang/String;)V";
		JavaNode onInit = SmaliReferenceResolver.resolve(jadxDecompiler, initLine, initLine.indexOf("<init>"));
		assertThat(onInit).isInstanceOf(JavaMethod.class);
		assertThat(((JavaMethod) onInit).getShortId()).isEqualTo("<init>(I[Ljava/lang/String;)V");

		String clinitLine = "    invoke-static {}, " + sampleDesc + "-><clinit>()V";
		JavaNode onClinit = SmaliReferenceResolver.resolve(jadxDecompiler, clinitLine, clinitLine.indexOf("<clinit>"));
		assertThat(onClinit).isInstanceOf(JavaMethod.class);
		assertThat(((JavaMethod) onClinit).getShortId()).isEqualTo("<clinit>()V");

		// 3. Inherited method & field resolution via Child -> Base
		String inheritedMthLine = "    invoke-virtual {v0}, " + childDesc + "->baseMethod()V";
		JavaNode onInheritedMth = SmaliReferenceResolver.resolve(jadxDecompiler, inheritedMthLine,
				inheritedMthLine.indexOf("baseMethod"));
		assertThat(onInheritedMth).isInstanceOf(JavaMethod.class);
		assertThat(((JavaMethod) onInheritedMth).getDeclaringClass().getRawName()).endsWith("$Base");

		String inheritedFldLine = "    iget v0, v1, " + childDesc + "->baseField:I";
		JavaNode onInheritedFld = SmaliReferenceResolver.resolve(jadxDecompiler, inheritedFldLine,
				inheritedFldLine.indexOf("baseField"));
		assertThat(onInheritedFld).isInstanceOf(JavaField.class);
		assertThat(((JavaField) onInheritedFld).getDeclaringClass().getRawName()).endsWith("$Base");

		// 4. Direct fields (primitive, static, 2D array, object)
		String matrixFldLine = "    iget-object v0, p0, " + sampleDesc + "->matrix:[[Ljava/lang/String;";
		JavaNode onMatrixCls = SmaliReferenceResolver.resolve(jadxDecompiler, matrixFldLine,
				matrixFldLine.indexOf("Sample"));
		assertThat(onMatrixCls).isInstanceOf(JavaClass.class);
		JavaNode onMatrixFld = SmaliReferenceResolver.resolve(jadxDecompiler, matrixFldLine,
				matrixFldLine.indexOf("matrix"));
		assertThat(onMatrixFld).isInstanceOf(JavaField.class);
		assertThat(((JavaField) onMatrixFld).getShortId()).isEqualTo("matrix:[[Ljava/lang/String;");

		String innerFldLine = "    iget-object v0, p0, " + innerDesc + "->label:Ljava/lang/String;";
		JavaNode onInnerFld = SmaliReferenceResolver.resolve(jadxDecompiler, innerFldLine,
				innerFldLine.indexOf("label"));
		assertThat(onInnerFld).isInstanceOf(JavaField.class);
		assertThat(((JavaField) onInnerFld).getDeclaringClass().getRawName()).endsWith("$Inner");

		// 5. Fallback to class when method or field is not found on existing class
		String missingMthLine = "    invoke-virtual {v0}, " + baseDesc + "->toString()Ljava/lang/String;";
		JavaNode fallbackFromMth = SmaliReferenceResolver.resolve(jadxDecompiler, missingMthLine,
				missingMthLine.indexOf("toString"));
		assertThat(fallbackFromMth).isInstanceOf(JavaClass.class);
		assertThat(((JavaClass) fallbackFromMth).getRawName()).endsWith("$Base");

		String missingFldLine = "    iget v0, v1, " + baseDesc + "->unknownField:I";
		JavaNode fallbackFromFld = SmaliReferenceResolver.resolve(jadxDecompiler, missingFldLine,
				missingFldLine.indexOf("unknownField"));
		assertThat(fallbackFromFld).isInstanceOf(JavaClass.class);
		assertThat(((JavaClass) fallbackFromFld).getRawName()).endsWith("$Base");

		// 6. Unresolvable external SDK class returns null
		String externalLine = "    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;";
		assertThat(SmaliReferenceResolver.resolve(jadxDecompiler, externalLine, externalLine.indexOf("StringBuilder")))
				.isNull();
		assertThat(SmaliReferenceResolver.resolve(jadxDecompiler, externalLine, externalLine.indexOf("toString")))
				.isNull();

		// 7. Verify resolution directly against disassembled Smali produced by JADX
		String smaliCode = sampleCls.getDisassembledCode();
		List<String> smaliLines = smaliCode.lines().collect(Collectors.toList());
		String pingInvokeLine = smaliLines.stream()
				.filter(l -> l.contains("$Inner$Deep;->ping()V"))
				.findFirst()
				.orElseThrow();
		JavaNode resolvedFromSmali = SmaliReferenceResolver.resolve(jadxDecompiler, pingInvokeLine,
				pingInvokeLine.indexOf("ping"));
		assertThat(resolvedFromSmali).isInstanceOf(JavaMethod.class);
		assertThat(((JavaMethod) resolvedFromSmali).getShortId()).isEqualTo("ping()V");
		assertThat(((JavaMethod) resolvedFromSmali).getDeclaringClass().getRawName()).endsWith("$Inner$Deep");

		// 8. Renamed class, method, and field still resolve via original Smali descriptors
		MethodNode overloadMth = sampleCls.searchMethodByShortId("overloaded(I[Ljava/lang/String;Z)Ljava/lang/String;");
		FieldNode countFld = sampleCls.searchFieldByName("count");
		sampleCls.getClassInfo().changePkgAndName("renamed.pkg", "RenamedSample");
		overloadMth.getMethodInfo().setAlias("renamedOverload");
		countFld.getFieldInfo().setAlias("renamedCount");

		JavaNode renamedClsTarget = SmaliReferenceResolver.resolve(jadxDecompiler, overloadLine,
				overloadLine.indexOf("Sample"));
		assertThat(renamedClsTarget).isInstanceOf(JavaClass.class);
		assertThat(((JavaClass) renamedClsTarget).getFullName()).isEqualTo("renamed.pkg.RenamedSample");

		JavaNode renamedMthTarget = SmaliReferenceResolver.resolve(jadxDecompiler, overloadLine,
				overloadLine.indexOf("overloaded"));
		assertThat(renamedMthTarget).isInstanceOf(JavaMethod.class);
		assertThat(renamedMthTarget.getName()).isEqualTo("renamedOverload");

		String countFldLine = "    iget v0, p0, " + sampleDesc + "->count:I";
		JavaNode renamedFldTarget = SmaliReferenceResolver.resolve(jadxDecompiler, countFldLine,
				countFldLine.indexOf("count"));
		assertThat(renamedFldTarget).isInstanceOf(JavaField.class);
		assertThat(renamedFldTarget.getName()).isEqualTo("renamedCount");
	}

	@Test
	void resolveNullAndUnloadedDecompilerReturnsNull() {
		String line = "    invoke-static {}, Lcom/example/Foo;->bar()V";
		assertThat(SmaliReferenceResolver.resolve(null, line, line.indexOf("bar"))).isNull();
		try (JadxDecompiler emptyDecompiler = new JadxDecompiler()) {
			assertThat(SmaliReferenceResolver.resolve(emptyDecompiler, line, line.indexOf("bar"))).isNull();
		}
	}

	@Test
	void resolveDefaultPackageClassFromSmaliFile(@TempDir Path tempDir) throws Exception {
		Path smaliFile = tempDir.resolve("DefaultPkgTarget.smali");
		String smaliContent = ""
				+ ".class public LDefaultPkgTarget;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".field public value:I\n"
				+ ".method public constructor <init>()V\n"
				+ "    .registers 1\n"
				+ "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
				+ "    return-void\n"
				+ ".end method\n"
				+ ".method public greet()V\n"
				+ "    .registers 1\n"
				+ "    return-void\n"
				+ ".end method\n";
		Files.writeString(smaliFile, smaliContent);

		JadxArgs jadxArgs = new JadxArgs();
		jadxArgs.setInputFiles(Collections.singletonList(smaliFile.toFile()));
		jadxArgs.setOutDir(tempDir.toFile());
		jadxArgs.setSkipResources(true);
		try (JadxDecompiler decompiler = new JadxDecompiler(jadxArgs)) {
			decompiler.load();

			String invokeLine = "    invoke-virtual {v0}, LDefaultPkgTarget;->greet()V";
			JavaNode clsNode = SmaliReferenceResolver.resolve(decompiler, invokeLine, invokeLine.indexOf("DefaultPkgTarget"));
			assertThat(clsNode).isInstanceOf(JavaClass.class);
			assertThat(((JavaClass) clsNode).getRawName()).isEqualTo("DefaultPkgTarget");

			JavaNode mthNode = SmaliReferenceResolver.resolve(decompiler, invokeLine, invokeLine.indexOf("greet"));
			assertThat(mthNode).isInstanceOf(JavaMethod.class);
			assertThat(((JavaMethod) mthNode).getShortId()).isEqualTo("greet()V");

			String fldLine = "    iget v0, p0, LDefaultPkgTarget;->value:I";
			JavaNode fldNode = SmaliReferenceResolver.resolve(decompiler, fldLine, fldLine.indexOf("value"));
			assertThat(fldNode).isInstanceOf(JavaField.class);
			assertThat(((JavaField) fldNode).getShortId()).isEqualTo("value:I");
		}
	}
}
