package com.cpiassistant.actions

import TenantInfo
import TenantStateComponent
import com.cpiassistant.nodes.Tenant
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import org.jetbrains.annotations.NotNull
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


class DeleteTenantAction : AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val tenant = selectedNode.userObject as Tenant

        val confirmResult = Messages.showYesNoDialog(
            "Are you sure you want to delete the tenant '${tenant.name}'?",
            "Confirm Deletion",
            Messages.getQuestionIcon()
        )

        if (confirmResult != Messages.YES) {
            return
        }

        try {
            val tenantStateComponent = service<TenantStateComponent>()
            val tenantInfo = TenantInfo(tenant.name, tenant.service.url, tenant.service.tokenUrl, "", "")
            tenantStateComponent.deleteTenant(tenantInfo)

            val treeModel = tree.model as? DefaultTreeModel ?: return

            treeModel.removeNodeFromParent(selectedNode)
            treeModel.reload()

            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Tenant '${tenant.name}' has been deleted successfully.",
                    NotificationType.INFORMATION
                )
            )
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Error",
                    "Error deleting tenant: ${e.message}",
                    NotificationType.ERROR
                )
            )
        }

    }
}