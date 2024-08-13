package com.cpiassistant.toolWindow

import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiResource
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellEditor
import javax.swing.tree.DefaultTreeCellRenderer


class MyTreeCellEditor(tree: JTree, renderer: DefaultTreeCellRenderer) : DefaultTreeCellEditor(tree, renderer) {

    override fun getTreeCellEditorComponent(
        tree: JTree?,
        value: Any?,
        isSelected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int
    ): Component {
        val component = super.getTreeCellEditorComponent(tree, (value as DefaultMutableTreeNode).userObject, isSelected, expanded, leaf, row)
        component.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {}
            override fun focusLost(e: FocusEvent) {
                tree!!.stopEditing()
            }
        })
        return component
    }

    override fun getCellEditorValue(): Any {
        return (super.getCellEditorValue() as BaseNode).name
    }
}