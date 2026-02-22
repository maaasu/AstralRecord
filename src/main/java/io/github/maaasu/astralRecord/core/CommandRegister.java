package io.github.maaasu.astralRecord.core;

import io.github.maaasu.astralRecord.AstralRecord;

public class CommandRegister {
    private final AstralRecord instance;

    public CommandRegister(AstralRecord plugin) {
        this.instance = plugin;
        registerCommand();
    }

    public void registerCommand() {

    }

    private AstralRecord getInstance() {
        return instance;
    }
}
