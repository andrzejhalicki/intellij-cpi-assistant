package com.cpiassistant.services

import CustomDataProvider
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.CpiResource
import com.cpiassistant.nodes.Favorites
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.toolWindow.MyTreeCellEditor
import com.cpiassistant.toolWindow.MyTreeModel
import com.cpiassistant.toolWindow.TreeCellRenderer
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.treeStructure.Tree
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

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

                if (e == null) return

                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e,tree)
                    return
                }

                if (SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2) {
                    handleDoubleClick(e,tree)
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
        val favoritesNode = DefaultMutableTreeNode(Favorites())
        tenant.add(favoritesNode)
        cpiTenant.getPackages { packages ->
            packages.forEach { ps ->
                tenant.add(DefaultMutableTreeNode(ps))
            }
        }
        cpiTenant.favoritePackages.forEach { favPackage ->
            favoritesNode.add(DefaultMutableTreeNode(favPackage))
        }
        tenant.children().asIterator().forEach { p ->
            val packageNode = p as DefaultMutableTreeNode
            if (packageNode.userObject is Favorites) {
                packageNode.children().asIterator().forEach { fp ->
                    val favPackageNode = fp as DefaultMutableTreeNode
                    loadPackage(favPackageNode)
                }
                return@forEach
            }
            loadPackage(packageNode)
        }
        cpiTenant.isLoaded = true
    }

    private fun loadPackage(packageNode: DefaultMutableTreeNode) {
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

    private fun handleRightClick(e: MouseEvent?, tree: Tree) {
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
            val parent = nodeHoveredOver.parent as DefaultMutableTreeNode
            if (parent.userObject is Favorites) {
                val actionGroup =
                    actionManager.getAction("com.cpiassistant.actions.FavoritePackageActionGroup") as ActionGroup
                val popupMenu = actionManager.createActionPopupMenu(
                    "com.cpiassistant.actions.FavoritePackageActionGroup",
                    actionGroup
                )
                tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
                popupMenu.component.show(e?.component, e!!.x, e.y)
                return
            }
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

    private fun handleDoubleClick(e: MouseEvent?, tree: Tree) {
        val path = tree.getPathForLocation(e!!.x, e!!.y)
        if (path?.getLastPathComponent() == null) {
            return
        }

        val nodeHoveredOver = path.getLastPathComponent() as DefaultMutableTreeNode
        if (nodeHoveredOver.userObject is CpiResource) {
            val resource = nodeHoveredOver.userObject as CpiResource
            val file = LocalFileSystem.getInstance().findFileByPath(resource.path)
            if (file == null || !file.exists()) {
                return
            }
            FileEditorManager.getInstance(project).openFile(file, true)
        }
    }
}
