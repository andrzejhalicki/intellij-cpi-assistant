package com.cpiassistant.nodes.resource

import com.cpiassistant.nodes.BaseNode

enum class ResourceType(val value: String, val fileExtension: String) {
    GROOVY("groovy", ".groovy"),
    XSLT("xslt", ".xslt"),
    UNKNOWN("", "")
}

open class CpiResource(override val id: String, override val name: String, open var path: String = "", open val parent: String, override var isLoaded: Boolean = false): BaseNode() {
    override val type = "Resource"
    open val resourceType = ResourceType.UNKNOWN

    companion object {
        fun create(id: String, name: String, path: String = "", parent: String, isLoaded: Boolean = false): CpiResource {
            return when {
                name.endsWith(".groovy", ignoreCase = true) ->
                    CpiResourceGroovy(id, name, path, parent, isLoaded)
                name.endsWith(".xsl", ignoreCase = true) || name.endsWith(".xslt", ignoreCase = true) ->
                    CpiResourceXslt(id, name, path, parent, isLoaded)
                else ->
                    CpiResource(id, name, path, parent, isLoaded)
            }
        }
    }

}