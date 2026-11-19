.class final enum Lenums/TestEnumAnonymousSyntheticCtor$1;
.super Lenums/TestEnumAnonymousSyntheticCtor;

.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lenums/TestEnumAnonymousSyntheticCtor;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x4010
    name = null
.end annotation

.method constructor <init>(Ljava/lang/String;ILjava/lang/String;I)V
    .registers 11
    const/4 v5, 0x0
    move-object v0, p0
    move-object v1, p1
    move v2, p2
    move-object v3, p3
    move v4, p4
    invoke-direct/range {v0 .. v5}, Lenums/TestEnumAnonymousSyntheticCtor;-><init>(Ljava/lang/String;ILjava/lang/String;ILenums/TestEnumAnonymousSyntheticCtor$1;)V
    return-void
.end method

.method public toString()Ljava/lang/String;
    .registers 2
    const-string v0, "custom"
    return-object v0
.end method
