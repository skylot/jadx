.class public final enum Lenums/ExtEnum;
.super Ljava/lang/Enum;
.source ""

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lenums/ExtEnum;",
        ">;"
    }
.end annotation


.field public static final enum FIRST:Lenums/ExtEnum;

.field public static final enum SECOND:Lenums/ExtEnum;

.field private static final synthetic $VALUES:[Lenums/ExtEnum;


.method static constructor <clinit>()V
    .registers 4

    new-instance v0, Lenums/ExtEnum;

    const-string v1, "FIRST"

    const/4 v2, 0x0

    invoke-direct {v0, v1, v2}, Lenums/ExtEnum;-><init>(Ljava/lang/String;I)V

    sput-object v0, Lenums/ExtEnum;->FIRST:Lenums/ExtEnum;

    new-instance v1, Lenums/ExtEnum;

    const-string v2, "SECOND"

    const/4 v3, 0x1

    invoke-direct {v1, v2, v3}, Lenums/ExtEnum;-><init>(Ljava/lang/String;I)V

    sput-object v1, Lenums/ExtEnum;->SECOND:Lenums/ExtEnum;

    const/4 v2, 0x2

    new-array v2, v2, [Lenums/ExtEnum;

    const/4 v3, 0x0

    aput-object v0, v2, v3

    const/4 v0, 0x1

    aput-object v1, v2, v0

    sput-object v2, Lenums/ExtEnum;->$VALUES:[Lenums/ExtEnum;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;I)V
    .registers 3

    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/ExtEnum;
    .registers 2

    const-class v0, Lenums/ExtEnum;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lenums/ExtEnum;

    return-object p0
.end method

.method public static values()[Lenums/ExtEnum;
    .registers 1

    sget-object v0, Lenums/ExtEnum;->$VALUES:[Lenums/ExtEnum;

    invoke-virtual {v0}, [Lenums/ExtEnum;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lenums/ExtEnum;

    return-object v0
.end method
