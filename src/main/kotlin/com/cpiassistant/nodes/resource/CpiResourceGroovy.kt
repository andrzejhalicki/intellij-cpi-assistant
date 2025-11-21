package com.cpiassistant.nodes.resource

class CpiResourceGroovy(id: String, name: String, path: String = "", parent: String, isLoaded: Boolean = false): CpiResource(id, name, path, parent, isLoaded) {
    override val type = "GroovyResource"
    override val resourceType = ResourceType.GROOVY
}