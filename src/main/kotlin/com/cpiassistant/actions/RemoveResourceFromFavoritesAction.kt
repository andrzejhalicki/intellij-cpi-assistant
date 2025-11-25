package com.cpiassistant.actions

import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.Favorites
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.nodes.artifact.CpiArtifact
import com.cpiassistant.nodes.resource.CpiResource
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class RemoveResourceFromFavoritesAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val resource = selectedNode.userObject as CpiResource
        val artifactNode = selectedNode.parent as DefaultMutableTreeNode
        val artifact = artifactNode.userObject as CpiArtifact
        val packageNode = artifactNode.parent as DefaultMutableTreeNode
        val cpiPackage = packageNode.userObject as CpiPackage
        val favoritesNode = packageNode.parent as DefaultMutableTreeNode
        var tenantNode = favoritesNode.parent as DefaultMutableTreeNode
        while (tenantNode.userObject !is Tenant) {
            tenantNode = tenantNode.parent as DefaultMutableTreeNode
        }
        val tenant = tenantNode.userObject as Tenant

        tenant.removeResourceFromFavorites(cpiPackage, artifact, resource)

        artifactNode.remove(selectedNode)

        val model = tree.model as DefaultTreeModel
        model.nodeStructureChanged(artifactNode)
    }

    override fun update(e: AnActionEvent) {
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JTree ?: return
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode
        val userObject = selectedNode?.userObject

        if (userObject is CpiResource) {
            val artifactNode = selectedNode.parent as? DefaultMutableTreeNode
            val artifact = artifactNode?.userObject as? CpiArtifact
            val packageNode = artifactNode?.parent as? DefaultMutableTreeNode
            val cpiPackage = packageNode?.userObject as? CpiPackage
            val favoritesNode = packageNode?.parent as? DefaultMutableTreeNode
            if (favoritesNode?.userObject is Favorites && !artifact?.autoLoad!! && !cpiPackage?.autoLoad!!) {
                e.presentation.isEnabledAndVisible = true
                return
            }
        }
        e.presentation.isEnabledAndVisible = false
    }
}
