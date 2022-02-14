.class synthetic Linner/C;
.super Ljava/lang/Object;

.implements Ljava/lang/Runnable;

.field final synthetic this$1:Linner/B;

.method constructor <init>(Linner/B;)V
    .registers 2
    .prologue
    iput-object p1, p0, Linner/C;->this$1:Linner/B;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public run()V
    .registers 3
    .prologue
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    const-string v1, "run"
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    return-void
.end method
