package com.cpiassistant

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon


object MyIcons {
    @JvmField
    val Tenant = IconLoader.getIcon("/icons/connected.svg", javaClass)
    @JvmField
    val Package = IconLoader.getIcon("/icons/package.svg", javaClass)
    @JvmField
    val Artifact = IconLoader.getIcon("/icons/objectGroup.svg", javaClass)
    @JvmField
    val Script = IconLoader.getIcon("/icons/scriptingScript.svg", javaClass)
    @JvmField
    val Add = IconLoader.getIcon("/icons/add.svg", javaClass)
    @JvmField
    val Delete = IconLoader.getIcon("/icons/delete.svg", javaClass)
    @JvmField
    val Deploy = IconLoader.getIcon("/icons/deploy.svg", javaClass)
    @JvmField
    val Refresh = IconLoader.getIcon("/icons/refresh.svg", javaClass)
    @JvmField
    val AddFile = IconLoader.getIcon("/icons/addFile.svg", javaClass)
    @JvmField
    val Expand = IconLoader.getIcon("/icons/expandAll.svg", javaClass)
    @JvmField
    val Collapse = IconLoader.getIcon("/icons/collapseAll.svg", javaClass)
    @JvmField
    val Loading = AnimatedIcon.Default()
    @JvmField
    val ScriptCollection = IconLoader.getIcon("/icons/listFiles.svg", javaClass)

}