.class public Lconditions/TestBooleanToLong;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(Lconditions/TestBooleanToLong;)V
    .locals 0

    iget-boolean p1, p0, Lconditions/TestBooleanToLong;->showConsent:Z

    int-to-long p1, p1

    invoke-virtual {p0, p1}, Lconditions/TestBooleanToLong;->write(J)V

    return-void
.end method

.method public write(J)V
    .locals 0

    return-void
.end method
