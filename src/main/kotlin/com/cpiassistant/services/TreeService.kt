package com.cpiassistant.services

import CustomDataProvider
import FavoritePackageInfo
import FavoriteResourceInfo
import com.cpiassistant.nodes.artifact.CpiArtifact
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.resource.CpiResource
import com.cpiassistant.nodes.Favorites
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.toolWindow.LazyLoadingTreeModel
import com.cpiassistant.toolWindow.MyTreeCellEditor
import com.cpiassistant.toolWindow.MyTreeModel
import com.cpiassistant.toolWindow.TreeCellRenderer
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.treeStructure.Tree
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

@Service(Service.Level.PROJECT)
class TreeService(private val project: Project) {

    var tree: Tree = Tree()

    init {

    }

    fun buildTree(): Tree {

        val treeModel = MyTreeModel()

        val models = treeModel.getModels()
        val dummyRoot = DefaultMutableTreeNode("Dummy Root")
        models.forEach { dummyRoot.add(it) }
        val rootModel = LazyLoadingTreeModel(dummyRoot)
        tree = Tree(rootModel).apply {
            isRootVisible = false
            showsRootHandles = true
        }

        tree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)

        // Add tree expansion listener for lazy loading
        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                val expandedNode = event.path.lastPathComponent as? DefaultMutableTreeNode ?: return
                handleNodeExpansion(expandedNode)
            }

            override fun treeCollapsed(event: TreeExpansionEvent) {
                // No action needed on collapse
            }
        })

        tree.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {

                if (e == null) return

                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e, tree)
                    return
                }

                if (SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2) {
                    handleDoubleClick(e, tree)
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
        val renderer = TreeCellRenderer()
        tree.setCellRenderer(renderer)
        tree.cellEditor = MyTreeCellEditor(tree, DefaultTreeCellRenderer())

        return tree
    }

    private fun handleNodeExpansion(node: DefaultMutableTreeNode) {
        val userObject = node.userObject

        when (userObject) {
            is Tenant -> {
                if (node.childCount <= 1) {
                    updateTree(node)
                }
            }

            is CpiPackage -> {
                if (node.childCount == 0) {
                    loadPackageLazy(node)
                }
            }

            is CpiArtifact -> {
                if (node.childCount == 0) {
                    loadArtifactResources(node)
                }
            }
        }
    }

    fun updateTree(tenant: DefaultMutableTreeNode) {

        val cpiTenant = tenant.userObject as Tenant

        cpiTenant.isLoading = true

        val model = tree.model as DefaultTreeModel
        model.nodeChanged(tenant)

        val favorites = Favorites()
        val favoritesNode = DefaultMutableTreeNode(favorites)
        tenant.add(favoritesNode)

        favorites.isLoading = true
        model.nodeChanged(favoritesNode)

        ApplicationManager.getApplication().executeOnPooledThread {

            cpiTenant.getPackages { packages ->
                packages.forEach { ps ->
                    tenant.add(DefaultMutableTreeNode(ps))
                }
            }

            cpiTenant.favoritePackages.forEach { favPackage ->
                val cpiPackage = CpiPackage(
                    favPackage.packageId,
                    favPackage.packageName,
                    cpiTenant.service
                )
                val packageNode = DefaultMutableTreeNode(cpiPackage)
                favoritesNode.add(packageNode)
                cpiPackage.isLoading = true
                ApplicationManager.getApplication().invokeLater {
                    val model = tree.model as DefaultTreeModel
                    model.nodeStructureChanged(packageNode)
                }
            }

            val childrenList = tenant.children().toList()
            childrenList.forEach { p ->
                val packageNode = p as DefaultMutableTreeNode
                if (packageNode.userObject is Favorites) {
                    val favChildrenList = packageNode.children().toList()
                    favChildrenList.forEach { fp ->
                        val favPackageNode = fp as DefaultMutableTreeNode
                        loadFavPackage(favPackageNode, cpiTenant)
                    }
                    return@forEach
                }
            }

            cpiTenant.isLoaded = true
            cpiTenant.isLoading = false
            favorites.isLoaded = true
            favorites.isLoading = false
            ApplicationManager.getApplication().invokeLater {
                val model = tree.model as DefaultTreeModel
                model.nodeStructureChanged(tenant)
                model.nodeStructureChanged(favoritesNode)
            }
        }
    }

    private fun notifyNodeChanged(node: DefaultMutableTreeNode) {
        ApplicationManager.getApplication().invokeLater {
            val model = tree.model as DefaultTreeModel
            model.nodeStructureChanged(node)
        }
    }

    private fun loadArtifactWithResources(
        artifact: CpiArtifact,
        parentNode: DefaultMutableTreeNode
    ) {
        val artifactNode = DefaultMutableTreeNode(artifact)
        parentNode.add(artifactNode)
        artifact.getResources(artifact.id) { resources ->
            resources.forEach { resource ->
                artifactNode.add(DefaultMutableTreeNode(resource))
                resource.isLoaded = true
            }
            artifact.isLoading = false
            artifact.isLoaded = true
            notifyNodeChanged(artifactNode)
        }
    }

    private fun loadFullPackageContent(
        cpiPackage: CpiPackage,
        packageNode: DefaultMutableTreeNode
    ) {
        cpiPackage.getArtifacts(cpiPackage.id) { artifacts ->
            artifacts.forEach { artifact ->
                loadArtifactWithResources(artifact, packageNode)
            }
        }
        cpiPackage.getScriptCollections(cpiPackage.id) { scriptCollections ->
            scriptCollections.forEach { scriptCollection ->
                loadArtifactWithResources(scriptCollection, packageNode)
            }
        }
    }

    private fun loadSelectedFavorites(
        packageInfo: FavoritePackageInfo,
        packageNode: DefaultMutableTreeNode,
        service: CpiService
    ) {
        packageInfo.favoriteArtifacts.forEach { favArtifact ->
            val artifact = CpiArtifact(
                favArtifact.artifactId,
                favArtifact.artifactName,
                service
            )
            val artifactNode = DefaultMutableTreeNode(artifact)
            packageNode.add(artifactNode)

            if (favArtifact.autoLoad) {
                loadAllResourcesForArtifact(artifact, artifactNode)
            } else {
                loadSelectedResources(favArtifact.favoriteResources, artifact, artifactNode)
            }

            artifact.isLoaded = true
            artifact.isLoading = false
            notifyNodeChanged(artifactNode)
        }
    }

    private fun loadAllResourcesForArtifact(
        artifact: CpiArtifact,
        artifactNode: DefaultMutableTreeNode
    ) {
        artifact.getResources(artifact.id) { resources ->
            resources.forEach { resource ->
                artifactNode.add(DefaultMutableTreeNode(resource))
                resource.isLoaded = true
            }
        }
    }

    private fun loadSelectedResources(
        favoriteResources: List<FavoriteResourceInfo>,
        artifact: CpiArtifact,
        artifactNode: DefaultMutableTreeNode
    ) {
        favoriteResources.forEach { favResource ->
            val resource = CpiResource.create(
                favResource.resourceId,
                favResource.resourceName,
                path = artifact.service.getResourcePath(artifact.id, favResource.resourceName) ?: "",
                artifact.id,
                true
            )
            artifactNode.add(DefaultMutableTreeNode(resource))
            resource.isLoaded = true
        }
    }

    private fun loadFavPackage(packageNode: DefaultMutableTreeNode, tenant: Tenant) {
        val cpiPackage = packageNode.userObject as CpiPackage
        val packageInfo = tenant.favoritePackages.find { it.packageId == cpiPackage.id }

        if (packageInfo?.autoLoad == true) {
            loadFullPackageContent(cpiPackage, packageNode)
        } else {
            packageInfo?.let { loadSelectedFavorites(it, packageNode, tenant.service) }
        }

        cpiPackage.isLoaded = true
        cpiPackage.isLoading = false
        notifyNodeChanged(packageNode)
    }

    private fun loadPackageLazy(packageNode: DefaultMutableTreeNode) {
        val cpiPackage = packageNode.userObject as CpiPackage

        cpiPackage.isLoading = true

        val model = tree.model as DefaultTreeModel
        model.nodeChanged(packageNode)

        val pendingOps = java.util.concurrent.atomic.AtomicInteger(2)

        fun markLoadedIfComplete() {
            if (pendingOps.decrementAndGet() == 0) {
                cpiPackage.isLoading = false
                cpiPackage.isLoaded = true
                ApplicationManager.getApplication().invokeLater {
                    val treeModel = tree.model as DefaultTreeModel
                    treeModel.nodeChanged(packageNode)
                }
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            cpiPackage.getArtifacts(cpiPackage.id) { artifacts ->
                artifacts.forEach { artifact ->
                    val artifactNode = DefaultMutableTreeNode(artifact)
                    packageNode.add(artifactNode)
                }
                ApplicationManager.getApplication().invokeLater {
                    val treeModel = tree.model as DefaultTreeModel
                    treeModel.nodeStructureChanged(packageNode)
                }
                markLoadedIfComplete()
            }

            cpiPackage.getScriptCollections(cpiPackage.id) { scriptCollections ->
                scriptCollections.forEach { scriptCollection ->
                    val collectionNode = DefaultMutableTreeNode(scriptCollection)
                    packageNode.add(collectionNode)
                }
                ApplicationManager.getApplication().invokeLater {
                    val treeModel = tree.model as DefaultTreeModel
                    treeModel.nodeStructureChanged(packageNode)
                }
                markLoadedIfComplete()
            }
        }
    }

    private fun loadArtifactResources(artifactNode: DefaultMutableTreeNode) {
        val artifact = artifactNode.userObject as CpiArtifact

        artifact.isLoading = true

        val model = tree.model as DefaultTreeModel
        model.nodeChanged(artifactNode)

        ApplicationManager.getApplication().executeOnPooledThread {
            artifact.getResources(artifact.id) { resources ->
                resources.forEach { resource ->
                    artifactNode.add(DefaultMutableTreeNode(resource))
                }

                artifact.isLoading = false
                artifact.isLoaded = true

                ApplicationManager.getApplication().invokeLater {
                    val treeModel = tree.model as DefaultTreeModel
                    treeModel.nodeStructureChanged(artifactNode)
                }
            }
        }
    }

    private fun handleRightClick(e: MouseEvent?, tree: Tree) {
        val path = tree.getPathForLocation(e!!.x, e.y)
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
            popupMenu.component.show(e.component, e.x, e.y)
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
                popupMenu.component.show(e.component, e.x, e.y)
                return
            }
            val actionGroup =
                actionManager.getAction("com.cpiassistant.actions.PackageActionGroup") as ActionGroup
            val popupMenu = actionManager.createActionPopupMenu(
                "com.cpiassistant.actions.PackageActionGroup",
                actionGroup
            )
            tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
            popupMenu.component.show(e.component, e.x, e.y)
            return
        } else if (nodeHoveredOver.userObject is Tenant) {
            val actionGroup =
                actionManager.getAction("com.cpiassistant.actions.TenantActionGroup") as ActionGroup
            val popupMenu = actionManager.createActionPopupMenu(
                "com.cpiassistant.actions.TenantActionGroup",
                actionGroup
            )
            tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
            popupMenu.component.show(e.component, e.x, e.y)
            return
        } else if (nodeHoveredOver.userObject is CpiResource) {
            val actionGroup =
                actionManager.getAction("com.cpiassistant.actions.ResourceActionGroup") as ActionGroup
            val popupMenu = actionManager.createActionPopupMenu(
                "com.cpiassistant.actions.ResourceActionGroup",
                actionGroup
            )
            tree.putClientProperty("CustomDataProvider", CustomDataProvider(nodeHoveredOver.userObject))
            popupMenu.component.show(e.component, e.x, e.y)
            return
        }
    }

    private fun handleDoubleClick(e: MouseEvent?, tree: Tree) {
        if (e == null) return
        val path = tree.getPathForLocation(e.x, e.y)
        if (path?.getLastPathComponent() == null) {
            return
        }

        val nodeHoveredOver = path.getLastPathComponent() as DefaultMutableTreeNode
        if (nodeHoveredOver.userObject is CpiResource) {
            val resource = nodeHoveredOver.userObject as CpiResource
            if (resource.path.isEmpty()) {
                tree.selectionPath = path
                val action = ActionManager.getInstance().getAction("com.cpiassistant.actions.DownloadResourceAction")
                val dataContext = com.intellij.openapi.actionSystem.DataContext { dataId ->
                    when (dataId) {
                        com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT.name -> project
                        com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT.name -> tree
                        else -> null
                    }
                }
                val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
                    action,
                    null,
                    "DoubleClick",
                    dataContext
                )
                action.actionPerformed(event)
                return
            }
            val file = LocalFileSystem.getInstance().findFileByPath(resource.path)
            if (file == null || !file.exists()) return
            FileEditorManager.getInstance(project).openFile(file, true)
        }
    }
}
