.class public Lconditions/TestBooleanToChar;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(Lconditions/TestBooleanToChar;)V
    .locals 0

    iget-boolean p1, p0, Lconditions/TestBooleanToChar;->showConsent:Z

    int-to-char p1, p1

    invoke-virtual {p0, p1}, Lconditions/TestBooleanToChar;->write(C)V

    return-void
.end method

.method public write(C)V
    .locals 0

    return-void
.end method
