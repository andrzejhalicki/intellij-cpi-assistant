package com.cpiassistant.actions

import CustomDataProvider
import FileNodeInfo
import FileNodeStateComponent
import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiResource
import com.cpiassistant.toolWindow.MyTreeCellEditor
import com.cpiassistant.toolWindow.TableCellRenderer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NotNull
import java.io.File
import java.util.*
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JTree
import javax.swing.tree.*

class AddResourceAction: AnAction() {
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val component = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JComponent
        val project = event.project
        val tree = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as JTree
        val dataProvider = tree.getClientProperty("CustomDataProvider") as? CustomDataProvider
        val artifact = dataProvider?.getData("com.cpiassistant.nodes.CpiArtifact") as? CpiArtifact
        val selectionPath: TreePath? = tree.selectionPath
        val selectedNode = selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return

        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        val selectedFiles = FileChooser.chooseFiles(descriptor, project, null)
        if (selectedFiles.isNotEmpty()) {
            val selectedFile: VirtualFile = selectedFiles[0]
            val newResource = CpiResource(selectedFile.name, selectedFile.name, selectedFile.path,artifact!!.id)
            val fileEncoded = Base64.getEncoder().encodeToString(selectedFile.contentsToByteArray())
            artifact.addResource(newResource.id,fileEncoded) { res ->
                if (!res) return@addResource

                val newNodeData =
                    FileNodeInfo(selectedFile.name, selectedFile.path, (selectedNode.userObject as BaseNode).id)

                val newNode = DefaultMutableTreeNode(
                    CpiResource(
                        selectedFile.name,
                        selectedFile.name,
                        selectedFile.path,
                        artifact!!.id,
                        true
                    )
                )

                selectedNode.add(newNode)

                val model = tree.model as DefaultTreeModel
                model.nodeStructureChanged(selectedNode)

                // Optionally, you can expand the selected node to show the new child
                tree.expandPath(selectionPath)
                // Start editing the new node
                val newNodePath = selectionPath.pathByAddingChild(newNode)
                tree.scrollPathToVisible(newNodePath)
                tree.startEditingAtPath(newNodePath)

                // Save to persistent storage
                val fileNodeStateComponent = service<FileNodeStateComponent>()
                fileNodeStateComponent.addFileNode(newNodeData)
            }
        }
    }

}