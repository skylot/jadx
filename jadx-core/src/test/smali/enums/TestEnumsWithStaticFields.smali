.class public final enum Lenums/TestEnumsWithStaticFields;
.super Ljava/lang/Enum;
.source "SourceFile"

# interfaces
.implements Lx/a/c;


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lx/a/d$a;
    }
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum",
        "<",
        "Lenums/TestEnumsWithStaticFields;",
        ">;",
        "Lx/a/c;"
    }
.end annotation


# static fields
.field public static final enum sA:Lenums/TestEnumsWithStaticFields;

.field private static sB:Lx/a/c;

.field private static final synthetic sC:[Lenums/TestEnumsWithStaticFields;


# direct methods
.method static constructor <clinit>()V
    .registers 4

    .prologue
    const v3, 0x23900

    const/4 v2, 0x0

    invoke-static {v3}, Lx/q;->i(I)V

    .line 10
    new-instance v0, Lenums/TestEnumsWithStaticFields;

    const-string/jumbo v1, "INSTANCE"

    invoke-direct {v0, v1}, Lenums/TestEnumsWithStaticFields;-><init>(Ljava/lang/String;)V

    sput-object v0, Lenums/TestEnumsWithStaticFields;->sA:Lenums/TestEnumsWithStaticFields;

    .line 9
    const/4 v0, 0x1

    new-array v0, v0, [Lenums/TestEnumsWithStaticFields;

    sget-object v1, Lenums/TestEnumsWithStaticFields;->sA:Lenums/TestEnumsWithStaticFields;

    aput-object v1, v0, v2

    sput-object v0, Lenums/TestEnumsWithStaticFields;->sC:[Lenums/TestEnumsWithStaticFields;

    .line 36
    new-instance v0, Lx/a/d$a;

    invoke-direct {v0, v2}, Lx/a/d$a;-><init>(B)V

    sput-object v0, Lenums/TestEnumsWithStaticFields;->sB:Lx/a/c;

    invoke-static {v3}, Lx/q;->o(I)V

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;)V
    .registers 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    .prologue
    .line 9
    const/4 v0, 0x0

    invoke-direct {p0, p1, v0}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    return-void
.end method

.method public static a(Lx/a/c;)V
    .registers 1

    .prologue
    .line 79
    if-eqz p0, :cond_4

    .line 80
    sput-object p0, Lenums/TestEnumsWithStaticFields;->sB:Lx/a/c;

    .line 82
    :cond_4
    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lenums/TestEnumsWithStaticFields;
    .registers 3

    .prologue
    const v1, 0x238f8

    invoke-static {v1}, Lx/q;->i(I)V

    .line 9
    const-class v0, Lenums/TestEnumsWithStaticFields;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object v0

    check-cast v0, Lenums/TestEnumsWithStaticFields;

    invoke-static {v1}, Lx/q;->o(I)V

    return-object v0
.end method

.method public static values()[Lenums/TestEnumsWithStaticFields;
    .registers 2

    .prologue
    const v1, 0x238f7

    invoke-static {v1}, Lx/q;->i(I)V

    .line 9
    sget-object v0, Lenums/TestEnumsWithStaticFields;->sC:[Lenums/TestEnumsWithStaticFields;

    invoke-virtual {v0}, [Lenums/TestEnumsWithStaticFields;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lenums/TestEnumsWithStaticFields;

    invoke-static {v1}, Lx/q;->o(I)V

    return-object v0
.end method


# virtual methods
.method public final FR(I)V
    .registers 4

    .prologue
    const v1, 0x238fb

    invoke-static {v1}, Lx/q;->i(I)V

    .line 96
    sget-object v0, Lenums/TestEnumsWithStaticFields;->sB:Lx/a/c;

    invoke-interface {v0, p1}, Lx/a/c;->FR(I)V

    .line 97
    invoke-static {v1}, Lx/q;->o(I)V

    return-void
.end method

