.class public LTestDuplicatedNames;
.super Ljava/lang/Object;
.source "TestDuplicatedNames.java"


# instance fields
.field public fieldName:Ljava/lang/String;
.field public fieldName:Ljava/lang/Object;


# direct methods
.method public constructor <init>()V
    .registers 1

    .prologue
    .line 3
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public run()Ljava/lang/String;
    .registers 2

    .prologue
    iget-object v0, p0, LTestDuplicatedNames;->fieldName:Ljava/lang/String;

    return-object v0
.end method

.method public run()Ljava/lang/Object;
    .registers 2

    .prologue
    iget-object v0, p0, LTestDuplicatedNames;->fieldName:Ljava/lang/Object;

    return-object v0
.end method
