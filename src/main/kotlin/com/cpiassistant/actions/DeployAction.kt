package com.cpiassistant.actions

import CustomDataProvider
import com.cpiassistant.deployment.DeploymentBackgroundTask
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiScriptCollection
import com.cpiassistant.nodes.Tenant
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
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

                // Create and run background task
                val backgroundTask = DeploymentBackgroundTask(
                    project = project,
                    artifactId = artifact.id,
                    artifactName = artifact.name,
                    artifactType = when (artifact) {
                        is CpiScriptCollection -> "ScriptCollection"
                        else -> "IntegrationFlow"
                    },
                    tenantName = tenantName,
                    service = artifact.service
                )

                // Run the task in background with progress indicator
                ProgressManager.getInstance().run(backgroundTask)

            } catch (e: Exception) {
                Notifications.Bus.notify(
                    Notification(
                        "Custom Notification Group",
                        "Deployment Error",
                        "Failed to start deployment: ${e.message}",
                        NotificationType.ERROR
                    )
                )
            }
        } else {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Deployment Error",
                    "No artifact selected for deployment",
                    NotificationType.ERROR
                )
            )
        }
    }

}