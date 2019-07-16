.class public final Lconditions/TestTernary4;
.super Ljava/lang/Object;

.field private defaultValuesByPath:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Object;",
            ">;"
        }
    .end annotation
.end field

.field private valuesByPath:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Object;",
            ">;"
        }
    .end annotation
.end field

.method private test(Ljava/util/HashMap;)Ljava/util/Set;
    .registers 10
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/HashMap<",
            "Ljava/lang/String;",
            "Ljava/lang/Object;",
            ">;)",
            "Ljava/util/Set;"
        }
    .end annotation

    .line 278
    new-instance v0, Ljava/util/HashSet;

    invoke-direct {v0}, Ljava/util/HashSet;-><init>()V

    .line 280
    iget-object v1, p0, Lconditions/TestTernary4;->defaultValuesByPath:Ljava/util/Map;

    monitor-enter v1

    .line 281
    :try_start_14
    iget-object v3, p0, Lconditions/TestTernary4;->defaultValuesByPath:Ljava/util/Map;

    invoke-interface {v3}, Ljava/util/Map;->keySet()Ljava/util/Set;

    move-result-object v3

    invoke-interface {v3}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v3

    :cond_1e
    :goto_1e
    invoke-interface {v3}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_4c

    invoke-interface {v3}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    .line 286
    invoke-virtual {p1, v4}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    const/4 v6, 0x1

    if-eqz v5, :cond_3b

    .line 287
    invoke-direct {p0, v4}, Lconditions/TestTernary4;->getValueObject(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v7

    invoke-virtual {v7, v5}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v5

    xor-int/2addr v5, v6

    goto :goto_46

    .line 289
    :cond_3b
    iget-object v5, p0, Lconditions/TestTernary4;->valuesByPath:Ljava/util/Map;

    invoke-interface {v5, v4}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    if-eqz v5, :cond_45

    const/4 v5, 0x1

    goto :goto_46

    :cond_45
    const/4 v5, 0x0

    :goto_46
    if-eqz v5, :cond_1e

    .line 293
    invoke-interface {v0, v4}, Ljava/util/Set;->add(Ljava/lang/Object;)Z

    goto :goto_1e

    .line 296
    :cond_4c
    monitor-exit v1

    return-object v0

    :catchall_4e
    move-exception p1

    monitor-exit v1
    :try_end_50
    .catchall {:try_start_14 .. :try_end_50} :catchall_4e

    throw p1

    return-void
.end method

.method private getValueObject(Ljava/lang/String;)Ljava/lang/Object;
    .locals 1
    const/4 v0, 0x0
    return-object v0
.end method
