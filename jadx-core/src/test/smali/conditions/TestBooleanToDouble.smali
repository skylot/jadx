.class public Lconditions/TestBooleanToDouble;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(Lconditions/TestBooleanToDouble;)V
    .locals 0

    iget-boolean p1, p0, Lconditions/TestBooleanToDouble;->showConsent:Z

    int-to-double p1, p1

    invoke-virtual {p0, p1}, Lconditions/TestBooleanToDouble;->write(D)V

    return-void
.end method

.method public write(D)V
    .locals 0

    return-void
.end method
