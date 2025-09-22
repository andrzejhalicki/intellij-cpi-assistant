package com.cpiassistant.actions

import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.Tenant
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class RemovePackageFromFavoritesAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val cpiPackage = selectedNode.userObject as CpiPackage
        val favoritesNode = selectedNode.parent as DefaultMutableTreeNode
        var tenantNode = favoritesNode.parent as DefaultMutableTreeNode
        while (tenantNode.userObject !is Tenant) {
            tenantNode = tenantNode.parent as DefaultMutableTreeNode
        }
        val tenant = tenantNode.userObject as Tenant

        tenant.removePackageFromFavorites(cpiPackage)

        favoritesNode.remove(selectedNode)

        val model = tree.model as DefaultTreeModel
        model.nodeStructureChanged(favoritesNode)
    }

    override fun update(e: AnActionEvent) {
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JTree ?: return
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode
        val userObject = selectedNode?.userObject

        if (userObject is CpiPackage) {
            val favoritesNode = selectedNode.parent as? DefaultMutableTreeNode
            if (favoritesNode?.userObject is com.cpiassistant.nodes.Favorites) {
                e.presentation.isEnabledAndVisible = true
                return
            }
        }
        e.presentation.isEnabledAndVisible = false
    }
}
