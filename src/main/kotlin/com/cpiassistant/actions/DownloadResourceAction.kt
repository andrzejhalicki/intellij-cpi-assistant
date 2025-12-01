package com.cpiassistant.actions

import FileNodeInfo
import FileNodeStateComponent
import com.cpiassistant.nodes.artifact.CpiArtifact
import com.cpiassistant.nodes.resource.CpiResource
import com.cpiassistant.services.NotificationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NotNull
import java.io.File
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class DownloadResourceAction : AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project: Project? = event.project
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val artifact = (selectedNode.parent as DefaultMutableTreeNode).userObject as CpiArtifact
        val resource = selectedNode.userObject as CpiResource

        downloadResource(resource, artifact, selectedNode, tree, project)

    }

    fun downloadResource(resource: CpiResource, artifact: CpiArtifact, selectedNode: DefaultMutableTreeNode, tree: JTree, project: Project?) {
        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withTitle("Select Directory to Download Script")
            .withDescription("Choose where to save ${resource.name}")

        val selectedFolders = FileChooser.chooseFiles(descriptor, project, null)

        if (selectedFolders.isEmpty()) {
            NotificationService.getInstance()?.showInfo("No folder selected.")
            return
        }

        val selectedFolder: VirtualFile = selectedFolders[0]

        val folder = LocalFileSystem.getInstance().findFileByPath(selectedFolder.path)
        if (folder == null || !folder.exists()) {
            Messages.showInfoMessage("Folder does not exist", selectedFolder.path)
            return
        }

        val filePath = selectedFolder.path + File.separator + resource.name
        var file = LocalFileSystem.getInstance().findFileByPath(filePath)

        if (file != null && file.exists()) {
            val confirmResult = Messages.showYesNoDialog(
                project,
                "File ${resource.name} already exists in this directory. Do you want to overwrite it?",
                "Confirm Overwrite",
                Messages.getQuestionIcon()
            )

            if (confirmResult != Messages.YES) {
                return
            }
        }

        artifact.downloadResource(resource) { content ->
            try {
                WriteCommandAction.runWriteCommandAction(project) {
                    file = selectedFolder.createChildData(this, resource.name)
                }

                LocalFileSystem.getInstance().refresh(false)

                file?.let { file ->
                    WriteCommandAction.runWriteCommandAction(project) {
                        file.setBinaryContent(content.toByteArray())
                    }
                    resource.path = file.path
                    selectedNode.userObject = resource

                    val fileNodeStateComponent = project?.service<FileNodeStateComponent>()
                    val newNodeData = FileNodeInfo(file.name, file.path, artifact.id)
                    fileNodeStateComponent?.addFileNode(newNodeData)

                    (tree.model as DefaultTreeModel).nodeStructureChanged(selectedNode)
                    tree.updateUI()

                    NotificationService.getInstance()?.showSuccess(
                        "Resource ${resource.name} downloaded and mapped successfully to ${selectedFolder.path}"
                    )

                    project?.let { FileEditorManager.getInstance(it) }?.openFile(file, true)
                } ?: run {
                    NotificationService.getInstance()?.showError(
                        "Error",
                        "Failed to create or access the file ${resource.name}."
                    )
                }
            } catch (e: Exception) {
                NotificationService.getInstance()?.showError(
                    "Error",
                    "Error downloading and mapping resource: ${e.message}"
                )
            }
        }
    }
}
