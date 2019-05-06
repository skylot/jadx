.class public LTestBooleanToByte;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToByte;)V
    .locals 0

    iget-boolean p1, p0, LTestBooleanToByte;->showConsent:Z

    int-to-byte p1, p1

    invoke-virtual {p0, p1}, LTestBooleanToByte;->write(B)V

    return-void
.end method

.method public write(B)V
    .locals 0

    return-void
.end method
