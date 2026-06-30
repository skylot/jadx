.class Ltest/pkg/HelloWorld;
.super Ljava/lang/Object;
.source "HelloWorld.java"

.method public static main([Ljava/lang/String;)V
    .registers 2

    const-string v0, "Hello, World"
    invoke-static {p0, v0}, Ltest/pkg/HelloWorld;->hello(Ljava/lang/String;)V
    return-void
.end method

.method public static hello(Ljava/lang/String;)V
    .registers 2

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v0, p0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
