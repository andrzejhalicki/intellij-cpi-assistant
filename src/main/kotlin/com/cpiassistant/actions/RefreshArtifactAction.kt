package com.cpiassistant.actions

import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.services.NotificationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.jetbrains.annotations.NotNull
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class RefreshArtifactAction : AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val artifact = selectedNode.userObject as CpiArtifact

        artifact.isLoaded = false
        artifact.refreshResources(artifact.id) { resources ->
            if (resources.isEmpty()) {
                artifact.isLoaded = true
                return@refreshResources
            }
            selectedNode.removeAllChildren()
            resources.forEach { resource ->
                selectedNode.add(DefaultMutableTreeNode(resource))
                resource.isLoaded = true
            }
            artifact.isLoaded = true
            val model = tree.model as DefaultTreeModel
            model.nodeStructureChanged(selectedNode)
            tree.expandPath(selectionPath)

            NotificationService.getInstance()?.showInfo("Artifact ${artifact.name} refreshed.")
        }
    }

}
