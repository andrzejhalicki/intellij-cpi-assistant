package com.cpiassistant.actions;

import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.Tenant
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.jetbrains.annotations.NotNull
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class AddPackageToFavoritesAction : AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
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
            val favoritePackageNode = DefaultMutableTreeNode(addedPackage)
            selectedNode.children().asIterator().forEach {
                favoritePackageNode.add(it as DefaultMutableTreeNode)
            }
            favoritesNode.add(favoritePackageNode)
        }

        val model = tree.model as DefaultTreeModel
        model.nodeStructureChanged(favoritesNode)
        //tree.expandPath(favoritesNode)
    }

}
