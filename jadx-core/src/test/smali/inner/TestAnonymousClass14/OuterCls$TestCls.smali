.class Linner/OuterCls$TestCls;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Linner/OuterCls;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = "TestCls"
.end annotation

.field final synthetic this$0:Linner/OuterCls;


# direct methods
.method private constructor <init>(Linner/OuterCls;)V
    .locals 0

    iput-object p1, p0, Linner/OuterCls$TestCls;->this$0:Linner/OuterCls;

    new-instance p1, Ljava/util/ArrayList;

    invoke-direct {p1}, Ljava/util/ArrayList;-><init>()V

    return-void
.end method

.method synthetic constructor <init>(Linner/OuterCls;Linner/OuterCls$1;)V
    .locals 0

    invoke-direct {p0, p1}, Linner/OuterCls$TestCls;-><init>(Linner/OuterCls;)V

    return-void
.end method

