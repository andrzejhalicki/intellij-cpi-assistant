package com.cpiassistant.actions

import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.services.NotificationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NotNull
import java.io.File
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class SyncAllResourcesAction : AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project: Project? = event.project
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val artifact = selectedNode.userObject as CpiArtifact

        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
        val selectedFolders = FileChooser.chooseFiles(descriptor, event.project, null)

        if (selectedFolders.isEmpty()) {
            NotificationService.getInstance().showInfo("No folder selected.")
            return
        }

        val selectedFolder: VirtualFile = selectedFolders[0]

        val folder = LocalFileSystem.getInstance().findFileByPath(selectedFolder.path)
        if (folder == null || !folder.exists()) {
            Messages.showInfoMessage("Folder does not exist", selectedFolder.path)
            return
        }

        var overwriteAll = false
        artifact.getResources(artifact.id) { resources ->
            resources.forEach { resource ->
                artifact.downloadResource(resource.name) { content ->
                    try {
                        val filePath = selectedFolder.path + File.separator + resource.name
                        var file = LocalFileSystem.getInstance().findFileByPath(filePath)
                        if (file == null || !file.exists()){
                            WriteCommandAction.runWriteCommandAction(project) {
                                file = selectedFolder.createChildData(this, resource.name)
                            }
                        } else {
                            if (!overwriteAll) {
                                val confirmResult = Messages.showDialog(
                                    "Are you sure you want to overwrite local file ${resource.name}?",
                                    "Confirm Overwrite",
                                    arrayOf("Yes", "Yes to All", "No"),
                                    0,
                                    Messages.getQuestionIcon()
                                )

                                when (confirmResult) {
                                    0 -> {}
                                    1 -> overwriteAll = true
                                    else -> return@downloadResource
                                }
                            }
                        }
                        WriteCommandAction.runWriteCommandAction(project) {
                            file?.setBinaryContent(content.toByteArray())
                        }
                        ApplicationManager.getApplication().invokeLater {
                            NotificationService.getInstance().showSuccess("Script ${resource.name} synced successfully.")
                        }
                    } catch (e: Exception) {
                        NotificationService.getInstance().showError("Failed to sync script ${resource.name}: ${e.message}")
                    }
                }
            }
        }
    }

}