package com.cpiassistant.actions

import com.cpiassistant.MyIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.util.IconLoader
import javax.swing.JTree

class CollapseTreeAction(private val tree: JTree) : AnAction("Collapse All", null, MyIcons.Collapse) {
    override fun actionPerformed(event: AnActionEvent) {

        for (i in tree.rowCount - 1 downTo 0) {
            tree.collapseRow(i)
        }

    }
}