.class public final enum Lenums/TestEnumWithFields;
.super Ljava/lang/Enum;


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lenums/TestEnumWithFields;",
        ">;"
    }
.end annotation


# static fields
.field public static final synthetic $VALUES:[Lenums/TestEnumWithFields;

.field public static final DEFAULT:Lenums/TestEnumWithFields;

.field public static final enum DISABLED:Lenums/TestEnumWithFields;

.field public static final enum FIVE_SECONDS:Lenums/TestEnumWithFields;

.field public static final MAX:Lenums/TestEnumWithFields;

.field public static final enum TWO_AND_A_HALF_SECONDS:Lenums/TestEnumWithFields;

.field public static final sValues:[Lenums/TestEnumWithFields;


# instance fields
.field public final mRawValue:I


# direct methods
.method public static constructor <clinit>()V
    .locals 6

    .line 1
    new-instance v0, Lenums/TestEnumWithFields;

    const/4 v1, 0x0

    const-string v2, "DISABLED"

    invoke-direct {v0, v2, v1, v1}, Lenums/TestEnumWithFields;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lenums/TestEnumWithFields;->DISABLED:Lenums/TestEnumWithFields;

    .line 2
    new-instance v0, Lenums/TestEnumWithFields;

    const/4 v2, 0x1

    const-string v3, "TWO_AND_A_HALF_SECONDS"

    invoke-direct {v0, v3, v2, v2}, Lenums/TestEnumWithFields;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lenums/TestEnumWithFields;->TWO_AND_A_HALF_SECONDS:Lenums/TestEnumWithFields;

    .line 3
    new-instance v0, Lenums/TestEnumWithFields;

    const/4 v3, 0x2

    const-string v4, "FIVE_SECONDS"

    invoke-direct {v0, v4, v3, v3}, Lenums/TestEnumWithFields;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lenums/TestEnumWithFields;->FIVE_SECONDS:Lenums/TestEnumWithFields;

    const/4 v4, 0x3

    new-array v4, v4, [Lenums/TestEnumWithFields;

    .line 4
    sget-object v5, Lenums/TestEnumWithFields;->DISABLED:Lenums/TestEnumWithFields;

    aput-object v5, v4, v1

    sget-object v1, Lenums/TestEnumWithFields;->TWO_AND_A_HALF_SECONDS:Lenums/TestEnumWithFields;

    aput-object v1, v4, v2

    aput-object v0, v4, v3

    sput-object v4, Lenums/TestEnumWithFields;->$VALUES:[Lenums/TestEnumWithFields;

    .line 5
    sput-object v5, Lenums/TestEnumWithFields;->DEFAULT:Lenums/TestEnumWithFields;

    .line 6
    sput-object v0, Lenums/TestEnumWithFields;->MAX:Lenums/TestEnumWithFields;

    .line 7
    invoke-static {}, Lenums/TestEnumWithFields;->values()[Lenums/TestEnumWithFields;

    move-result-object v0

    sput-object v0, Lenums/TestEnumWithFields;->sValues:[Lenums/TestEnumWithFields;

    return-void
.end method

.method public constructor <init>(Ljava/lang/String;II)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(I)V"
        }
    .end annotation

    .line 1
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    iput p3, p0, Lenums/TestEnumWithFields;->mRawValue:I

    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnumWithFields;
    .locals 1

    .line 1
    const-class v0, Lenums/TestEnumWithFields;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lenums/TestEnumWithFields;

    return-object p0
.end method

.method public static values()[Lenums/TestEnumWithFields;
    .locals 1

    .line 1
    sget-object v0, Lenums/TestEnumWithFields;->$VALUES:[Lenums/TestEnumWithFields;

    invoke-virtual {v0}, [Lenums/TestEnumWithFields;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lenums/TestEnumWithFields;

    return-object v0
.end method
