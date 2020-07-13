.class public Lothers/TestConstructor;
.super Ljava/lang/Object;

.method private test(DDLSomeObject;)LSomeObject;
    .locals 22
    .param p1, "arg1"    # D
    .param p3, "arg2"    # D
    .param p5, "arg3"    # LSomeObject;

    .prologue
    .line 54

    new-instance v17, LSomeObject;

    move-object/from16 v0, v17

    move-object/from16 v1, p5

    invoke-direct {v0, v1}, LSomeObject;-><init>(LSomeObject;)V

    .line 59
    .local v17, "localSomeObject":LSomeObject;

    return-object v17
.end method
