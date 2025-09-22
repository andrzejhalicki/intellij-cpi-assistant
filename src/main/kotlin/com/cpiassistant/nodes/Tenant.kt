package com.cpiassistant.nodes

import TenantStateComponent
import com.cpiassistant.services.CpiService
import kotlin.collections.mutableListOf

class Tenant(override val id: String, override val name: String, cpiService: CpiService, favoritePackages: List<String> = mutableListOf<String>(),
             val tenantStateComponent: TenantStateComponent, override var isLoaded: Boolean = false) : BaseNode() {

    val service: CpiService = cpiService;
    val packages: MutableList<CpiPackage> = mutableListOf<CpiPackage>();
    var isConnected: Boolean = false
    val favoritePackages: MutableList<CpiPackage> = favoritePackages.map { CpiPackage(it, it, this.service, true) }.toMutableList()

    fun getPackages(callback: (List<CpiPackage>) -> Unit) {
        this.service.getPackages { p ->
            this.packages.addAll(p)
            callback(this.packages)
        }
    }

    fun addPackageToFavorites(cpiPackage: CpiPackage, callback: (CpiPackage) -> Unit) {
        if(!this.favoritePackages.any { it.id == cpiPackage.id }) {
            this.favoritePackages.add(cpiPackage)
            tenantStateComponent.addFavoritePackage(this.id, cpiPackage.id)
            callback(cpiPackage)
        }
    }

    fun removePackageFromFavorites(cpiPackage: CpiPackage) {
        this.favoritePackages.removeIf { it.id == cpiPackage.id }
        tenantStateComponent.removeFavoritePackage(this.id, cpiPackage.id)
    }

    fun isFavorite(cpiPackage: CpiPackage): Boolean {
        return this.favoritePackages.any { it.id == cpiPackage.id }
    }

}