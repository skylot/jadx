.class public Lconditions/TestBooleanToShort;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(Lconditions/TestBooleanToShort;)V
    .locals 0

    iget-boolean p1, p0, Lconditions/TestBooleanToShort;->showConsent:Z

    int-to-short p1, p1

    invoke-virtual {p0, p1}, Lconditions/TestBooleanToShort;->write(S)V

    return-void
.end method

.method public write(S)V
    .locals 0

    return-void
.end method
