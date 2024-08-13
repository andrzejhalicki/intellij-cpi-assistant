package com.cpiassistant.toolWindow

import com.cpiassistant.MyIcons
import com.cpiassistant.nodes.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer


class TableCellRenderer() : DefaultTreeCellRenderer() {

    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val renderer = JPanel(FlowLayout(FlowLayout.LEFT))
        if ((value != null) && (value is DefaultMutableTreeNode)) {
            val userObject = (value as DefaultMutableTreeNode).userObject;
            if(userObject == "Root") {
                buildRoot(renderer)
            } else if (userObject is Tenant) {
                buildTenant(renderer, userObject)
                if (userObject.isLoaded) {
                    renderer.remove(0)
                }
            } else if (userObject is CpiPackage) {
                buildPackage(renderer, userObject)
                if (userObject.isLoaded) {
                    renderer.remove(0)
                }
            } else if (userObject is CpiArtifact && userObject !is CpiScriptCollection) {
                buildArtifact(renderer, userObject)
                if (userObject.isLoaded) {
                    renderer.remove(0)
                }
            } else if (userObject is CpiScriptCollection) {
                buildScriptCollection(renderer, userObject)
                if (userObject.isLoaded) {
                    renderer.remove(0)
                }
            } else if (userObject is CpiResource) {
                buildResource(renderer, userObject)
                if (userObject.isLoaded) {
                    renderer.remove(0)
                }
            }
        }
        return renderer as Component
    }

    private fun buildRoot(renderer: JPanel) {
        renderer.add(JBLabel("Systems"));
    }

    private fun buildTenant(renderer: JPanel, tenant: Tenant) {
        renderer.add(JBLabel(MyIcons.Loading))
        renderer.add(JBLabel(MyIcons.Tenant))
        renderer.add(JLabel(tenant.name));
    }

    private fun buildPackage(renderer: JPanel, cpiPackage: CpiPackage) {
        renderer.add(JBLabel(MyIcons.Loading))
        val icon = JBLabel(MyIcons.Package)
        renderer.add(icon)
        renderer.add(JLabel(cpiPackage.name));
    }

    private fun buildArtifact(renderer: JPanel, cpiArtifact: CpiArtifact) {
        renderer.add(JBLabel(MyIcons.Loading))
        val icon = JBLabel(MyIcons.Artifact)
        renderer.add(icon)
        renderer.add(JBLabel(cpiArtifact.name));
    }

    private fun buildScriptCollection(renderer: JPanel, cpiScriptCollection: CpiScriptCollection) {
        renderer.add(JBLabel(MyIcons.Loading))
        val icon = JBLabel(MyIcons.ScriptCollection)
        renderer.add(icon)
        renderer.add(JBLabel(cpiScriptCollection.name));
    }

    private fun buildResource(renderer: JPanel, resource: CpiResource) {
        renderer.add(JBLabel(MyIcons.Loading))
        val icon = JBLabel(MyIcons.Script)
        renderer.add(icon)
        renderer.add(JLabel(resource.name));
        val pathLabel = JBLabel(resource.path)
        pathLabel.foreground = SimpleTextAttributes.GRAYED_ATTRIBUTES.fgColor
        renderer.add(pathLabel)
    }
}