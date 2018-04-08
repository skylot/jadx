.class public LTestVariablesNames;
.super Ljava/lang/Object;
.source "TestVariablesNames.java"

.method public test(Ljava/lang/String;I)V
    .registers 10

    .prologue
    .line 17
    invoke-direct {p0, p1}, LTestVariablesNames;->f1(Ljava/lang/String;)V

    .line 18
    add-int/lit8 p1, p2, 0x3

    .line 19
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "i"

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    .line 20
    invoke-direct {p0, p1, v3}, LTestVariablesNames;->f2(ILjava/lang/String;)V

    .line 21
    mul-int/lit8 v5, p1, 0x5

    int-to-double p1, v5

    .line 22
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "d"

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5, p1, v1}, Ljava/lang/StringBuilder;->append(D)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    .line 23
    invoke-direct {p0, p1, v1, v4}, LTestVariablesNames;->f3(DLjava/lang/String;)V

    .line 24
    return-void
.end method


.method public constructor <init>()V
    .registers 1
    .prologue
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method private f1(Ljava/lang/String;)V
    .registers 2
    .prologue
    return-void
.end method

.method private f2(ILjava/lang/String;)V
    .registers 3
    .prologue
    return-void
.end method

.method private f3(DLjava/lang/String;)V
    .registers 4
    .prologue
    return-void
.end method
