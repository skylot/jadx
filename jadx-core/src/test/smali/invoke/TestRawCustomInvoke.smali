.class public Linvoke/TestRawCustomInvoke;
.super Ljava/lang/Object;

.method public static func(ID)Ljava/lang/String;
    .registers 5
    int-to-double v0, p0
    add-double/2addr v0, p1
    invoke-static {v0, v1}, Ljava/lang/String;->valueOf(D)Ljava/lang/String;
    move-result-object p0
    return-object p0
.end method

.method private static staticBootstrap(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
    .registers 5
    :try_start_0
    new-instance v0, Ljava/lang/invoke/ConstantCallSite;
    invoke-virtual {p0}, Ljava/lang/invoke/MethodHandles$Lookup;->lookupClass()Ljava/lang/Class;
    move-result-object v1
    invoke-virtual {p0, v1, p1, p2}, Ljava/lang/invoke/MethodHandles$Lookup;->findStatic(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;
    move-result-object p0
    invoke-direct {v0, p0}, Ljava/lang/invoke/ConstantCallSite;-><init>(Ljava/lang/invoke/MethodHandle;)V
    :try_end_d
    .catch Ljava/lang/NoSuchMethodException; {:try_start_0 .. :try_end_d} :catch_e
    .catch Ljava/lang/IllegalAccessException; {:try_start_0 .. :try_end_d} :catch_e
    return-object v0
    :catch_e
    move-exception p0
    new-instance p1, Ljava/lang/RuntimeException;
    invoke-direct {p1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/Throwable;)V
    throw p1
.end method

.method public test()Ljava/lang/String;
    .registers 3
    :try_start_0

    const/4 v0, 0x1
    const-wide/high16 v1, 0x4000000000000000L    # 2.0
    invoke-custom {v0, v1}, call_site_0("func", (ID)Ljava/lang/String;)@Linvoke/TestRawCustomInvoke;->staticBootstrap(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;

    move-result-object v0

    :try_end_25
    .catchall {:try_start_0 .. :try_end_25} :catchall_26
    return-object v0
    :catchall_26
    move-exception v0
    invoke-static {v0}, Lorg/junit/jupiter/api/Assertions;->fail(Ljava/lang/Throwable;)Ljava/lang/Object;
    const/4 v0, 0x0
    return-object v0
.end method

.method public check()V
    .registers 3
    invoke-virtual {p0}, Linvoke/TestRawCustomInvoke;->test()Ljava/lang/String;
    move-result-object v0
    invoke-static {v0}, Ljadx/tests/api/utils/assertj/JadxAssertions;->assertThat(Ljava/lang/String;)Ljadx/tests/api/utils/assertj/JadxCodeAssertions;
    move-result-object v0
    const-string v1, "3.0"
    invoke-virtual {v0, v1}, Ljadx/tests/api/utils/assertj/JadxCodeAssertions;->isEqualTo(Ljava/lang/String;)Lorg/assertj/core/api/AbstractStringAssert;
    return-void
.end method
