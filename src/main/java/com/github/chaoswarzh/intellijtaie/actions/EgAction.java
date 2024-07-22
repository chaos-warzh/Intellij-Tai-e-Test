package com.github.chaoswarzh.intellijtaie.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

public class EgAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
//        Messages.showMessageDialog("Hello tai-e!", "Greeting", Messages.getInformationIcon());
        fileOperate(e);

    }

    private void fileOperate(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            throw new TaieException("Editor is null");
        }

        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            throw new TaieException("Virtual file is null");
        }

        String filePath = virtualFile.getPath();
        // Question Icon, get user input (config path)
        // TODO:
        String configPath = Messages.showInputDialog("Please input the config path: ", "Input Analysis Config", Messages.getQuestionIcon());

        if (configPath == null || configPath.isEmpty()) {
            throw new TaieException("Invalid config path!");
        }


        Messages.showMessageDialog(filePath  + "\n" + configPath, "File Path", Messages.getInformationIcon());

    }
}

class TaieException extends RuntimeException {
    public TaieException(String message) {
        super(message);
    }
}
