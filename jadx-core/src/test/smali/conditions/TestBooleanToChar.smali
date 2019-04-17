.class public LTestBooleanToChar;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToChar;I)V
    .locals 0

    iget-boolean p2, p0, LTestBooleanToChar;->showConsent:Z

    int-to-char p2, p2

    invoke-virtual {p1, p2}, LTestBooleanToChar;->write(C)V

    return-void
.end method

.method public write(C)V
    .locals 0

    return-void
.end method
