.class LHelloWorld;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 2
    sget-object p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v0, "Hello, World"
    invoke-virtual {p0, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
