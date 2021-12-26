.class public final synthetic Linner/Lambda$TestCls$1;
.super Ljava/lang/Object;

.implements Ljava/lang/Runnable;

.field final synthetic this$0:Linner/ATestCls;
.field final synthetic val$a:Z
.field final synthetic val$b:Z
.field final synthetic val$c:Z

.method constructor <init>(Linner/ATestCls;ZZZ)V
    .registers 5
    .param p1, "this$0"
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    .prologue
    .line 15
    iput-object p1, p0, Linner/Lambda$TestCls$1;->this$0:Linner/ATestCls;
    iput-boolean p2, p0, Linner/Lambda$TestCls$1;->val$a:Z
    iput-boolean p3, p0, Linner/Lambda$TestCls$1;->val$b:Z
    iput-boolean p4, p0, Linner/Lambda$TestCls$1;->val$c:Z
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public run()V
    .registers 4

    .prologue
    .line 18
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    iget-boolean v2, p0, Linner/Lambda$TestCls$1;->val$a:Z
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;
    move-result-object v1
    const-string v2, " && "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    iget-boolean v2, p0, Linner/Lambda$TestCls$1;->val$b:Z
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;
    move-result-object v1
    const-string v2, " = "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    iget-boolean v2, p0, Linner/Lambda$TestCls$1;->val$c:Z
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    .line 19
    return-void
.end method
