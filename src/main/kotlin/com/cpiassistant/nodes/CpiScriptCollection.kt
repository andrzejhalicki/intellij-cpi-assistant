package com.cpiassistant.nodes

import com.cpiassistant.services.CpiService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager

class CpiScriptCollection(override val id: String, override val name: String, override val service: CpiService,
                          override var isLoaded: Boolean = false
): CpiArtifact(id, name, service, isLoaded) {
    private val resources = mutableListOf<CpiResource>()

    override fun getResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        this.service.getScriptCollectionResources(artifactId) { r ->
            this.resources.addAll(r)
            callback(this.resources)
        }
    }

    override fun addResource(name: String, content: String, callback: (Boolean) -> Unit) {
        this.service.createScriptCollectionResource(this.id, name,content) { res ->
            if(res) {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource ${name} added", NotificationType.INFORMATION))
            } else {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource ${name} not added", NotificationType.ERROR))
            }
            callback(res)
        }
    }

    override fun updateResource(name: String, content: String, callback: (Boolean) -> Unit) {
        this.service.updateScriptCollectionResource(this.id, name,content) { success, message ->
            if(success) {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource ${name} updated", NotificationType.INFORMATION))
            } else {
                Notifications.Bus.notify(Notification("Custom Notification Group", "Resource ${name} not updated: $message", NotificationType.ERROR))
            }
            callback(success)
        }
    }

    override fun deploy() {
        this.service.deployScriptCollection(this.id) { taskId ->
            val service = this.service
            ApplicationManager.getApplication().executeOnPooledThread(object: Runnable {
                override fun run() {
                    var running = true
                    while(running) {
                        service.checkDeploymentStatus(taskId) { status, success ->
                            if(success == true && status == "SUCCESS") {
                                running = false
                                Notifications.Bus.notify(Notification("Custom Notification Group", "Deployment of ${this@CpiScriptCollection.name} Succeeded", NotificationType.INFORMATION))
                            } else if(success == false) {
                                running = false
                                Notifications.Bus.notify(Notification("Custom Notification Group", "${this@CpiScriptCollection.name}: ${status}", NotificationType.ERROR))
                            }
                        }
                        Thread.sleep(3000)
                    }
                }
            })
        }
    }
}