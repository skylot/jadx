.class public LTestBooleanToFloat;
.super Ljava/lang/Object;

.field private showConsent:Z

.method public writeToParcel(LTestBooleanToFloat;)V
    .locals 0

    iget-boolean p1, p0, LTestBooleanToFloat;->showConsent:Z

    int-to-float p1, p1

    invoke-virtual {p0, p1}, LTestBooleanToFloat;->write(F)V

    return-void
.end method

.method public write(F)V
    .locals 0

    return-void
.end method
