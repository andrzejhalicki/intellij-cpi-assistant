package com.cpiassistant.toolWindow

import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.nodes.artifact.CpiArtifact
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class LazyLoadingTreeModel(root: DefaultMutableTreeNode) : DefaultTreeModel(root) {

    override fun isLeaf(node: Any?): Boolean {
        if (node !is DefaultMutableTreeNode) {
            return true
        }

        val userObject = node.userObject

        return when (userObject) {
            is Tenant -> userObject.isLoaded && node.childCount == 0
            is CpiPackage -> userObject.isLoaded && node.childCount == 0
            is CpiArtifact -> userObject.isLoaded && node.childCount == 0
            else -> node.childCount == 0
        }
    }
}