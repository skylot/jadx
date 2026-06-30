.class public final enum Lenums/TestEnumKotlinEntries;
.super Ljava/lang/Enum;
.source "TestEnumKotlinEntries.kt"


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lenums/TestEnumKotlinEntries;",
        ">;"
    }
.end annotation

.annotation runtime Lkotlin/Metadata;
    d1 = {
        "\u0000\u000c\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\u0008\u0005\u0008\u0086\u0081\u0002\u0018\u00002\u0008\u0012\u0004\u0012\u00020\u00000\u0001B\t\u0008\u0002\u00a2\u0006\u0004\u0008\u0002\u0010\u0003j\u0002\u0008\u0004j\u0002\u0008\u0005j\u0002\u0008\u0006"
    }
    d2 = {
        "Lenums/TestEnumKotlinEntries;",
        "",
        "<init>",
        "(Ljava/lang/String;I)V",
        "ALPHA",
        "BETA",
        "GAMMA"
    }
    k = 0x1
    mv = {
        0x2,
        0x3,
        0x0
    }
    xi = 0x30
.end annotation


# static fields
.field private static final synthetic $ENTRIES:Lkotlin/enums/EnumEntries;

.field private static final synthetic $VALUES:[Lenums/TestEnumKotlinEntries;

.field public static final enum ALPHA:Lenums/TestEnumKotlinEntries;

.field public static final enum BETA:Lenums/TestEnumKotlinEntries;

.field public static final enum GAMMA:Lenums/TestEnumKotlinEntries;


# direct methods
.method private static final synthetic $values()[Lenums/TestEnumKotlinEntries;
    .registers 3

    const/4 v0, 0x3

    new-array v0, v0, [Lenums/TestEnumKotlinEntries;

    const/4 v1, 0x0

    sget-object v2, Lenums/TestEnumKotlinEntries;->ALPHA:Lenums/TestEnumKotlinEntries;

    aput-object v2, v0, v1

    const/4 v1, 0x1

    sget-object v2, Lenums/TestEnumKotlinEntries;->BETA:Lenums/TestEnumKotlinEntries;

    aput-object v2, v0, v1

    const/4 v1, 0x2

    sget-object v2, Lenums/TestEnumKotlinEntries;->GAMMA:Lenums/TestEnumKotlinEntries;

    aput-object v2, v0, v1

    return-object v0
.end method

.method static constructor <clinit>()V
    .registers 3

    .line 4
    new-instance v0, Lenums/TestEnumKotlinEntries;

    const-string v1, "ALPHA"

    const/4 v2, 0x0

    invoke-direct {v0, v1, v2}, Lenums/TestEnumKotlinEntries;-><init>(Ljava/lang/String;I)V

    sput-object v0, Lenums/TestEnumKotlinEntries;->ALPHA:Lenums/TestEnumKotlinEntries;

    .line 5
    new-instance v0, Lenums/TestEnumKotlinEntries;

    const-string v1, "BETA"

    const/4 v2, 0x1

    invoke-direct {v0, v1, v2}, Lenums/TestEnumKotlinEntries;-><init>(Ljava/lang/String;I)V

    sput-object v0, Lenums/TestEnumKotlinEntries;->BETA:Lenums/TestEnumKotlinEntries;

    .line 6
    new-instance v0, Lenums/TestEnumKotlinEntries;

    const-string v1, "GAMMA"

    const/4 v2, 0x2

    invoke-direct {v0, v1, v2}, Lenums/TestEnumKotlinEntries;-><init>(Ljava/lang/String;I)V

    sput-object v0, Lenums/TestEnumKotlinEntries;->GAMMA:Lenums/TestEnumKotlinEntries;

    invoke-static {}, Lenums/TestEnumKotlinEntries;->$values()[Lenums/TestEnumKotlinEntries;

    move-result-object v0

    sput-object v0, Lenums/TestEnumKotlinEntries;->$VALUES:[Lenums/TestEnumKotlinEntries;

    check-cast v0, [Ljava/lang/Enum;

    invoke-static {v0}, Lkotlin/enums/EnumEntriesKt;->enumEntries([Ljava/lang/Enum;)Lkotlin/enums/EnumEntries;

    move-result-object v0

    sput-object v0, Lenums/TestEnumKotlinEntries;->$ENTRIES:Lkotlin/enums/EnumEntries;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;I)V
    .registers 3
    .param p1, "$enum$name"    # Ljava/lang/String;
    .param p2, "$enum$ordinal"    # I
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    .line 3
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    return-void
.end method

.method public static getEntries()Lkotlin/enums/EnumEntries;
    .registers 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Lkotlin/enums/EnumEntries<",
            "Lenums/TestEnumKotlinEntries;",
            ">;"
        }
    .end annotation

    sget-object v0, Lenums/TestEnumKotlinEntries;->$ENTRIES:Lkotlin/enums/EnumEntries;

    return-object v0
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnumKotlinEntries;
    .registers 2

    const-class v0, Lenums/TestEnumKotlinEntries;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object v0

    check-cast v0, Lenums/TestEnumKotlinEntries;

    return-object v0
.end method

.method public static values()[Lenums/TestEnumKotlinEntries;
    .registers 1

    sget-object v0, Lenums/TestEnumKotlinEntries;->$VALUES:[Lenums/TestEnumKotlinEntries;

    invoke-virtual {v0}, Ljava/lang/Object;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lenums/TestEnumKotlinEntries;

    return-object v0
.end method
