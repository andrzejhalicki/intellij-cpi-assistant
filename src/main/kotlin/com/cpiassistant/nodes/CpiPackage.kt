package com.cpiassistant.nodes

import com.cpiassistant.services.CpiService

class CpiPackage(override val id: String, override val name: String, private val service: CpiService, override var isLoaded: Boolean = false): BaseNode() {
    private val artifacts = mutableListOf<CpiArtifact>()
    private val scriptCollections = mutableListOf<CpiScriptCollection>()

    fun getArtifacts(packageId: String, callback: (List<CpiArtifact>) -> Unit) {
        this.service.getArtifacts(packageId) { p ->
            this.artifacts.addAll(p)
            callback(this.artifacts)
        }
    }

    fun getScriptCollections(packageId: String, callback: (List<CpiScriptCollection>) -> Unit) {
        this.service.getScriptCollections(packageId) { p ->
            this.scriptCollections.addAll(p)
            callback(this.scriptCollections)
        }
    }
}