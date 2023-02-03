.class public Linvoke/TestPolymorphicInvoke;
.super Ljava/lang/Object;

.method public func(II)Ljava/lang/String;
    .registers 4
    .param p1, "a"    # I
    .param p2, "c"    # I

    .line 23
    add-int v0, p1, p2
    invoke-static {v0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method

.method public test()V
    .registers 7

    .line 32
    :try_start_0
    invoke-static {}, Ljava/lang/invoke/MethodHandles;->lookup()Ljava/lang/invoke/MethodHandles$Lookup;
    move-result-object v0

    .line 33
    .local v0, "lookup":Ljava/lang/invoke/MethodHandles$Lookup;
    const-class v1, Ljava/lang/String;
    sget-object v2, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;
    const/4 v3, 0x1
    new-array v3, v3, [Ljava/lang/Class;
    const/4 v4, 0x0
    sget-object v5, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;
    aput-object v5, v3, v4
    invoke-static {v1, v2, v3}, Ljava/lang/invoke/MethodType;->methodType(Ljava/lang/Class;Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;
    move-result-object v1

    .line 34
    .local v1, "methodType":Ljava/lang/invoke/MethodType;
    const-class v2, Linvoke/TestPolymorphicInvoke;
    const-string v3, "func"
    invoke-virtual {v0, v2, v3, v1}, Ljava/lang/invoke/MethodHandles$Lookup;->findVirtual(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;
    move-result-object v2

    .line 35
    .local v2, "methodHandle":Ljava/lang/invoke/MethodHandle;
    const/16 v3, 0xa
    const/16 v4, 0x14

    invoke-polymorphic {v2, p0, v3, v4}, Ljava/lang/invoke/MethodHandle;->invoke([Ljava/lang/Object;)Ljava/lang/Object;, (Linvoke/TestPolymorphicInvoke;II)Ljava/lang/String;
	move-result-object v3

    .line 36
    .local v3, "ret":Ljava/lang/String;
    sget-object v4, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v4, v3}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    :try_end_2a
    .catchall {:try_start_0 .. :try_end_2a} :catchall_2b

    .line 39
    .end local v0    # "lookup":Ljava/lang/invoke/MethodHandles$Lookup;
    .end local v1    # "methodType":Ljava/lang/invoke/MethodType;
    .end local v2    # "methodHandle":Ljava/lang/invoke/MethodHandle;
    .end local v3    # "ret":Ljava/lang/String;
    goto :goto_2f

    .line 37
    :catchall_2b
    move-exception v0

    .line 38
    .local v0, "e":Ljava/lang/Throwable;
    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    .line 40
    .end local v0    # "e":Ljava/lang/Throwable;
    :goto_2f
    return-void
.end method
