package com.cpiassistant.nodes

abstract class BaseNode {

    abstract val name: String;
    abstract val id: String
    abstract var isLoaded: Boolean
    abstract var isLoading: Boolean
    abstract val type: NodeType
}

enum class NodeType(val value: String) {
    TENANT("Tenant"),
    PACKAGE("Package"),
    SCRIPT_COLLECTION("ScriptCollection"),
    RESOURCE("Resource"),
    GROOVY_RESOURCE("GroovyResource"),
    XSLT_RESOURCE("XsltResource"),
    FAVORITES("Favorites"),
    IFLOW("IFlow"),
    UNKNOWN("")
}