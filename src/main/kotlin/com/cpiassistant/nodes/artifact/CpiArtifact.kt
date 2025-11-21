package com.cpiassistant.nodes.artifact

import com.cpiassistant.nodes.BaseNode
import com.cpiassistant.nodes.resource.CpiResource
import com.cpiassistant.services.CpiService
import com.cpiassistant.services.NotificationService

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

    open fun addResource(resource: CpiResource, content: String, callback: (Boolean) -> Unit) {
        createResourceInService(resource, content) { res ->
            val notificationService = NotificationService.Companion.getInstance()
            if(res) {
                notificationService?.showSuccess("Resource $name added")
            } else {
                notificationService?.showError("Resource $name not added")
            }
            callback(res)
        }
    }

    protected open fun createResourceInService(resource: CpiResource, content: String, callback: (Boolean) -> Unit) {
        this.service.createResource(this.id, resource, content, callback)
    }

    protected open fun updateResourceInService(resource: CpiResource, content: String, callback: (Boolean, String) -> Unit) {
        this.service.updateResource(this.id, resource, content, callback)
    }

    open fun downloadResource(resource: CpiResource, callback: (String) -> Unit) {
        downloadResourceFromService(resource, callback)
    }

    protected open fun downloadResourceFromService(resource: CpiResource, callback: (String) -> Unit) {
        this.service.getResource(this.id, resource, callback)
    }

    protected open fun deployInService(callback: (String) -> Unit) {
        this.service.deployArtifact(this.id, callback)
    }
}