package com.cpiassistant.actions

import CustomDataProvider
import FileNodeInfo
import FileNodeStateComponent
import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiResource
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NotNull
import java.util.*
import javax.swing.JTree
import javax.swing.tree.*

class AddResourceAction : AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project = event.project
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val dataProvider = tree.getClientProperty("CustomDataProvider") as? CustomDataProvider
        val artifact = dataProvider?.getData("com.cpiassistant.nodes.CpiArtifact") as? CpiArtifact
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return

        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        val selectedFiles = FileChooser.chooseFiles(descriptor, project, null)

        if (selectedFiles.isEmpty()) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "No file selected.",
                    NotificationType.INFORMATION
                )
            )
            return
        }

        val selectedFile: VirtualFile = selectedFiles[0]
        val newResource = CpiResource(selectedFile.name, selectedFile.name, selectedFile.path, artifact!!.id)
        try {
            val fileEncoded = Base64.getEncoder().encodeToString(selectedFile.contentsToByteArray())
            artifact.addResource(newResource.id, fileEncoded) { res ->
                if (!res) {
                    Notifications.Bus.notify(
                        Notification(
                            "Custom Notification Group",
                            "Error",
                            "Failed to add resource to the artifact.",
                            NotificationType.ERROR
                        )
                    )
                    return@addResource
                }

                ApplicationManager.getApplication().invokeLater {
                    updateTreeWithNewResource(tree, selectedNode, selectionPath, newResource, artifact)
                    saveToStorage(newResource, selectedNode)
                }
            }
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Error",
                    "Error processing file: ${e.message}",
                    NotificationType.ERROR
                )
            )
        }
    }

    private fun updateTreeWithNewResource(
        tree: JTree,
        selectedNode: DefaultMutableTreeNode,
        selectionPath: TreePath,
        newResource: CpiResource,
        artifact: CpiArtifact
    ) {
        val newNode = DefaultMutableTreeNode(
            CpiResource(
                newResource.name,
                newResource.name,
                newResource.path,
                artifact.id,
                true
            )
        )

        selectedNode.add(newNode)

        val model = tree.model as DefaultTreeModel
        model.nodeStructureChanged(selectedNode)

        tree.expandPath(selectionPath)
        val newNodePath = selectionPath.pathByAddingChild(newNode)
        tree.scrollPathToVisible(newNodePath)
        tree.startEditingAtPath(newNodePath)
    }

    private fun saveToStorage(newResource: CpiResource, selectedNode: DefaultMutableTreeNode) {
        val newNodeData = FileNodeInfo(newResource.name, newResource.path, (selectedNode.userObject as BaseNode).id)
        val fileNodeStateComponent = service<FileNodeStateComponent>()
        fileNodeStateComponent.addFileNode(newNodeData)
    }

}