.class public Linline/TestGetterInlineNegative;
.super Ljava/lang/Object;

.field public static final field:Ljava/lang/String; = "some string"

.method public static synthetic getter()Ljava/lang/String;
    .locals 1

    sget-object v0, Linline/TestGetterInlineNegative;->field:Ljava/lang/String;

    return-object v0
.end method

.method public test()V
    .locals 1

    invoke-static {}, Linline/TestGetterInlineNegative;->getter()Ljava/lang/String;

    return-void
.end method

.method public test2()Ljava/lang/String;
    .locals 2

    invoke-static {}, Linline/TestGetterInlineNegative;->getter()Ljava/lang/String;
    move-result-object v1

    return-object v1
.end method
