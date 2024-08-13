package com.cpiassistant.nodes

class CpiResource(override val id: String, override val name: String, var path: String = "", val parent: String, override var isLoaded: Boolean = false): BaseNode() {

}