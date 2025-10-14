package com.cpiassistant.actions

import CustomDataProvider
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiScriptCollection
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.operations.DeploymentOperationExecutor
import com.cpiassistant.operations.DeploymentPhase
import com.cpiassistant.operations.OperationBackgroundTask
import com.cpiassistant.operations.OperationManager
import com.cpiassistant.services.NotificationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class DeployAction : AnAction() {

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
        val component = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JComponent
        val dataProvider = component?.getClientProperty("CustomDataProvider") as? CustomDataProvider
        val artifact = dataProvider?.getData("com.cpiassistant.nodes.CpiArtifact") as? CpiArtifact
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return

        if (artifact != null) {

            try {
                val tenantName = getTenantName(selectedNode)
                val artifactType = when (artifact) {
                    is CpiScriptCollection -> "ScriptCollection"
                    else -> "IntegrationFlow"
                }

                // Create deployment operation executor
                val executor = DeploymentOperationExecutor(
                    artifactId = artifact.id,
                    artifactType = artifactType,
                    service = artifact.service
                )

                // Start the operation in the manager
                val operationManager = project.service<OperationManager>()
                val taskId = operationManager.startOperation(
                    targetName = artifact.name,
                    tenantName = tenantName,
                    executor = executor
                )

                // Create and run background task to monitor progress
                val backgroundTask = OperationBackgroundTask<DeploymentPhase>(
                    project = project,
                    taskId = taskId,
                    taskTitle = "Deploying ${artifact.name}"
                )

                // Run the task in background with progress indicator
                ProgressManager.getInstance().run(backgroundTask)

            } catch (e: Exception) {
                NotificationService.getInstance()?.showError("Deployment Error", "Failed to start deployment: ${e.message}")
            }
        } else {
            NotificationService.getInstance()?.showError("Deployment Error", "No artifact selected for deployment")
        }
    }

}