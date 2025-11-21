package com.cpiassistant.nodes.resource

class CpiResourceXslt(id: String, name: String, path: String = "", parent: String, isLoaded: Boolean = false): CpiResource(id, name, path, parent, isLoaded) {
    override val type = "XsltResource"
    override val resourceType = ResourceType.XSLT
}