package com.cpiassistant.toolWindow

import com.cpiassistant.MyIcons
import com.cpiassistant.nodes.*
import com.cpiassistant.nodes.artifact.CpiArtifact
import com.cpiassistant.nodes.artifact.CpiScriptCollection
import com.cpiassistant.nodes.resource.CpiResource
import com.cpiassistant.nodes.resource.ResourceType
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode


class TreeCellRenderer : NodeRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        if (value is DefaultMutableTreeNode) {
            when (val userObject = value.userObject) {
                "Root" -> {
                    append("Systems")
                }
                is Tenant -> {
                    icon = getIconWithLoading(MyIcons.Tenant, userObject.isLoading)
                    append(userObject.name)
                }
                is Favorites -> {
                    icon = getIconWithLoading(MyIcons.Star, userObject.isLoading)
                    append(userObject.name)
                }
                is CpiPackage -> {
                    icon = getIconWithLoading(MyIcons.Package, userObject.isLoading)
                    append(userObject.name)
                }
                is CpiScriptCollection -> {
                    icon = getIconWithLoading(MyIcons.ScriptCollection, userObject.isLoading)
                    append(userObject.name)
                }
                is CpiArtifact -> {
                    icon = getIconWithLoading(MyIcons.Artifact, userObject.isLoading)
                    append(userObject.name)
                }
                is CpiResource -> {
                    val resourceIcon = when (userObject.resourceType) {
                        ResourceType.GROOVY -> MyIcons.Script
                        ResourceType.XSLT -> MyIcons.Xml
                        else -> MyIcons.Script
                    }
                    icon = getIconWithLoading(resourceIcon, userObject.isLoading)
                    append(userObject.name)
                    append(" ${userObject.path}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }

    private fun getIconWithLoading(baseIcon: Icon, isLoading: Boolean): Icon {
        return if (isLoading) MyIcons.Loading else baseIcon
    }
}