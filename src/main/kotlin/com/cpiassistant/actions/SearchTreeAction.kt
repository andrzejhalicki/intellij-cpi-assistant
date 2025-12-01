package com.cpiassistant.actions

import com.cpiassistant.nodes.BaseNode
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.TreePath
import javax.swing.tree.DefaultMutableTreeNode

class SearchTreeAction(private val tree: JTree) : AnAction(), CustomComponentAction {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var scheduledTask: ScheduledFuture<*>? = null
    override fun actionPerformed(event: AnActionEvent) {

    }

    override fun createCustomComponent(
        presentation: com.intellij.openapi.actionSystem.Presentation,
        place: String
    ): JComponent {
        val searchField = SearchTextField(false)

        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                scheduledTask?.cancel(false)

                scheduledTask = scheduler.schedule({
                    val query = searchField.text.trim()
                    if (query.length > 2) {
                        searchTree(tree, query)
                    }
                }, 300, TimeUnit.MILLISECONDS)
            }
        })
        return searchField
    }

    private fun searchTree(tree: JTree, query: String) {
        val model = tree.model
        val root = model.root as? DefaultMutableTreeNode ?: return

        val matchingPaths = mutableListOf<TreePath>()
        findMatchingNodes(root, query, matchingPaths, tree)

        ApplicationManager.getApplication().invokeLater {

            for (path in matchingPaths) {
                tree.expandPath(path)
            }

            if (matchingPaths.isNotEmpty()) {
                tree.scrollPathToVisible(matchingPaths.first())
                tree.selectionPaths = matchingPaths.toTypedArray()
            }
        }
    }

    private fun findMatchingNodes(
        node: DefaultMutableTreeNode,
        query: String,
        matchingPaths: MutableList<TreePath>,
        tree: JTree
    ) {
        val name = (node.userObject as? BaseNode)?.name ?: ""
        if (name.contains(query, ignoreCase = true)) {
            matchingPaths.add(TreePath(node.path))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
            findMatchingNodes(child, query, matchingPaths, tree)
        }
    }
}
