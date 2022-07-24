###### Class jadx.tests.integration.enums.TestEnums3$TestCls.Numbers (jadx.tests.integration.enums.TestEnums3$TestCls$Numbers)
.class public final enum Lenums/TestEnumObfuscated;
.super Ljava/lang/Enum;

# static fields
.field private static final synthetic $VLS:[Lenums/TestEnumObfuscated;
.field public static final enum ONE:Lenums/TestEnumObfuscated;
.field public static final enum TWO:Lenums/TestEnumObfuscated;

# instance fields
.field private final num:I

# direct methods
.method static constructor <clinit>()V
    .registers 7

    .prologue
    const/4 v6, 0x3
    const/4 v5, 0x0
    const/4 v4, 0x2
    const/4 v3, 0x1

    new-instance v0, Lenums/TestEnumObfuscated;
    const-string v1, "ONE"
    invoke-direct {v0, v1, v5, v3}, Lenums/TestEnumObfuscated;-><init>(Ljava/lang/String;II)V
    sput-object v0, Lenums/TestEnumObfuscated;->ONE:Lenums/TestEnumObfuscated;
    new-instance v0, Lenums/TestEnumObfuscated;

    const-string v1, "TWO"
    invoke-direct {v0, v1, v3, v4}, Lenums/TestEnumObfuscated;-><init>(Ljava/lang/String;II)V
    sput-object v0, Lenums/TestEnumObfuscated;->TWO:Lenums/TestEnumObfuscated;
    const/4 v0, 0x2

    new-array v0, v0, [Lenums/TestEnumObfuscated;
    sget-object v1, Lenums/TestEnumObfuscated;->ONE:Lenums/TestEnumObfuscated;
    aput-object v1, v0, v5
    sget-object v1, Lenums/TestEnumObfuscated;->TWO:Lenums/TestEnumObfuscated;
    aput-object v1, v0, v3
    sput-object v0, Lenums/TestEnumObfuscated;->$VLS:[Lenums/TestEnumObfuscated;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;II)V
    .registers 4

    .prologue
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V
    iput p3, p0, Lenums/TestEnumObfuscated;->num:I
    return-void
.end method

.method public static vo(Ljava/lang/String;)Lenums/TestEnumObfuscated;
    .registers 2

    .prologue
    const-class v0, Lenums/TestEnumObfuscated;
    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    move-result-object v0
    check-cast v0, Lenums/TestEnumObfuscated;
    return-object v0
.end method

.method public static vs()[Lenums/TestEnumObfuscated;
    .registers 1

    .prologue
    sget-object v0, Lenums/TestEnumObfuscated;->$VLS:[Lenums/TestEnumObfuscated;
    invoke-virtual {v0}, [Lenums/TestEnumObfuscated;->clone()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, [Lenums/TestEnumObfuscated;
    return-object v0
.end method

.method public static values()[Lenums/TestEnumObfuscated;
    .registers 1
    sget-object v0, Lenums/TestEnumObfuscated;->$VLS:[Lenums/TestEnumObfuscated;
    return v0
.end method

.method public static valuesCount()I
    .registers 2
    invoke-static {v0, p0}, Lenums/TestEnumObfuscated;->vs()[Lenums/TestEnumObfuscated;
    move-result-object v0
    array-length v1, v0
    return v1
.end method

.method public static valuesFieldUse()I
    .registers 2
    sget-object v0, Lenums/TestEnumObfuscated;->$VLS:[Lenums/TestEnumObfuscated;
    array-length v1, v0
    return v1
.end method

.method public synthetic getNum()I
    .registers 2

    .prologue
    iget v0, p0, Lenums/TestEnumObfuscated;->num:I
    return v0
.end method
