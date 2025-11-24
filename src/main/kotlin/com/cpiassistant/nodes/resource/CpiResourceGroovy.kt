package com.cpiassistant.nodes.resource

import com.cpiassistant.nodes.NodeType

class CpiResourceGroovy(id: String, name: String, path: String = "", parent: String, isLoaded: Boolean = false): CpiResource(id, name, path, parent, isLoaded) {
    override val type = NodeType.GROOVY_RESOURCE
    override val resourceType = ResourceType.GROOVY
}