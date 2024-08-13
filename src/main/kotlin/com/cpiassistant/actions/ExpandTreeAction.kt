package com.cpiassistant.actions

import com.cpiassistant.MyIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JTree

class ExpandTreeAction(private val tree: JTree) : AnAction("Expand All", null, MyIcons.Expand) {
    override fun actionPerformed(event: AnActionEvent) {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }
}