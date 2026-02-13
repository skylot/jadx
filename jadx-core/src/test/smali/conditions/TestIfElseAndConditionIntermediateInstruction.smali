###### Class conditions.TestIfElseAndConditionIntermediateInstruction (conditions.TestIfElseAndConditionIntermediateInstruction)
.class public Lconditions/TestIfElseAndConditionIntermediateInstruction;
.super Ljava/lang/Object;
.source "TestIfElseAndConditionIntermediateInstruction.java"


# static fields
.field private static final CONST:F = 342.0f


# instance fields
.field private bool:Z

.field private num:F


# direct methods
.method public constructor <init>()V
    .registers 1

    .line 3
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private nothing1()V
    .registers 1

    .line 19
    return-void
.end method

.method private nothing2()V
    .registers 1

    .line 23
    return-void
.end method


# virtual methods
.method public function()V
    .registers 3

    .line 9
    iget-boolean v0, p0, Lconditions/TestIfElseAndConditionIntermediateInstruction;->bool:Z

    if-eqz v0, :cond_12

    iget v0, p0, Lconditions/TestIfElseAndConditionIntermediateInstruction;->num:F

    const/high16 v1, 0x3f800000    # 1.0f

    cmpg-float v1, v0, v1

    if-gez v1, :cond_12

    .line 10
    const/high16 v1, 0x43ab0000    # 342.0f

    add-float/2addr v0, v1

    iput v0, p0, Lconditions/TestIfElseAndConditionIntermediateInstruction;->num:F

    goto :goto_15

    .line 12
    :cond_12
    invoke-direct {p0}, Lconditions/TestIfElseAndConditionIntermediateInstruction;->nothing2()V

    .line 14
    :goto_15
    invoke-direct {p0}, Lconditions/TestIfElseAndConditionIntermediateInstruction;->nothing1()V

    .line 15
    return-void
.end method
