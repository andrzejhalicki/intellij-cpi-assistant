package com.cpiassistant.actions

import AddTenantDialog
import TenantInfo
import TenantStateComponent
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.services.CpiService
import com.cpiassistant.services.TreeService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.annotations.NotNull
import java.awt.Dimension
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


class AddTenant : AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val dialog = AddTenantDialog()
        dialog.setSize(300, 300)
        if (dialog.showAndGet()) {
            val project: Project = event.project ?: return
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CPI Assistant") ?: return
            val treeService = toolWindow.project.service<TreeService>()
            val model = treeService.tree.model as? DefaultTreeModel ?: return
            val root = model.root as DefaultMutableTreeNode
            try {
                val tenantInfo = TenantInfo(
                    dialog.getName(),
                    dialog.getURL(),
                    dialog.getTokenUrl(),
                    dialog.getClientId(),
                    dialog.getClientSecret()
                )
                val tenantStateComponent = project?.service<TenantStateComponent>()
                tenantStateComponent?.addTenant(tenantInfo)

                val newTenant = Tenant(
                    dialog.getName(), dialog.getName(),
                    CpiService(dialog.getClientId(), dialog.getClientSecret(), dialog.getURL(), dialog.getTokenUrl())
                )

                val isAuthenticated = newTenant.service.authenticate()
                newTenant.isConnected = isAuthenticated

                if (!isAuthenticated) {
                    Notifications.Bus.notify(
                        Notification(
                            "Custom Notification Group",
                            "Warning",
                            "Tenant added but authentication failed. Please check your credentials.",
                            NotificationType.WARNING
                        )
                    )
                }

                val newTenantNode = DefaultMutableTreeNode(newTenant)
                model.insertNodeInto(newTenantNode, root, root.childCount)
                model.reload(root)

                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        treeService.updateTree(newTenantNode)
                    } catch (e: Exception) {
                        ApplicationManager.getApplication().invokeLater {
                            Notifications.Bus.notify(
                                Notification(
                                    "Custom Notification Group",
                                    "Error",
                                    "Error updating tree: ${e.message}",
                                    NotificationType.ERROR
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Notifications.Bus.notify(
                    Notification(
                        "Custom Notification Group",
                        "Error",
                        "Error updating tenant",
                        NotificationType.ERROR
                    )
                )
            }
        }
    }
}