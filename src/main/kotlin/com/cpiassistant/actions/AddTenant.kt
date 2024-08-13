package com.cpiassistant.actions

import AddTenantDialog
import TenantInfo
import TenantStateComponent
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.services.CpiService
import com.cpiassistant.services.TreeService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.annotations.NotNull
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


class AddTenant: AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val dialog = AddTenantDialog()
        if (dialog.showAndGet()) {
            val tenantStateComponent = service<TenantStateComponent>()
            val tenantInfo =
                TenantInfo(dialog.getName(), dialog.getURL(), dialog.getTokenUrl(), dialog.getClientId(), dialog.getClientSecret())
            tenantStateComponent.addTenant(tenantInfo)
            val project: Project = event.project ?: return
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CPI Assistant") ?: return
            val treeService = toolWindow.project.service<TreeService>()
            val model = treeService.tree.model as? DefaultTreeModel ?: return
            val root = model.root as DefaultMutableTreeNode
            val newTenant = Tenant(dialog.getName(),dialog.getName(),
                CpiService(dialog.getClientId(), dialog.getClientSecret(), dialog.getURL(), dialog.getTokenUrl())
            )
            val isAuthenticated = newTenant.service.authenticate();
            newTenant.isConnected = isAuthenticated
            val newTenantNode = DefaultMutableTreeNode(newTenant)
            model.insertNodeInto(newTenantNode, root, root.childCount)
            model.reload(root)
            ApplicationManager.getApplication().executeOnPooledThread(object : Runnable {
                override fun run() {
                    treeService.updateTree(newTenantNode)
                }
            })
        }
    }
}