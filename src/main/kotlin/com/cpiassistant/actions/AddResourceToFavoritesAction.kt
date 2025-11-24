package com.cpiassistant.actions

import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiPackage
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

class AddResourceToFavoritesAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath = tree.selectionPath ?: return
        val selectedNode = selectionPath.lastPathComponent as? DefaultMutableTreeNode ?: return

        val resource = selectedNode.userObject as? CpiResource ?: return
        val artifactNode = selectedNode.parent as? DefaultMutableTreeNode ?: return
        val artifact = artifactNode.userObject as? CpiArtifact ?: return
        val packageNode = artifactNode.parent as? DefaultMutableTreeNode ?: return
        val cpiPackage = packageNode.userObject as? CpiPackage ?: return
        val tenantNode = packageNode.parent as? DefaultMutableTreeNode ?: return
        val tenant = tenantNode.userObject as? Tenant ?: return

        val favoritesNode = tenantNode.children().toList().find {
            ((it as DefaultMutableTreeNode).userObject as BaseNode).id == "Favorites"
        } as? DefaultMutableTreeNode ?: return

        tenant.addResourceToFavorites(cpiPackage, artifact, resource) { addedResource ->
            val packageInFavorites = favoritesNode.children().toList()
                .map { it as DefaultMutableTreeNode }
                .find { (it.userObject as? CpiPackage)?.id == cpiPackage.id }
                ?: DefaultMutableTreeNode(CpiPackage(cpiPackage.id, cpiPackage.name, tenant.service)).also {
                    favoritesNode.add(it)
                }

            val artifactInFavorites = packageInFavorites.children().toList()
                .map { it as DefaultMutableTreeNode }
                .find { (it.userObject as? CpiArtifact)?.id == artifact.id }
                ?: DefaultMutableTreeNode(CpiArtifact(artifact.id, artifact.name, tenant.service)).also {
                    packageInFavorites.add(it)
                }

            val resourceNode = DefaultMutableTreeNode(addedResource)
            addedResource.isLoaded = true
            artifactInFavorites.add(resourceNode)

            val model = tree.model as DefaultTreeModel
            model.nodeStructureChanged(favoritesNode)
        }
    }

    override fun update(e: AnActionEvent) {
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JTree
        val selectionPath: TreePath? = tree?.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode
        val userObject = selectedNode?.userObject

        if (userObject is CpiResource) {
            val artifactNode = selectedNode.parent as? DefaultMutableTreeNode
            val artifact = artifactNode?.userObject as? CpiArtifact
            val packageNode = artifactNode?.parent as? DefaultMutableTreeNode
            val cpiPackage = packageNode?.userObject as? CpiPackage
            val tenantNode = packageNode?.parent as? DefaultMutableTreeNode
            val tenant = tenantNode?.userObject as? Tenant

            if (tenant != null && cpiPackage != null && artifact != null) {
                e.presentation.isEnabledAndVisible = !tenant.isResourceFavorite(cpiPackage, artifact, userObject)
            } else {
                e.presentation.isEnabledAndVisible = false
            }
        } else {
            e.presentation.isEnabledAndVisible = false
        }
    }
}
