package com.cpiassistant.actions

import AddTenantDialog
import TenantInfo
import TenantStateComponent
import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.toolWindow.ToolWindowFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
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
        val tenantStateComponent = service<TenantStateComponent>()
        val tenantInfo = TenantInfo(tenant.name, tenant.service.url, tenant.service.tokenUrl, "", "")
        tenantStateComponent.deleteTenant(tenantInfo)
        val treeModel = tree.model as? DefaultTreeModel ?: return
        treeModel.removeNodeFromParent(selectedNode)
        treeModel.reload()
    }
}