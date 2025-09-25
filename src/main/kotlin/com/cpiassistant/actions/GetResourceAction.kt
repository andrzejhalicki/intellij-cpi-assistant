package com.cpiassistant.actions

import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiResource
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.annotations.NotNull
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class GetResourceAction : AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project: Project? = event.project
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val artifact = (selectedNode.parent as DefaultMutableTreeNode).userObject as CpiArtifact
        val resource = selectedNode.userObject as CpiResource

        if (resource.path.isEmpty()) {
            val mapResourceAction: AnAction =
                ActionManager.getInstance().getAction("com.cpiassistant.actions.MapResourceAction")
            ActionManager.getInstance().tryToExecute(mapResourceAction, event.inputEvent,null,event.place,true)
        } else {
            val confirmResult = Messages.showYesNoDialog(
                "Are you sure you want to overwrite local file?",
                "Confirm Overwrite",
                Messages.getQuestionIcon()
            )

            if (confirmResult != Messages.YES) {
                return
            }
        }

        artifact.downloadResource(resource.name) { content ->
            val file = LocalFileSystem.getInstance().findFileByPath(resource.path)
            if (file == null || !file.exists()) {
                Messages.showInfoMessage("File does not exist", resource.path)
                return@downloadResource
            }
            WriteCommandAction.runWriteCommandAction(project) {
                file.setBinaryContent(content.toByteArray())
            }
            if (project != null) {
                FileEditorManager.getInstance(project).openFile(file, true)
            }
        }

    }

}