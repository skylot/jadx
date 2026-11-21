.class public abstract enum Lenums/TestEnumAnonymousSyntheticCtor;
.super Ljava/lang/Enum;

.field private static final synthetic $VALUES:[Lenums/TestEnumAnonymousSyntheticCtor;
.field public static final enum TEST1:Lenums/TestEnumAnonymousSyntheticCtor;

.method static constructor <clinit>()V
    .registers 4

    new-instance v0, Lenums/TestEnumAnonymousSyntheticCtor$1;
    const-string v1, "TEST1"
    const/4 v2, 0x0
    invoke-direct {v0, v1, v2, v1, v2}, Lenums/TestEnumAnonymousSyntheticCtor$1;-><init>(Ljava/lang/String;ILjava/lang/String;I)V
    sput-object v0, Lenums/TestEnumAnonymousSyntheticCtor;->TEST1:Lenums/TestEnumAnonymousSyntheticCtor;

    const/4 v1, 0x1
    new-array v1, v1, [Lenums/TestEnumAnonymousSyntheticCtor;
    aput-object v0, v1, v2
    sput-object v1, Lenums/TestEnumAnonymousSyntheticCtor;->$VALUES:[Lenums/TestEnumAnonymousSyntheticCtor;
    return-void
.end method

.method private constructor <init>(Ljava/lang/String;ILjava/lang/String;I)V
    .registers 5
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V
    return-void
.end method

.method synthetic constructor <init>(Ljava/lang/String;ILjava/lang/String;ILenums/TestEnumAnonymousSyntheticCtor$1;)V
    .registers 6
    invoke-direct {p0, p1, p2, p3, p4}, Lenums/TestEnumAnonymousSyntheticCtor;-><init>(Ljava/lang/String;ILjava/lang/String;I)V
    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnumAnonymousSyntheticCtor;
    .registers 2
    const-class v0, Lenums/TestEnumAnonymousSyntheticCtor;
    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    move-result-object p0
    check-cast p0, Lenums/TestEnumAnonymousSyntheticCtor;
    return-object p0
.end method

.method public static values()[Lenums/TestEnumAnonymousSyntheticCtor;
    .registers 1
    sget-object v0, Lenums/TestEnumAnonymousSyntheticCtor;->$VALUES:[Lenums/TestEnumAnonymousSyntheticCtor;
    invoke-virtual {v0}, [Lenums/TestEnumAnonymousSyntheticCtor;->clone()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, [Lenums/TestEnumAnonymousSyntheticCtor;
    return-object v0
.end method
