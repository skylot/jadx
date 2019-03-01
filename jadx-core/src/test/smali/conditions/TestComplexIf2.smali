.class public final Lconditions/TestComplexIf2;
.super Ljava/lang/ClassLoader;


# instance fields
.field private isSaved:Z

.field private project:Ljava/lang/String;


.method public test()V
    .locals 4

    .line 415
    iget-boolean v0, p0, Lconditions/TestComplexIf2;->isSaved:Z

    if-eqz v0, :cond_0

    .line 416
    new-instance v0, Ljava/lang/RuntimeException;

    const-string v1, "Error"

    invoke-direct {v0, v1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 418
    :cond_0
    invoke-static {}, Lorg/apache/tools/ant/util/LoaderUtils;->isContextLoaderAvailable()Z

    move-result v0

    if-eqz v0, :cond_2

    .line 419
    invoke-static {}, Lorg/apache/tools/ant/util/LoaderUtils;->getContextClassLoader()Ljava/lang/ClassLoader;

    move-result-object v0

    iput-object v0, p0, Lconditions/TestComplexIf2;->savedContextLoader:Ljava/lang/ClassLoader;

    .line 420
    move-object v0, p0

    .line 421
    .local v0, "loader":Ljava/lang/ClassLoader;
    iget-object v1, p0, Lconditions/TestComplexIf2;->project:Ljava/lang/String;

    if-eqz v1, :cond_1

    const-string v1, "simple"

    iget-object v2, p0, Lconditions/TestComplexIf2;->project:Ljava/lang/String;

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1

    .line 423
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;

    move-result-object v0

    .line 425
    :cond_1
    invoke-static {v0}, Lorg/apache/tools/ant/util/LoaderUtils;->setContextClassLoader(Ljava/lang/ClassLoader;)V

    .line 426
    const/4 v1, 0x1

    iput-boolean v1, p0, Lconditions/TestComplexIf2;->isSaved:Z

    .line 428
    .end local v0    # "loader":Ljava/lang/ClassLoader;
    :cond_2
    return-void
.end method

