package com.cpiassistant.nodes

import FavoriteArtifactInfo
import FavoritePackageInfo
import TenantStateComponent
import com.cpiassistant.nodes.artifact.CpiArtifact
import com.cpiassistant.nodes.resource.CpiResource
import com.cpiassistant.services.CpiService
import kotlin.collections.mutableListOf

class Tenant(
    override val id: String,
    override val name: String,
    cpiService: CpiService,
    favoritePackages: List<FavoritePackageInfo> = mutableListOf(),
    val tenantStateComponent: TenantStateComponent,
    override var isLoaded: Boolean = false
) : BaseNode() {

    override var isLoading: Boolean = false
    override var autoLoad: Boolean = false
    override val type = NodeType.TENANT
    val service: CpiService = cpiService;
    val packages: MutableList<CpiPackage> = mutableListOf<CpiPackage>();
    var isConnected: Boolean = false
    val favoritePackages: MutableList<FavoritePackageInfo> =
        favoritePackages.toMutableList()

    fun getPackages(callback: (List<CpiPackage>) -> Unit) {
        this.service.getPackages { p ->
            this.packages.addAll(p)
            callback(this.packages)
        }
    }

    fun addPackageToFavorites(cpiPackage: CpiPackage, callback: (CpiPackage) -> Unit) {
        val favPackage = tenantStateComponent.addFavoritePackage(this.id, cpiPackage.id, cpiPackage.name)
        favPackage?.let { favoritePackage ->
            this.favoritePackages.add(favoritePackage)
            callback(CpiPackage(cpiPackage.id, cpiPackage.name, this.service))
        }
    }

    fun removePackageFromFavorites(cpiPackage: CpiPackage) {
        this.favoritePackages.removeIf { it.packageId == cpiPackage.id }
        tenantStateComponent.removeFavoritePackage(this.id, cpiPackage.id)
    }

    fun addArtifactToFavorites(cpiPackage: CpiPackage, artifact: CpiArtifact, callback: (CpiArtifact) -> Unit) {
        val favArtifact = tenantStateComponent.addFavoriteArtifact(
            this.id,
            cpiPackage.id,
            cpiPackage.name,
            artifact.id,
            artifact.name
        )
            ?: return

        val localPackage = this.favoritePackages.find { it.packageId == cpiPackage.id }
            ?: FavoritePackageInfo(packageId = cpiPackage.id, packageName = cpiPackage.name).also {
                this.favoritePackages.add(it)
            }

        localPackage.favoriteArtifacts.add(favArtifact)
        callback(CpiArtifact(artifact.id, artifact.name, this.service))
    }

    fun removeArtifactFromFavorites(cpiPackage: CpiPackage, artifact: CpiArtifact) {
        this.favoritePackages.find { it.packageId == cpiPackage.id }?.let { pkg ->
            pkg.favoriteArtifacts.removeIf { it.artifactId == artifact.id }
            tenantStateComponent.removeFavoriteArtifact(this.id, cpiPackage.id, artifact.id)
        }
    }

    fun addResourceToFavorites(
        cpiPackage: CpiPackage,
        artifact: CpiArtifact,
        resource: CpiResource,
        callback: (CpiResource) -> Unit
    ) {
        val favResource = tenantStateComponent.addFavoriteResource(
            this.id, cpiPackage.id, cpiPackage.name,
            artifact.id, artifact.name,
            resource.id, resource.name
        ) ?: return

        val localPackage = this.favoritePackages.find { it.packageId == cpiPackage.id }
            ?: FavoritePackageInfo(packageId = cpiPackage.id, packageName = cpiPackage.name).also {
                this.favoritePackages.add(it)
            }

        val localArtifact = localPackage.favoriteArtifacts.find { it.artifactId == artifact.id }
            ?: FavoriteArtifactInfo(artifactId = artifact.id, artifactName = artifact.name).also {
                localPackage.favoriteArtifacts.add(it)
            }

        localArtifact.favoriteResources.add(favResource)
        callback(CpiResource.create(resource.id, resource.name, resource.path, artifact.id, true))
    }

    fun removeResourceFromFavorites(cpiPackage: CpiPackage, artifact: CpiArtifact, resource: CpiResource) {
        this.favoritePackages.find { it.packageId == cpiPackage.id }?.let { pkg ->
            pkg.favoriteArtifacts.find { it.artifactId == artifact.id }?.let { art ->
                art.favoriteResources.removeIf { it.resourceName == resource.name }
                tenantStateComponent.removeFavoriteResource(this.id, cpiPackage.id, artifact.id, resource.name)
            }
        }
    }

    fun isFavorite(cpiPackage: CpiPackage): Boolean {
        return this.favoritePackages.any { it.packageId == cpiPackage.id }
    }

    fun isArtifactFavorite(cpiPackage: CpiPackage, artifact: CpiArtifact): Boolean {
        return this.favoritePackages.find { it.packageId == cpiPackage.id }
            ?.favoriteArtifacts?.any { it.artifactId == artifact.id } ?: false
    }

    fun isResourceFavorite(cpiPackage: CpiPackage, artifact: CpiArtifact, resource: CpiResource): Boolean {
        return this.favoritePackages.find { it.packageId == cpiPackage.id }
            ?.favoriteArtifacts?.find { it.artifactId == artifact.id }
            ?.favoriteResources?.any { it.resourceName == resource.name } ?: false
    }

}
