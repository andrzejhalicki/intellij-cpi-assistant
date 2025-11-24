package com.cpiassistant.actions

import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.Tenant
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class AddPackageToFavoritesAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val cpiPackage = selectedNode.userObject as CpiPackage
        val tenantNode = selectedNode.parent as DefaultMutableTreeNode
        val favoritesNode = tenantNode.children().toList().find {
            ((it as DefaultMutableTreeNode).userObject as BaseNode).id == "Favorites"
        } as DefaultMutableTreeNode
        val tenant = tenantNode.userObject as Tenant

        tenant.addPackageToFavorites(cpiPackage) { addedPackage ->
            val addedPackageNode = DefaultMutableTreeNode(addedPackage)
            addedPackage.isLoaded = false
            favoritesNode.add(addedPackageNode)
        }

        val model = tree.model as DefaultTreeModel
        model.nodeStructureChanged(favoritesNode)
    }

    override fun update(e: AnActionEvent) {
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JTree ?: return
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode
        val userObject = selectedNode?.userObject

        if (userObject is CpiPackage) {
            val tenantNode = selectedNode.parent as? DefaultMutableTreeNode
            val tenant = tenantNode?.userObject as? Tenant
            if (tenant != null) {
                e.presentation.isEnabledAndVisible = !tenant.isFavorite(userObject)
            } else {
                e.presentation.isEnabledAndVisible = false
            }
        } else {
            e.presentation.isEnabledAndVisible = false
        }
    }
}