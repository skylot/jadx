.class public final Ltypes/TestGenerics2;
.super Ljava/lang/Object;
.source "SourceFile"

# instance fields
.field private field:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/Integer;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.method public test()V
    .registers 5

    iget-object v4, p0, Ltypes/TestGenerics2;->field:Ljava/util/Map;

    invoke-interface {v4}, Ljava/util/Map;->size()I
    move-result v0

    invoke-static {v0}, Ltypes/TestGenerics2;->useInt(I)V

    invoke-interface {v4}, Ljava/util/Map;->entrySet()Ljava/util/Set;
    move-result-object v4

    invoke-interface {v4}, Ljava/util/Set;->iterator()Ljava/util/Iterator;
    move-result-object v4

    :goto_16
    invoke-interface {v4}, Ljava/util/Iterator;->hasNext()Z
    move-result v0

    if-eqz v0, :ret

    invoke-interface {v4}, Ljava/util/Iterator;->next()Ljava/lang/Object;
    move-result-object v0

    invoke-interface {v0}, Ljava/util/Map$Entry;->getKey()Ljava/lang/Object;
    move-result-object v1

    check-cast v1, Ljava/lang/Integer;

    invoke-virtual {v1}, Ljava/lang/Integer;->intValue()I
    move-result v1

    invoke-static {v1}, Ltypes/TestGenerics2;->useInt(I)V

    invoke-interface {v0}, Ljava/util/Map$Entry;->getValue()Ljava/lang/Object;
    move-result-object v0

    check-cast v0, Ljava/lang/String;

    invoke-interface {v0, p1}, Ljava/lang/String;->trim()Ljava/lang/String;

    goto :goto_16

    :ret
    return-void
.end method

.method public static useInt(I)V
    .registers 3
    return-void
.end method
