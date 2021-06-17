###### Class others.TestExplicitOverride (others.TestExplicitOverride)
.class public Lothers/TestExplicitOverride;
.super Ljava/lang/Exception;
.source "TestExplicitOverride.java"


# direct methods
.method public constructor <init>()V
    .registers 1

    .prologue
    .line 3
    invoke-direct {p0}, Ljava/lang/Exception;-><init>()V

    return-void
.end method


# virtual methods
.method public getMessage()Ljava/lang/String;
    .registers 2

    .annotation runtime Ljava/lang/Override;
    .end annotation

    .prologue
    .line 7
    invoke-super {p0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

