.class public final Lsynchronize/TestNestedSynchronize;
.super Ljava/lang/Object;
.source "TestNestedSynchronize.java"

.method public final test()V
    .locals 2
    const/4 v0, 0
    const/4 v1, 0
    monitor-enter v0
    monitor-enter v1
    monitor-exit v1
    monitor-exit v0
    return-void
.end method
