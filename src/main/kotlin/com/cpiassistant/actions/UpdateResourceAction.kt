package com.cpiassistant.actions

import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiResource
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.operations.OperationBackgroundTask
import com.cpiassistant.operations.OperationManager
import com.cpiassistant.operations.ScriptUpdateOperationExecutor
import com.cpiassistant.operations.ScriptUpdatePhase
import com.cpiassistant.services.NotificationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.annotations.NotNull
import java.util.Base64
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class UpdateResourceAction : AnAction() {

    private fun getTenantName(treeNode: DefaultMutableTreeNode?): String {
        var currentNode: DefaultMutableTreeNode? = treeNode

        // Traverse up the tree to find the Tenant node
        while (currentNode != null) {
            val userObject = currentNode.userObject
            if (userObject is Tenant) {
                return userObject.name
            }
            currentNode = currentNode.parent as? DefaultMutableTreeNode
        }

        return "Unknown Tenant"
    }

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project = event.project ?: return
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val artifact = (selectedNode.parent as DefaultMutableTreeNode).userObject as CpiArtifact
        val resource = selectedNode.userObject as CpiResource

        if (resource.path.isEmpty()) {
            Messages.showInfoMessage("Resource is not mapped to any local file", resource.path)
            return
        }
        val file = LocalFileSystem.getInstance().findFileByPath(resource.path)
        if (file == null || !file.exists()) {
            Messages.showInfoMessage("File does not exist", resource.path)
            return
        }

        try {
            val fileEncoded = Base64.getEncoder().encodeToString(file.contentsToByteArray())
            val tenantName = getTenantName(selectedNode)

            // Create script update operation executor
            val executor = ScriptUpdateOperationExecutor(
                artifact = artifact,
                resourceName = resource.name,
                content = fileEncoded
            )

            // Start the operation in the manager
            val operationManager = project.service<OperationManager>()
            val taskId = operationManager.startOperation(
                targetName = resource.name,
                tenantName = tenantName,
                executor = executor
            )

            // Create and run background task to monitor progress
            val backgroundTask = OperationBackgroundTask<ScriptUpdatePhase>(
                project = project,
                taskId = taskId,
                taskTitle = "Updating ${resource.name}"
            )

            // Run the task in background with progress indicator
            ProgressManager.getInstance().run(backgroundTask)

        } catch (e: Exception) {
            NotificationService.getInstance()?.showError("Error", "Error updating resource: ${e.message}")
        }
    }

}