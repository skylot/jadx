.class public Lconditions/TestBooleanToByte;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(Lconditions/TestBooleanToByte;)V
    .locals 0

    iget-boolean p1, p0, Lconditions/TestBooleanToByte;->showConsent:Z

    int-to-byte p1, p1

    invoke-virtual {p0, p1}, Lconditions/TestBooleanToByte;->write(B)V

    return-void
.end method

.method public write(B)V
    .locals 0

    return-void
.end method
