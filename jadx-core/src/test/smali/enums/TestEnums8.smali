.class public final enum Lenums/TestEnums8;
.super Ljava/lang/Enum;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum<",
        "Lenums/TestEnums8;",
        ">;"
    }
.end annotation


# static fields
.field private static final synthetic $VALUES:[Lenums/TestEnums8;

.field private static final FOR_BITS:[Lenums/TestEnums8;

.field public static final enum H:Lenums/TestEnums8;

.field public static final enum L:Lenums/TestEnums8;

.field public static final enum M:Lenums/TestEnums8;

.field public static final enum Q:Lenums/TestEnums8;


# instance fields
.field private final bits:I


# direct methods
.method static constructor <clinit>()V
    .locals 10

    .line 28
    new-instance v0, Lenums/TestEnums8;

    const/4 v1, 0x1

    const/4 v2, 0x0

    const-string v3, "L"

    invoke-direct {v0, v3, v2, v1}, Lenums/TestEnums8;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lenums/TestEnums8;->L:Lenums/TestEnums8;

    .line 30
    new-instance v0, Lenums/TestEnums8;

    const-string v3, "M"

    invoke-direct {v0, v3, v1, v2}, Lenums/TestEnums8;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lenums/TestEnums8;->M:Lenums/TestEnums8;

    .line 32
    new-instance v0, Lenums/TestEnums8;

    const/4 v3, 0x3

    const/4 v4, 0x2

    const-string v5, "Q"

    invoke-direct {v0, v5, v4, v3}, Lenums/TestEnums8;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lenums/TestEnums8;->Q:Lenums/TestEnums8;

    .line 34
    new-instance v0, Lenums/TestEnums8;

    const-string v5, "H"

    invoke-direct {v0, v5, v3, v4}, Lenums/TestEnums8;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lenums/TestEnums8;->H:Lenums/TestEnums8;

    const/4 v0, 0x4

    new-array v5, v0, [Lenums/TestEnums8;

    .line 25
    sget-object v6, Lenums/TestEnums8;->L:Lenums/TestEnums8;

    aput-object v6, v5, v2

    sget-object v7, Lenums/TestEnums8;->M:Lenums/TestEnums8;

    aput-object v7, v5, v1

    sget-object v8, Lenums/TestEnums8;->Q:Lenums/TestEnums8;

    aput-object v8, v5, v4

    sget-object v9, Lenums/TestEnums8;->H:Lenums/TestEnums8;

    aput-object v9, v5, v3

    sput-object v5, Lenums/TestEnums8;->$VALUES:[Lenums/TestEnums8;

    new-array v0, v0, [Lenums/TestEnums8;

    aput-object v7, v0, v2

    aput-object v6, v0, v1

    aput-object v9, v0, v4

    aput-object v8, v0, v3

    .line 36
    sput-object v0, Lenums/TestEnums8;->FOR_BITS:[Lenums/TestEnums8;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;II)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(I)V"
        }
    .end annotation

    .line 40
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    .line 41
    iput p3, p0, Lenums/TestEnums8;->bits:I

    return-void
.end method

.method public static forBits(I)Lenums/TestEnums8;
    .locals 2

    if-ltz p0, :cond_0

    .line 53
    sget-object v0, Lenums/TestEnums8;->FOR_BITS:[Lenums/TestEnums8;

    array-length v1, v0

    if-ge p0, v1, :cond_0

    .line 56
    aget-object p0, v0, p0

    return-object p0

    .line 54
    :cond_0
    new-instance p0, Ljava/lang/IllegalArgumentException;

    invoke-direct {p0}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p0
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnums8;
    .locals 1

    .line 25
    const-class v0, Lenums/TestEnums8;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object p0

    check-cast p0, Lenums/TestEnums8;

    return-object p0
.end method

.method public static values()[Lenums/TestEnums8;
    .locals 1

    .line 25
    sget-object v0, Lenums/TestEnums8;->$VALUES:[Lenums/TestEnums8;

    invoke-virtual {v0}, [Lenums/TestEnums8;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lenums/TestEnums8;

    return-object v0
.end method


# virtual methods
.method public getBits()I
    .locals 1

    .line 45
    iget v0, p0, Lenums/TestEnums8;->bits:I

    return v0
.end method
