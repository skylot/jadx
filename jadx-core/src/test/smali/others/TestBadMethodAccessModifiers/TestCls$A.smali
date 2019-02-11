.class public abstract Lothers/TestCls$A;
.super Ljava/lang/Object;
.source "TestCls.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lothers/TestCls;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x401
    name = "A"
.end annotation


# instance fields
.field final synthetic this$0:Lothers/TestCls;


# direct methods
.method public constructor <init>(Lothers/TestCls;)V
    .registers 2
    .param p1, "this$0"    # Lothers/TestCls;

    .prologue
    .line 5
    iput-object p1, p0, Lothers/TestCls$A;->this$0:Lothers/TestCls;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public abstract test()V
.end method
