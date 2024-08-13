package com.cpiassistant.actions

import CustomDataProvider
import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiResource
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.annotations.NotNull
import java.util.*
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class UpdateResourceAction: AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val artifact = (selectedNode.parent as DefaultMutableTreeNode).userObject as CpiArtifact
        val resource = selectedNode.userObject as CpiResource
        if(resource.path.isEmpty()) {
            Messages.showInfoMessage("Resource is not mapped to any local file",resource.path)
            return
        }
        val file = LocalFileSystem.getInstance().findFileByPath(resource.path)
        if(file?.exists() == false){
            Messages.showInfoMessage("File does not exist",resource.path)
            return
        }
        val fileEncoded = Base64.getEncoder().encodeToString(file?.contentsToByteArray())
        artifact.isLoaded = false
        artifact.updateResource(resource.name, fileEncoded) { res ->
        }
        artifact.isLoaded = true
    }

}