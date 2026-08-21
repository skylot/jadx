.class public final enum Lenums/TestEnumExternalSget;
.super Ljava/lang/Enum;
.source ""

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lenums/TestEnumExternalSget;",
        ">;"
    }
.end annotation


.field public static final enum A:Lenums/TestEnumExternalSget;

.field public static final enum B:Lenums/TestEnumExternalSget;

.field public static final enum C:Lenums/TestEnumExternalSget;

.field private static final synthetic $VALUES:[Lenums/TestEnumExternalSget;


# instance fields
.field public final ext:Lenums/ExtEnum;


.method static constructor <clinit>()V
    .registers 10

    # External enum constant reused across two enum fields, so it stays in a
    # register (not inlined) and reaches the enum constructor as a register arg.
    sget-object v0, Lenums/ExtEnum;->FIRST:Lenums/ExtEnum;

    new-instance v1, Lenums/TestEnumExternalSget;

    const-string v5, "A"

    const/4 v6, 0x0

    invoke-direct {v1, v5, v6, v0}, Lenums/TestEnumExternalSget;-><init>(Ljava/lang/String;ILenums/ExtEnum;)V

    sput-object v1, Lenums/TestEnumExternalSget;->A:Lenums/TestEnumExternalSget;

    new-instance v2, Lenums/TestEnumExternalSget;

    const-string v5, "B"

    const/4 v6, 0x1

    invoke-direct {v2, v5, v6, v0}, Lenums/TestEnumExternalSget;-><init>(Ljava/lang/String;ILenums/ExtEnum;)V

    sput-object v2, Lenums/TestEnumExternalSget;->B:Lenums/TestEnumExternalSget;

    new-instance v3, Lenums/TestEnumExternalSget;

    sget-object v4, Lenums/ExtEnum;->SECOND:Lenums/ExtEnum;

    const-string v5, "C"

    const/4 v6, 0x2

    invoke-direct {v3, v5, v6, v4}, Lenums/TestEnumExternalSget;-><init>(Ljava/lang/String;ILenums/ExtEnum;)V

    sput-object v3, Lenums/TestEnumExternalSget;->C:Lenums/TestEnumExternalSget;

    const/4 v5, 0x3

    new-array v5, v5, [Lenums/TestEnumExternalSget;

    const/4 v6, 0x0

    aput-object v1, v5, v6

    const/4 v6, 0x1

    aput-object v2, v5, v6

    const/4 v6, 0x2

    aput-object v3, v5, v6

    sput-object v5, Lenums/TestEnumExternalSget;->$VALUES:[Lenums/TestEnumExternalSget;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;ILenums/ExtEnum;)V
    .registers 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            "Lenums/ExtEnum;",
            ")V"
        }
    .end annotation

    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    iput-object p3, p0, Lenums/TestEnumExternalSget;->ext:Lenums/ExtEnum;

    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnumExternalSget;
    .registers 2

    const-class v0, Lenums/TestEnumExternalSget;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lenums/TestEnumExternalSget;

    return-object p0
.end method

.method public static values()[Lenums/TestEnumExternalSget;
    .registers 1

    sget-object v0, Lenums/TestEnumExternalSget;->$VALUES:[Lenums/TestEnumExternalSget;

    invoke-virtual {v0}, [Lenums/TestEnumExternalSget;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lenums/TestEnumExternalSget;

    return-object v0
.end method
