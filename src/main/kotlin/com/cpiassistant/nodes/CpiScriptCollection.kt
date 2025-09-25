package com.cpiassistant.nodes

import com.cpiassistant.services.CpiService

class CpiScriptCollection(override val id: String, override val name: String, override val service: CpiService,
                          override var isLoaded: Boolean = false
): CpiArtifact(id, name, service, isLoaded) {

    override fun getResourcesFromService(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        this.service.getScriptCollectionResources(artifactId, callback)
    }

    override fun createResourceInService(name: String, content: String, callback: (Boolean) -> Unit) {
        this.service.createScriptCollectionResource(this.id, name, content, callback)
    }

    override fun updateResourceInService(name: String, content: String, callback: (Boolean, String) -> Unit) {
        this.service.updateScriptCollectionResource(this.id, name, content, callback)
    }

    override fun downloadResourceFromService(name: String, callback: (String) -> Unit) {
        this.service.getScriptCollectionResource(this.id, name, callback)
    }

    override fun deployInService(callback: (String) -> Unit) {
        this.service.deployScriptCollection(this.id, callback)
    }
}