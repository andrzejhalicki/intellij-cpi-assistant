package com.cpiassistant.nodes.resource

import com.cpiassistant.nodes.NodeType

class CpiResourceXslt(id: String, name: String, path: String = "", parent: String, isLoaded: Boolean = false): CpiResource(id, name, path, parent, isLoaded) {
    override val type = NodeType.XSLT_RESOURCE
    override val resourceType = ResourceType.XSLT
}