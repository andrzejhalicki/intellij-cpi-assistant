package com.cpiassistant.nodes

import com.cpiassistant.services.CpiService
import com.cpiassistant.services.NotificationService
import com.intellij.openapi.application.ApplicationManager

open class CpiArtifact(override val id: String, override val name: String, open val service: CpiService,
                       override var isLoaded: Boolean = false
): BaseNode() {
    private val resources = mutableListOf<CpiResource>()
    override val type = "IFlow"

    open fun getResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        if(!this.resources.isEmpty()){
            callback(this.resources)
            return
        }
        getResourcesFromService(artifactId) { r ->
            this.resources.addAll(r)
            callback(this.resources)
        }
    }

    protected open fun getResourcesFromService(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        this.service.getResources(artifactId, callback)
    }

    open fun refreshResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        getResourcesFromService(artifactId) { r ->
            this.resources.clear()
            this.resources.addAll(r)
            callback(this.resources)
        }
    }

    open fun addResource(name: String, content: String, callback: (Boolean) -> Unit) {
        createResourceInService(name, content) { res ->
            val notificationService = NotificationService.getInstance()
            if(res) {
                notificationService?.showSuccess("Resource $name added")
            } else {
                notificationService?.showError("Resource $name not added")
            }
            callback(res)
        }
    }

    protected open fun createResourceInService(name: String, content: String, callback: (Boolean) -> Unit) {
        this.service.createResource(this.id, name, content, callback)
    }

    open fun updateResource(name: String, content: String, callback: (Boolean) -> Unit) {
        updateResourceInService(name, content) { success, message ->
            val notificationService = NotificationService.getInstance()
            if(success) {
                notificationService?.showSuccess("Resource $name updated")
            } else {
                notificationService?.showError("Resource $name not updated", message)
            }
            callback(success)
        }
    }

    protected open fun updateResourceInService(name: String, content: String, callback: (Boolean, String) -> Unit) {
        this.service.updateResource(this.id, name, content, callback)
    }

    open fun downloadResource(name: String, callback: (String) -> Unit) {
        downloadResourceFromService(name, callback)
    }

    protected open fun downloadResourceFromService(name: String, callback: (String) -> Unit) {
        this.service.getResource(this.id, name, callback)
    }

    open fun deploy() {
        deployInService { taskId ->
            val service = this.service
            ApplicationManager.getApplication().executeOnPooledThread(object: Runnable {
                override fun run() {
                    var running = true
                    while(running) {
                        service.checkDeploymentStatus(taskId) { status, success ->
                            if(success == true && status == "SUCCESS") {
                                running = false
                                NotificationService.getInstance()?.showSuccess("Deployment of ${this@CpiArtifact.name} Succeeded")
                            } else if(success == false) {
                                running = false
                                NotificationService.getInstance()?.showError("${this@CpiArtifact.name}: $status")
                            }
                        }
                        Thread.sleep(3000)
                    }
                }
            })
        }
    }

    protected open fun deployInService(callback: (String) -> Unit) {
        this.service.deployArtifact(this.id, callback)
    }
}