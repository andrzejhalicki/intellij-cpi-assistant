package com.cpiassistant.nodes

import com.cpiassistant.services.CpiService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager

open class CpiArtifact(override val id: String, override val name: String, open val service: CpiService,
                       override var isLoaded: Boolean = false
): BaseNode(), DataContext {
    private val resources = mutableListOf<CpiResource>()

    open fun getResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        this.service.getResources(artifactId) { r ->
            this.resources.addAll(r)
            callback(this.resources)
        }
    }

    open fun addResource(name: String, content: String, callback: (Boolean) -> Unit) {
        this.service.createResource(this.id, name,content) { res ->
            if(res) {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource added", NotificationType.INFORMATION))
            } else {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource not added", NotificationType.ERROR))
            }
            callback(res)
        }
    }

    open fun updateResource(name: String, content: String, callback: (Boolean) -> Unit) {
        this.service.updateResource(this.id, name,content) { success, message ->
            if(success) {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource ${name} updated", NotificationType.INFORMATION))
            } else {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource ${name} not updated: $message", NotificationType.ERROR))
            }
            callback(success)
        }
    }

    override fun getData(dataId: String): Any? {
        println(dataId)
        return this
    }

    open fun deploy() {
        this.service.deployArtifact(this.id) { taskId ->
            val service = this.service
            ApplicationManager.getApplication().executeOnPooledThread(object: Runnable {
                override fun run() {
                    var running = true
                    while(running) {
                        service.checkDeploymentStatus(taskId) { status, success ->
                            if(success == true && status == "SUCCESS") {
                                running = false
                                Notifications.Bus.notify(Notification("Custom Notification Group", "Deployment of ${this@CpiArtifact.name} Succeeded", NotificationType.INFORMATION))
                            } else if(success == false) {
                                running = false
                                Notifications.Bus.notify(Notification("Custom Notification Group", "${this@CpiArtifact.name}: ${status}", NotificationType.ERROR))
                            }
                        }
                        Thread.sleep(3000)
                    }
                }
            })
        }
    }
}