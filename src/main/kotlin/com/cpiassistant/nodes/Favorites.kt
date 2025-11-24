package com.cpiassistant.nodes

class Favorites(override val id: String = "Favorites", override val name: String = "Favorites", override var isLoaded: Boolean = true): BaseNode() {
    override var isLoading: Boolean = false
    override val type = NodeType.FAVORITES
}