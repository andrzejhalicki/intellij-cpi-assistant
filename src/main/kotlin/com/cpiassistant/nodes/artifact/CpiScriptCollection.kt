package com.cpiassistant.nodes.artifact

import com.cpiassistant.nodes.resource.CpiResource
import com.cpiassistant.services.CpiService

class CpiScriptCollection(override val id: String, override val name: String, override val service: CpiService,
                          override var isLoaded: Boolean = false
): CpiArtifact(id, name, service, isLoaded) {

    override fun getResourcesFromService(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        this.service.getScriptCollectionResources(artifactId, callback)
    }

    override fun createResourceInService(resource: CpiResource, content: String, callback: (Boolean) -> Unit) {
        this.service.createScriptCollectionResource(this.id, resource, content, callback)
    }

    override fun updateResourceInService(resource: CpiResource, content: String, callback: (Boolean, String) -> Unit) {
        this.service.updateScriptCollectionResource(this.id, resource.name, content, callback)
    }

    override fun downloadResourceFromService(resource: CpiResource, callback: (String) -> Unit) {
        this.service.getScriptCollectionResource(this.id, resource.name, callback)
    }

    override fun deployInService(callback: (String) -> Unit) {
        this.service.deployScriptCollection(this.id, callback)
    }
}