.class public Lothers/TestConstructorBranched3;
.super Ljava/lang/Object;

.method public static final test(Ljava/lang/Class;)Lml/f;
    .registers 5
    const/4 v0, 0x0
    :goto_1
    invoke-virtual {p0}, Ljava/lang/Class;->isArray()Z
    move-result v1
    if-eqz v1, :cond_13
    add-int/lit8 v0, v0, 0x1
    invoke-virtual {p0}, Ljava/lang/Class;->getComponentType()Ljava/lang/Class;
    move-result-object p0
    const-string v1, "currentClass.componentType"
    invoke-static {p0, v1}, Lve/e;->l(Ljava/lang/Object;Ljava/lang/String;)V
    goto :goto_1

    :cond_13
    invoke-virtual {p0}, Ljava/lang/Class;->isPrimitive()Z
    move-result v1
    if-eqz v1, :cond_68
    sget-object v1, Ljava/lang/Void;->TYPE:Ljava/lang/Class;
    invoke-static {p0, v1}, Lve/e;->g(Ljava/lang/Object;Ljava/lang/Object;)Z
    move-result v1

    if-eqz v1, :cond_31
    new-instance p0, Lml/f;
    sget-object v1, Lgk/k$a;->e:Lhl/c;
    invoke-virtual {v1}, Lhl/c;->i()Lhl/b;
    move-result-object v1
    invoke-static {v1}, Lhl/a;->l(Lhl/b;)Lhl/a;
    move-result-object v1
    invoke-direct {p0, v1, v0}, Lml/f;-><init>(Lhl/a;I)V
    return-object p0

    :cond_31
    invoke-virtual {p0}, Ljava/lang/Class;->getName()Ljava/lang/String;
    move-result-object p0
    invoke-static {p0}, Lpl/b;->c(Ljava/lang/String;)Lpl/b;
    move-result-object p0
    invoke-virtual {p0}, Lpl/b;->r()Lgk/i;
    move-result-object p0
    const-string v1, "get(currentClass.name).primitiveType"
    invoke-static {p0, v1}, Lve/e;->l(Ljava/lang/Object;Ljava/lang/String;)V
    new-instance v1, Lml/f;
    if-lez v0, :cond_58
    .line 1
    iget-object p0, p0, Lgk/i;->d:Ljj/d;
    invoke-interface {p0}, Ljj/d;->getValue()Ljava/lang/Object;
    move-result-object p0
    check-cast p0, Lhl/b;
    .line 2
    invoke-static {p0}, Lhl/a;->l(Lhl/b;)Lhl/a;
    move-result-object p0
    add-int/lit8 v0, v0, -0x1
    invoke-direct {v1, p0, v0}, Lml/f;-><init>(Lhl/a;I)V
    return-object v1

    .line 3
    :cond_58
    iget-object p0, p0, Lgk/i;->c:Ljj/d;
    invoke-interface {p0}, Ljj/d;->getValue()Ljava/lang/Object;
    move-result-object p0
    check-cast p0, Lhl/b;
    .line 4
    invoke-static {p0}, Lhl/a;->l(Lhl/b;)Lhl/a;
    move-result-object p0
    invoke-direct {v1, p0, v0}, Lml/f;-><init>(Lhl/a;I)V
    return-object v1

    :cond_68
    invoke-static {p0}, Lpk/b;->b(Ljava/lang/Class;)Lhl/a;
    move-result-object p0
    sget-object v1, Lik/c;->a:Lik/c;
    invoke-virtual {p0}, Lhl/a;->b()Lhl/b;
    move-result-object v2
    const-string v3, "javaClassId.asSingleFqName()"
    invoke-static {v2, v3}, Lve/e;->l(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {v1, v2}, Lik/c;->f(Lhl/b;)Lhl/a;
    move-result-object v1
    if-nez v1, :cond_7e
    goto :goto_7f

    :cond_7e
    move-object p0, v1

    :goto_7f
    new-instance v1, Lml/f;
    invoke-direct {v1, p0, v0}, Lml/f;-><init>(Lhl/a;I)V
    return-object v1
.end method
