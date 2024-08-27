package com.cpiassistant.services

import CustomDataProvider
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.CpiResource
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.toolWindow.MyTreeCellEditor
import com.cpiassistant.toolWindow.MyTreeModel
import com.cpiassistant.toolWindow.TreeCellRenderer
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.treeStructure.Tree
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

@Service(Service.Level.PROJECT)
class TreeService(private val project: Project) {

    public var tree: Tree = Tree()

    init {

    }
    fun buildTree(): Tree {

        val treeModel: MyTreeModel = MyTreeModel()

        val models = treeModel.getModels()
        val dummyRoot = DefaultMutableTreeNode("Dummy Root")
        models.forEach { dummyRoot.add(it) }
        val rootModel = DefaultTreeModel(dummyRoot)
        tree = Tree(rootModel).apply {
            isRootVisible = false
            showsRootHandles = true
        }

        tree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true);

        tree.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    return
                }

                val path = tree.getPathForLocation(e!!.x, e!!.y)
                if (path?.getLastPathComponent() == null) {
                    return
                }

                val nodeHoveredOver = path.getLastPathComponent() as DefaultMutableTreeNode
                val actionManager = ActionManager.getInstance()
                if (nodeHoveredOver.userObject is CpiArtifact) {
                    val actionGroup =
                        actionManager.getAction("com.cpiassistant.actions.ArtifactActionGroup") as ActionGroup
                    val popupMenu = actionManager.createActionPopupMenu(
                        "com.cpiassistant.actions.ArtifactActionGroup",
                        actionGroup
                    )
                    tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
                    popupMenu.component.show(e?.component, e!!.x, e.y)
                    return
                } else if (nodeHoveredOver.userObject is CpiPackage) {
                    val actionGroup =
                        actionManager.getAction("com.cpiassistant.actions.PackageActionGroup") as ActionGroup
                    val popupMenu = actionManager.createActionPopupMenu(
                        "com.cpiassistant.actions.PackageActionGroup",
                        actionGroup
                    )
                    tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
                    popupMenu.component.show(e?.component, e!!.x, e.y)
                    return
                } else if (nodeHoveredOver.userObject is Tenant) {
                    val actionGroup =
                        actionManager.getAction("com.cpiassistant.actions.TenantActionGroup") as ActionGroup
                    val popupMenu = actionManager.createActionPopupMenu(
                        "com.cpiassistant.actions.TenantActionGroup",
                        actionGroup
                    )
                    tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
                    popupMenu.component.show(e?.component, e!!.x, e.y)
                    return
                } else if (nodeHoveredOver.userObject is CpiResource) {
                    val actionGroup =
                        actionManager.getAction("com.cpiassistant.actions.ResourceActionGroup") as ActionGroup
                    val popupMenu = actionManager.createActionPopupMenu(
                        "com.cpiassistant.actions.ResourceActionGroup",
                        actionGroup
                    )
                    tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
                    popupMenu.component.show(e?.component, e!!.x, e.y)
                    return
                }
            }

            override fun mousePressed(e: MouseEvent?) {

            }

            override fun mouseReleased(e: MouseEvent?) {

            }

            override fun mouseEntered(e: MouseEvent?) {

            }

            override fun mouseExited(e: MouseEvent?) {

            }
        })
        val renderer: DefaultTreeCellRenderer = TreeCellRenderer()
        tree.setCellRenderer(renderer);
        tree.cellEditor = MyTreeCellEditor(tree, renderer)

        return tree
    }

    fun updateTree(tenant: DefaultMutableTreeNode) {

        val cpiTenant = tenant.userObject as Tenant
        cpiTenant.getPackages { ps ->
            ps.forEach {
                tenant.add(DefaultMutableTreeNode(it))
            }
        }
        tenant.children().asIterator().forEach { p ->
            val packageNode = p as DefaultMutableTreeNode
            val cpiPackage = packageNode.userObject as CpiPackage
            cpiPackage.getArtifacts(cpiPackage.id) { artifacts ->
                artifacts.forEach { artifact ->
                    val artifactNode = DefaultMutableTreeNode(artifact)
                    packageNode.add(artifactNode)
                    artifact.getResources(artifact.id) { resources ->
                        resources.forEach { resource ->
                            artifactNode.add(DefaultMutableTreeNode(resource))
                            resource.isLoaded = true
                        }
                    }
                    artifact.isLoaded = true
                }
            }
            cpiPackage.getScriptCollections(cpiPackage.id) { scriptCollections ->
                scriptCollections.forEach { scriptCollection ->
                    val collectionNode = DefaultMutableTreeNode(scriptCollection)
                    packageNode.add(collectionNode)
                    scriptCollection.getResources(scriptCollection.id) { resources ->
                        resources.forEach { resource ->
                            collectionNode.add(DefaultMutableTreeNode(resource))
                            resource.isLoaded = true
                        }
                    }
                    scriptCollection.isLoaded = true
                }
            }
            cpiPackage.isLoaded = true
        }
        cpiTenant.isLoaded = true
    }
}
