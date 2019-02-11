.class public Lothers/TestCls$B;
.super Lothers/TestCls$A;
.source "TestCls.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lothers/TestCls;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = "B"
.end annotation


# instance fields
.field final synthetic this$0:Lothers/TestCls;


# direct methods
.method public constructor <init>(Lothers/TestCls;)V
    .registers 2
    .param p1, "this$0"    # Lothers/TestCls;

    .prologue
    .line 9
    iput-object p1, p0, Lothers/TestCls$B;->this$0:Lothers/TestCls;

    invoke-direct {p0, p1}, Lothers/TestCls$A;-><init>(Lothers/TestCls;)V

    return-void
.end method


# virtual methods
.method protected test()V
    .registers 1

    .prologue
    .line 11
    return-void
.end method
