package jadx.tests.integration.names;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jadx.api.CommentsLevel;
import jadx.api.JadxInternalAccess;
import jadx.api.data.IJavaNodeRef.RefType;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestClassLocalNames extends SmaliTest {

	@ParameterizedTest
	@CsvSource({ "before, false, false", "after, true, false", "after, true, true" })
	public void test(String pkg, boolean unrelated, boolean reverse) throws IOException {
		noDebugInfo();
		args.setUseImports(false);
		args.setCommentsLevel(CommentsLevel.NONE);
		args.setFsCaseSensitive(true);
		String rawName = pkg + ".Subject";
		JadxCodeData mappings = new JadxCodeData();
		mappings.setRenames(Arrays.asList(
				new JadxCodeRename(new JadxNodeRef(RefType.CLASS, rawName, null), "local.Subject"),
				new JadxCodeRename(new JadxNodeRef(RefType.FIELD, rawName, "mapped:I"), "i2$field3")));
		args.setCodeData(mappings);

		List<File> inputs = new ArrayList<>();
		inputs.add(smali("Subject", ".class public L" + pkg + "/Subject;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".field public c:I\n.field public g:I\n.field public j:I\n"
				+ ".field public i2:I\n.field public i2$field:I\n.field public i2$field2:I\n.field public mapped:I\n"
				+ ".field public duplicate:I\n.field public duplicate:J\n.field public duplicate$field:I\n"
				+ ".method public static factory(ILjava/lang/String;IIII)I\n"
				+ "    .registers 7\n"
				+ "    invoke-virtual {p1}, Ljava/lang/String;->length()I\n"
				+ "    move-result v0\n"
				+ "    add-int/2addr v0, p0\n    add-int/2addr v0, p2\n"
				+ "    add-int/2addr v0, p3\n    add-int/2addr v0, p4\n    add-int/2addr v0, p5\n"
				+ "    return v0\n.end method\n"
				+ ".method public static cycle(Lcaller/Caller;)I\n    .registers 2\n"
				+ "    iget v0, p0, Lcaller/Caller;->i2:I\n    return v0\n.end method\n"
				+ ".method public static reference()I\n    .registers 1\n"
				+ "    invoke-static {}, Li2/Helper;->call()I\n    move-result v0\n    return v0\n.end method\n"));
		inputs.add(smali("Helper", helper("i2/Helper")));
		inputs.add(smali("Caller", ".class public Lcaller/Caller;\n.super Ljava/lang/Object;\n"
				+ ".field public i2:I\n"
				+ ".method public static reference()I\n    .registers 1\n"
				+ "    invoke-static {}, Li2/Helper;->call()I\n    move-result v0\n    return v0\n.end method\n"
				+ ".method public static read(L" + pkg + "/Subject;)I\n    .registers 2\n"
				+ "    iget v0, p0, L" + pkg + "/Subject;->i2:I\n    return v0\n.end method\n"));
		if (unrelated) {
			for (String name : Arrays.asList("c", "g", "j", "i3", "i4", "i5", "i6", "i7")) {
				inputs.add(smali(name, helper(name + "/Unrelated")));
			}
			inputs.add(smali("Earlier", ".class public Laa/Earlier;\n.super Ljava/lang/Object;\n"
					+ ".field public duplicate:I\n.field public duplicate:J\n"));
		}
		if (reverse) {
			Collections.reverse(inputs);
		}
		jadxDecompiler = loadFiles(inputs);
		RootNode root = JadxInternalAccess.getRoot(jadxDecompiler);
		ClassNode subject = root.resolveClass(rawName);
		ClassNode caller = root.resolveClass("caller.Caller");
		if (reverse) {
			CompletableFuture.allOf(
					CompletableFuture.runAsync(subject::decompile),
					CompletableFuture.runAsync(caller::decompile)).join();
		} else {
			caller.decompile();
		}
		decompileAndCheck(root.getClasses(false));
		assertThat(subject).code()
				.containsOne("factory(int i, java.lang.String str, int i3, int i4, int i5, int i6)")
				.containsOne("public int c;")
				.containsOne("public int g;")
				.containsOne("public int j;")
				.containsOne("public int i2$field4;")
				.containsOne("public int i2$field3;")
				.containsOne("public long duplicate$field2;")
				.containsOne(".i2$field;")
				.containsOne("return i2.Helper.call();");
		assertThat(caller).code().containsOne(".i2$field4;");
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	public void testUnreferencedPackages(boolean unrelated) throws IOException {
		noDebugInfo();
		List<File> inputs = new ArrayList<>();
		inputs.add(smali("Factory", ".class public abstract Llocal/Factory;\n.super Ljava/lang/Object;\n"
				+ ".method public abstract factory(ILjava/lang/String;IIII)V\n.end method\n"));
		if (unrelated) {
			for (int i = 2; i <= 9; i++) {
				inputs.add(smali("i" + i, helper("i" + i + "/Unrelated")));
			}
		}
		jadxDecompiler = loadFiles(inputs);
		ClassNode cls = JadxInternalAccess.getRoot(jadxDecompiler).resolveClass("local.Factory");
		decompileAndCheck(cls);
		assertThat(cls.getCode().getCodeStr())
				.containsOne("factory(int i, String str, int i2, int i3, int i4, int i5)");
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	public void testRenderedReferences(boolean imports) throws IOException {
		noDebugInfo();
		args.setUseImports(imports);
		List<File> inputs = new ArrayList<>();
		inputs.add(smali("Scope", ".class public Llocal/Scope;\n.super Ljava/lang/Object;\n"
				+ ".annotation runtime Lmeta/Marker;\n    value = Li3/Helper;\n.end annotation\n"
				+ ".field public i2:I\n.field public i3:I\n.field public i4:I\n.field public i5:I\n"
				+ ".field public Long:I\n.field public static final max:J = 0x7fffffffffffffffL\n"
				+ ".method public static literal(IIII)Ljava/lang/Class;\n    .registers 5\n"
				+ "    const-class v0, Li4/Helper;\n    return-object v0\n.end method\n"
				+ ".method public static aliased()Ljava/lang/Class;\n    .registers 1\n"
				+ "    const-class v0, Lraw/Helper;\n    return-object v0\n.end method\n"));
		inputs.add(smali("Nested", ".class public static Llocal/Scope$Nested;\n.super Ljava/lang/Object;\n"
				+ ".method public static call()I\n    .registers 1\n"
				+ "    invoke-static {}, Li2/Helper;->call()I\n    move-result v0\n    return v0\n.end method\n"));
		inputs.add(smali("Marker", ".class public interface abstract annotation Lmeta/Marker;\n"
				+ ".super Ljava/lang/Object;\n.implements Ljava/lang/annotation/Annotation;\n"
				+ ".method public abstract value()Ljava/lang/Class;\n.end method\n"));
		for (String pkg : Arrays.asList("i2", "i3", "i4", "raw")) {
			inputs.add(smali(pkg, helper(pkg + "/Helper")));
		}
		JadxCodeData mappings = new JadxCodeData();
		mappings.setRenames(Collections.singletonList(
				new JadxCodeRename(new JadxNodeRef(RefType.CLASS, "raw.Helper", null), "i5.Helper")));
		args.setCodeData(mappings);
		jadxDecompiler = loadFiles(inputs);
		RootNode root = JadxInternalAccess.getRoot(jadxDecompiler);
		decompileAndCheck(root.getClasses(false));
		assertThat(root.resolveClass("local.Scope")).code()
				.containsOne("literal(int i, int i6, int i7, int i8)")
				.containsOne("public int i2$field;")
				.containsOne("public int i3$field;")
				.containsOne("public int i4$field;")
				.containsOne("public int i5$field;")
				.containsOne(imports ? "public int Long$field;" : "public int Long;")
				.containsOne("Long.MAX_VALUE");
	}

	private File smali(String name, String code) throws IOException {
		Path file = args.getOutDir().toPath().resolve(name + ".smali");
		Files.writeString(file, code);
		return file.toFile();
	}

	private static String helper(String name) {
		return ".class public L" + name + ";\n.super Ljava/lang/Object;\n"
				+ ".method public static call()I\n    .registers 1\n    const/4 v0, 0x1\n    return v0\n.end method\n";
	}
}
