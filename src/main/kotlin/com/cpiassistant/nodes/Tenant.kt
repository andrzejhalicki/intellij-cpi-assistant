package com.cpiassistant.nodes

import com.cpiassistant.services.CpiService

class Tenant(override val id: String, override val name: String, cpiService: CpiService, override var isLoaded: Boolean = false) : BaseNode() {

    val service: CpiService = cpiService;
    val packages: MutableList<CpiPackage> = mutableListOf<CpiPackage>();
    var isConnected: Boolean = false

    fun getPackages(callback: (List<CpiPackage>) -> Unit) {
        this.service.getPackages { p ->
            this.packages.addAll(p)
            callback(this.packages)
        }
    }

}