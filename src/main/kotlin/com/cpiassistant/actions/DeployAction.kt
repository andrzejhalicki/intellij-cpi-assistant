package com.cpiassistant.actions

import CustomDataProvider
import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.CpiArtifact
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent

class DeployAction : AnAction() {

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val component = event.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JComponent
        val dataProvider = component?.getClientProperty("CustomDataProvider") as? CustomDataProvider
        val artifact = dataProvider?.getData("com.cpiassistant.nodes.CpiArtifact") as? CpiArtifact
        artifact?.deploy()
    }

}