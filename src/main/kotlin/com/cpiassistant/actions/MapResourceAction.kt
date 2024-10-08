package com.cpiassistant.actions

import FileNodeInfo
import FileNodeStateComponent
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiResource
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NotNull
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class MapResourceAction : AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val artifact = (selectedNode.parent as DefaultMutableTreeNode).userObject as CpiArtifact
        val resource = selectedNode.userObject as CpiResource
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        val selectedFiles = FileChooser.chooseFiles(descriptor, event.project, null)
        val project: Project? = event.project

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

        try {
            val fileNodeStateComponent = project?.service<FileNodeStateComponent>()
            val newNodeData = FileNodeInfo(selectedFile.name, selectedFile.path, artifact.id)
            fileNodeStateComponent?.addFileNode(newNodeData)

            resource.path = selectedFile.path
            selectedNode.userObject = resource

            (tree.model as DefaultTreeModel).nodeStructureChanged(selectedNode)
            tree.updateUI()

            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Resource mapped successfully.",
                    NotificationType.INFORMATION
                )
            )
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Error",
                    "Error mapping resource: ${e.message}",
                    NotificationType.ERROR
                )
            )
        }
    }

}