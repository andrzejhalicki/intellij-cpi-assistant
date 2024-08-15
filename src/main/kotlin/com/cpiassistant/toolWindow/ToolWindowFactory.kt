package com.cpiassistant.toolWindow

import com.cpiassistant.actions.CollapseTreeAction
import com.cpiassistant.actions.ExpandTreeAction
import com.cpiassistant.services.TreeService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


class ToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val treePanel = myToolWindow.getContent()
        val treeService = toolWindow.project.service<TreeService>()
        val content = ContentFactory.getInstance().createContent(treePanel, null, false)
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(content)
        ApplicationManager.getApplication().executeOnPooledThread(object : Runnable {
            override fun run() {
                //Tree
                val tree = treeService.buildTree()
                myToolWindow.actionGroup.add(ExpandTreeAction(tree))
                myToolWindow.actionGroup.add(CollapseTreeAction(tree))

                val treeScrollPane = JBScrollPane(tree)
                treePanel.add(treeScrollPane, BorderLayout.CENTER)

                val model = tree.model as DefaultTreeModel
                val childCount = model.getChildCount(model.root)
                for (i in 0 until childCount) {
                    val child = model.getChild(model.root, i) as DefaultMutableTreeNode
                    ApplicationManager.getApplication().executeOnPooledThread(object : Runnable {
                        override fun run() {
                            treeService.updateTree(child)
                        }
                    })
                }
            }
        })
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        val actionGroup: DefaultActionGroup = DefaultActionGroup()

        fun getContent() = JBPanel<JBPanel<*>>(BorderLayout()).apply {

            actionGroup.add(ActionManager.getInstance().getAction("com.cpiassistant.actions.AddTenant"))
            val actionToolbar: ActionToolbar =
                ActionManager.getInstance().createActionToolbar("CPI Assistant", actionGroup, true)
            actionToolbar.targetComponent = this
            add(actionToolbar.component, BorderLayout.NORTH)
        }

    }
}
