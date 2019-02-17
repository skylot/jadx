.class public final Lconditions/TestComplexIf;
.super Ljava/lang/Object;


# instance fields
.field private a:Ljava/lang/String;

.field private b:I

.field private c:F


# direct methods
.method public constructor <init>()V
    .locals 1
    return-void
.end method

.method public final test()Z
    .locals 5

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v1, "GT-P6200"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    const/4 v1, 0x1

    if-nez v0, :cond_b

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v2, "GT-P6210"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_b

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v2, "A100"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_b

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v2, "A101"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_b

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v2, "LIFETAB_S786X"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_0

    goto/16 :goto_2

    :cond_0
    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v2, "VS890 4G"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_1

    return v1

    :cond_1
    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v2, "SM-T810"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    const/4 v2, 0x0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T813"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T815"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T815N0"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T815Y"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T820"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T825"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-P585"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-P585N0"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T561"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T567V"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T320"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T321"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T325"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T700"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T705"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T705M"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T705Y"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SC-03G"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "GT-N5100"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "GT-N5105"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "GT-N5110"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "GT-N5120"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SHW-M500W"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T310"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T311"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T315"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T330"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T330NU"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T331"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T335"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T337V"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T710"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T715"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T715N0"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SM-T719"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "GT-P6800"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "SC-01E"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_2

    goto/16 :goto_1

    :cond_2
    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "LG-V500"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "LG-V930"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_3

    goto/16 :goto_1

    :cond_3
    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "P01T_1"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "P01MA"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "Nexus 9"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "ASUS_P00I"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_4

    goto :goto_1

    :cond_4
    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "Lenovo YT3-X90X"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_a

    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "Lenovo YT-X703F"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_5

    goto :goto_1

    :cond_5
    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "PMT3408_4G"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_6

    return v2

    :cond_6
    iget-object v0, p0, Lconditions/TestComplexIf;->a:Ljava/lang/String;

    const-string v3, "MediaPad T2 10.0 Pro"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_7

    return v2

    :cond_7
    iget-object v0, p0, Lconditions/TestComplexIf;->b:I

    and-int/lit8 v0, v0, 0xf

    const/4 v3, 0x4

    if-ne v0, v3, :cond_8

    const/4 v0, 0x1

    goto :goto_0

    :cond_8
    const/4 v0, 0x0

    :goto_0
    iget v3, p0, Lconditions/TestComplexIf;->c:F

    const/high16 v4, 0x43200000    # 160.0f

    cmpl-float v3, v3, v4

    if-lez v3, :cond_9

    return v1

    :cond_9
    iget v3, p0, Lconditions/TestComplexIf;->c:F

    const/4 v4, 0x0

    cmpg-float v3, v3, v4

    if-gtz v3, :cond_a

    if-eqz v0, :cond_a

    return v1

    :cond_a
    :goto_1
    return v2

    :cond_b
    :goto_2
    return v1
.end method
