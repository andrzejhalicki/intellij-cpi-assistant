package com.cpiassistant.actions

import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.NodeType
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.nodes.artifact.CpiArtifact
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class AddArtifactToFavoritesAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath = tree.selectionPath ?: return
        val selectedNode = selectionPath.lastPathComponent as? DefaultMutableTreeNode ?: return

        val artifact = selectedNode.userObject as? CpiArtifact ?: return
        val packageNode = selectedNode.parent as? DefaultMutableTreeNode ?: return
        val cpiPackage = packageNode.userObject as? CpiPackage ?: return
        val tenantNode = packageNode.parent as? DefaultMutableTreeNode ?: return
        val tenant = tenantNode.userObject as? Tenant ?: return

        val favoritesNode = tenantNode.children().toList().find {
            ((it as DefaultMutableTreeNode).userObject as BaseNode).type == NodeType.FAVORITES
        } as? DefaultMutableTreeNode ?: return

        tenant.addArtifactToFavorites(cpiPackage, artifact) { addedArtifact ->
            val packageInFavorites = favoritesNode.children().toList()
                .map { it as DefaultMutableTreeNode }
                .find { (it.userObject as? CpiPackage)?.id == cpiPackage.id }
                ?: DefaultMutableTreeNode(CpiPackage(cpiPackage.id, cpiPackage.name, tenant.service)).also {
                    favoritesNode.add(it)
                }

            val artifactNode = DefaultMutableTreeNode(addedArtifact)
            addedArtifact.isLoaded = false
            packageInFavorites.add(artifactNode)

            val model = tree.model as DefaultTreeModel
            model.nodeStructureChanged(favoritesNode)
        }
    }

    override fun update(e: AnActionEvent) {
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JTree
        val selectionPath: TreePath? = tree?.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode
        val userObject = selectedNode?.userObject

        if (userObject is CpiArtifact) {
            val packageNode = selectedNode.parent as? DefaultMutableTreeNode
            val cpiPackage = packageNode?.userObject as? CpiPackage
            val tenantNode = packageNode?.parent as? DefaultMutableTreeNode
            val tenant = tenantNode?.userObject as? Tenant

            if (tenant != null && cpiPackage != null) {
                e.presentation.isEnabledAndVisible = !tenant.isArtifactFavorite(cpiPackage, userObject)
            } else {
                e.presentation.isEnabledAndVisible = false
            }
        } else {
            e.presentation.isEnabledAndVisible = false
        }
    }
}
