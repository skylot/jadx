.class public LTestReservedNames;
.super Ljava/lang/Object;
.source "TestReservedNames.java"


# instance fields
.field public do:Ljava/lang/String; # reserved name
.field public 0f:Ljava/lang/String; # invalid identifier


# direct methods
.method public constructor <init>()V
    .registers 1

    .prologue
    .line 3
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public try()Ljava/lang/String;
    .registers 2

    .prologue
    .line 8
    iget-object v0, p0, LTestReservedNames;->do:Ljava/lang/String;

    return-object v0
.end method
