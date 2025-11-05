.class public Lgenerics/TestMissingGenericsTypes2;
.super Ljava/lang/Object;
.source "TestMissingGenericsTypes2.java"

# interfaces
.implements Ljava/lang/Iterable;


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "<T:",
        "Ljava/lang/Object;",
        ">",
        "Ljava/lang/Object;",
        "Ljava/lang/Iterable<",
        "TT;>;"
    }
.end annotation


# direct methods
.method public constructor <init>()V
    .registers 1

    .local p0, "this":Lgenerics/TestMissingGenericsTypes2;, "Lgenerics/TestMissingGenericsTypes2<TT;>;"
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private doSomething(Ljava/lang/String;)V
    .registers 2
    .param p1, "s"    # Ljava/lang/String;

    .local p0, "this":Lgenerics/TestMissingGenericsTypes2;, "Lgenerics/TestMissingGenericsTypes2<TT;>;"
    return-void
.end method


# virtual methods
.method public iterator()Ljava/util/Iterator;
    .registers 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/Iterator<",
            "TT;>;"
        }
    .end annotation

    .local p0, "this":Lgenerics/TestMissingGenericsTypes2;, "Lgenerics/TestMissingGenericsTypes2<TT;>;"
    const/4 v0, 0x0

    return-object v0
.end method

.method public test(Lgenerics/TestMissingGenericsTypes2;)V
    .registers 4
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Lgenerics/TestMissingGenericsTypes2<",
            "Ljava/lang/String;",
            ">;)V"
        }
    .end annotation

    .local p0, "this":Lgenerics/TestMissingGenericsTypes2;, "Lgenerics/TestMissingGenericsTypes2<TT;>;"
    .local p1, "l":Lgenerics/TestMissingGenericsTypes2;, "Lgenerics/TestMissingGenericsTypes2<Ljava/lang/String;>;"
    invoke-virtual {p1}, Lgenerics/TestMissingGenericsTypes2;->iterator()Ljava/util/Iterator;

    move-result-object v0

    # original:
    # .local v0, "i":Ljava/util/Iterator;, "Ljava/util/Iterator<Ljava/lang/String;>;"
    # manipulated: removed generic type
    .local v0, "i":Ljava/util/Iterator;

    :goto_4
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_14

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    .local v1, "s":Ljava/lang/String;
    invoke-direct {p0, v1}, Lgenerics/TestMissingGenericsTypes2;->doSomething(Ljava/lang/String;)V

    .end local v1    # "s":Ljava/lang/String;
    goto :goto_4

    :cond_14
    return-void
.end method
