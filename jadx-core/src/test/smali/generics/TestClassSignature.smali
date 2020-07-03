.class public abstract Lgenerics/TestClassSignature;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/util/Iterator;


# annotations
.annotation system Ldalvik/annotation/Signature;
    value = {
        "<T:",
        "Ljava/lang/Object;",
        ">",
        "Lgenerics/TestClassSignature<",
        "TT;>;"
    }
.end annotation


# instance fields
.field public f:Ljava/lang/Object;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "TT;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>(Ljava/lang/Object;)V
    .registers 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TT;)V"
        }
    .end annotation

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lgenerics/TestClassSignature;->f:Ljava/lang/Object;
    return-void
.end method


# virtual methods
.method public abstract a(Ljava/lang/Object;)Ljava/lang/Object;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(TT;)TT;"
        }
    .end annotation
.end method

.method public final hasNext()Z
    .registers 2

    .line 1
    iget-object v0, p0, Lgenerics/TestClassSignature;->f:Ljava/lang/Object;
    if-eqz v0, :cond_6
    const/4 v0, 0x1
    goto :goto_7

    :cond_6
    const/4 v0, 0x0

    :goto_7
    return v0
.end method

.method public final next()Ljava/lang/Object;
    .registers 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()TT;"
        }
    .end annotation

    invoke-virtual {p0}, Lgenerics/TestClassSignature;->hasNext()Z
    move-result v0
    if-eqz v0, :cond_1b

    :try_start_6
    iget-object v0, p0, Lgenerics/TestClassSignature;->f:Ljava/lang/Object;
    :try_end_8
    .catchall {:try_start_6 .. :try_end_8} :catchall_11

    iget-object v1, p0, Lgenerics/TestClassSignature;->f:Ljava/lang/Object;
    invoke-virtual {p0, v1}, Lgenerics/TestClassSignature;->a(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v1
    iput-object v1, p0, Lgenerics/TestClassSignature;->f:Ljava/lang/Object;
    return-object v0

    :catchall_11
    move-exception v0

    iget-object v1, p0, Lgenerics/TestClassSignature;->f:Ljava/lang/Object;
    invoke-virtual {p0, v1}, Lgenerics/TestClassSignature;->a(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v1
    iput-object v1, p0, Lgenerics/TestClassSignature;->f:Ljava/lang/Object;
    throw v0

    :cond_1b
    new-instance v0, Ljava/util/NoSuchElementException;
    invoke-direct {v0}, Ljava/util/NoSuchElementException;-><init>()V
    throw v0
.end method

.method public final remove()V
    .registers 2

    new-instance v0, Ljava/lang/UnsupportedOperationException;
    invoke-direct {v0}, Ljava/lang/UnsupportedOperationException;-><init>()V
    throw v0
.end method
