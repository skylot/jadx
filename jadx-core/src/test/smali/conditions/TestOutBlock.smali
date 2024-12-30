.class public Lconditions/TestOutBlock;
.super Lcom/eltechs/axs/activities/FrameworkActivity;


.method protected onCreate(Landroid/os/Bundle;)V
    .registers 9

    .line 98
    invoke-super {p0, p1}, Lcom/eltechs/axs/activities/FrameworkActivity;->onCreate(Landroid/os/Bundle;)V

    .line 101
    invoke-virtual {p0}, Lconditions/TestOutBlock;->getApplicationState()Lcom/eltechs/axs/applicationState/ApplicationStateBase;

    move-result-object p1

    .line 102
    invoke-interface {p1}, Lcom/eltechs/axs/applicationState/ApplicationStateBase;->getEnvironment()Lcom/eltechs/axs/environmentService/AXSEnvironment;

    move-result-object v0

    const-class v1, Lcom/eltechs/axs/environmentService/components/XServerComponent;

    invoke-virtual {v0, v1}, Lcom/eltechs/axs/environmentService/AXSEnvironment;->getComponent(Ljava/lang/Class;)Lcom/eltechs/axs/environmentService/EnvironmentComponent;

    move-result-object v0

    check-cast v0, Lcom/eltechs/axs/environmentService/components/XServerComponent;

    .line 103
    invoke-virtual {p0}, Lconditions/TestOutBlock;->getIntent()Landroid/content/Intent;

    move-result-object v1

    const-string v2, "facadeclass"

    invoke-virtual {v1, v2}, Landroid/content/Intent;->getSerializableExtra(Ljava/lang/String;)Ljava/io/Serializable;

    move-result-object v1

    check-cast v1, Ljava/lang/Class;

    if-eqz v1, :cond_46

    const/4 v2, 0x2

    const/4 v3, 0x0

    .line 108
    :try_start_23
    new-array v4, v2, [Ljava/lang/Class;

    const-class v5, Lcom/eltechs/axs/xserver/XServer;

    aput-object v5, v4, v3

    const-class v5, Lcom/eltechs/axs/applicationState/ApplicationStateBase;

    const/4 v6, 0x1

    aput-object v5, v4, v6

    invoke-virtual {v1, v4}, Ljava/lang/Class;->getDeclaredConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object v1

    .line 109
    new-array v2, v2, [Ljava/lang/Object;

    invoke-virtual {v0}, Lcom/eltechs/axs/environmentService/components/XServerComponent;->getXServer()Lcom/eltechs/axs/xserver/XServer;

    move-result-object v4

    aput-object v4, v2, v3

    aput-object p1, v2, v6

    invoke-virtual {v1, v2}, Ljava/lang/reflect/Constructor;->newInstance([Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lcom/eltechs/axs/xserver/ViewFacade;
    :try_end_42
    .catch Ljava/lang/Exception; {:try_start_23 .. :try_end_42} :catch_43

    goto :goto_47

    .line 112
    :catch_43
    invoke-static {v3}, Lcom/eltechs/axs/helpers/Assert;->state(Z)V

    :cond_46
    const/4 v1, 0x0

    .line 118
    :goto_47
    invoke-virtual {p0}, Lconditions/TestOutBlock;->getWindow()Landroid/view/Window;

    move-result-object v2

    const/16 v3, 0x80

    invoke-virtual {v2, v3}, Landroid/view/Window;->addFlags(I)V

    .line 120
    invoke-virtual {p0}, Lconditions/TestOutBlock;->getWindow()Landroid/view/Window;

    move-result-object v2

    const/high16 v3, 0x400000

    invoke-virtual {v2, v3}, Landroid/view/Window;->addFlags(I)V

    .line 125
    sget v2, Lcom/eltechs/axs/R$layout;->main:I

    invoke-virtual {p0, v2}, Lconditions/TestOutBlock;->setContentView(I)V

    .line 127
    invoke-direct {p0}, Lconditions/TestOutBlock;->checkForSuddenDeath()Z

    move-result v2

    if-eqz v2, :cond_65

    return-void

    .line 135
    :cond_65
    new-instance v2, Lcom/eltechs/axs/widgets/viewOfXServer/ViewOfXServer;

    invoke-virtual {v0}, Lcom/eltechs/axs/environmentService/components/XServerComponent;->getXServer()Lcom/eltechs/axs/xserver/XServer;

    move-result-object v0

    invoke-interface {p1}, Lcom/eltechs/axs/applicationState/ApplicationStateBase;->getXServerViewConfiguration()Lcom/eltechs/axs/configuration/XServerViewConfiguration;

    move-result-object p1

    invoke-direct {v2, p0, v0, v1, p1}, Lcom/eltechs/axs/widgets/viewOfXServer/ViewOfXServer;-><init>(Landroid/content/Context;Lcom/eltechs/axs/xserver/XServer;Lcom/eltechs/axs/xserver/ViewFacade;Lcom/eltechs/axs/configuration/XServerViewConfiguration;)V

    iput-object v2, p0, Lconditions/TestOutBlock;->viewOfXServer:Lcom/eltechs/axs/widgets/viewOfXServer/ViewOfXServer;

    .line 137
    iget-object p1, p0, Lconditions/TestOutBlock;->periodicIabCheckTimer:Landroid/os/CountDownTimer;

    invoke-virtual {p1}, Landroid/os/CountDownTimer;->start()Landroid/os/CountDownTimer;

    return-void
.end method
