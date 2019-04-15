.class public LTestBooleanToByte;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToByte;I)V
    .locals 0

    iget-boolean p2, p0, LTestBooleanToByte;->showConsent:Z

    int-to-byte p2, p2

    invoke-virtual {p1, p2}, LTestBooleanToByte;->write(B)V

    return-void
.end method

.method public write(B)V
    .locals 0

    return-void
.end method
