.class public Lconditions/TestBooleanToFloat;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(Lconditions/TestBooleanToFloat;)V
    .locals 0

    iget-boolean p1, p0, Lconditions/TestBooleanToFloat;->showConsent:Z

    int-to-float p1, p1

    invoke-virtual {p0, p1}, Lconditions/TestBooleanToFloat;->write(F)V

    return-void
.end method

.method public write(F)V
    .locals 0

    return-void
.end method
