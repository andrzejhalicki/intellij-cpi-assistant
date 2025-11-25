import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag


@Service(Service.Level.PROJECT)
@State(
    name = "TenantState",
    storages = [Storage("TenantState.xml")]
)
public class TenantStateComponent : PersistentStateComponent<TenantStateComponent.State> {

    private var state = State()

    data class State(
        var tenants: MutableList<TenantInfo> = mutableListOf()
    )

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun addTenant(tenant: TenantInfo) {
        val attributes = createCredentialAttributes(tenant.url, tenant.clientID)
        val credentials: Credentials = Credentials(tenant.clientID, tenant.clientSecret)
        PasswordSafe.instance.set(attributes, credentials)
        tenant.clientSecret = ""
        state.tenants.add(tenant)
    }

    fun deleteTenant(tenant: TenantInfo) {
        state.tenants.removeIf { it.url == tenant.url }
    }

    fun getTenants(): List<TenantInfo> {
        return this.state.tenants.map { tenant ->
            val attributes = createCredentialAttributes(tenant.url, tenant.clientID)
            val passwordSafe: PasswordSafe = PasswordSafe.instance
            val credentials = passwordSafe[attributes]
            if (credentials != null) {
                tenant.clientSecret = credentials.getPasswordAsString().toString()
                tenant.clientID = credentials.userName.toString()
            }
            tenant
        }
    }

    fun addFavoritePackage(tenantName: String, packageId: String, packageName: String): FavoritePackageInfo? {
        val tenant = state.tenants.find { it.name == tenantName } ?: return null
        val favPackage = tenant.favoritePackages.find { pkg -> pkg.packageId == packageId }
        if (favPackage != null) return null

        val newFavPackage = FavoritePackageInfo(packageId = packageId, packageName = packageName, autoLoad = true)
        tenant.favoritePackages.add(newFavPackage)
        return newFavPackage
    }

    fun removeFavoritePackage(tenantName: String, packageId: String) {
        val tenant = state.tenants.find { it.name == tenantName }
        tenant?.let {
            it.favoritePackages.removeIf { pkg -> pkg.packageId == packageId }
        }
    }

    fun addFavoriteArtifact(
        tenantName: String,
        packageId: String,
        packageName: String,
        artifactId: String,
        artifactName: String
    ): FavoriteArtifactInfo? {
        val tenant = state.tenants.find { it.name == tenantName } ?: return null

        val favoritePackage = tenant.favoritePackages.find { it.packageId == packageId }
            ?: FavoritePackageInfo(packageId = packageId, packageName = packageName, autoLoad = false).also {
                tenant.favoritePackages.add(it)
            }

        val existingArtifact = favoritePackage.favoriteArtifacts.find { it.artifactId == artifactId }
        if (existingArtifact != null) {
            existingArtifact.autoLoad = true
            return null
        }

        val newFavArtifact = FavoriteArtifactInfo(artifactId = artifactId, artifactName = artifactName, autoLoad = true)
        favoritePackage.favoriteArtifacts.add(newFavArtifact)
        return newFavArtifact
    }

    fun removeFavoriteArtifact(tenantName: String, packageId: String, artifactId: String) {
        val tenant = state.tenants.find { it.name == tenantName }
        tenant?.let {
            val favoritePackage = it.favoritePackages.find { pkg -> pkg.packageId == packageId }
            favoritePackage?.let { pkg ->
                pkg.favoriteArtifacts.removeIf { artifact -> artifact.artifactId == artifactId }
            }
        }
    }

    fun addFavoriteResource(
        tenantName: String,
        packageId: String,
        packageName: String,
        artifactId: String,
        artifactName: String,
        resourceId: String,
        resourceName: String
    ): FavoriteResourceInfo? {
        val tenant = state.tenants.find { it.name == tenantName } ?: return null

        val favoritePackage = tenant.favoritePackages.find { it.packageId == packageId }
            ?: FavoritePackageInfo(packageId = packageId, packageName = packageName, autoLoad = false).also {
                tenant.favoritePackages.add(it)
            }

        val favoriteArtifact = favoritePackage.favoriteArtifacts.find { it.artifactId == artifactId }
            ?: FavoriteArtifactInfo(artifactId = artifactId, artifactName = artifactName, autoLoad = false).also {
                favoritePackage.favoriteArtifacts.add(it)
            }

        if (favoriteArtifact.favoriteResources.any { it.resourceName == resourceName }) {
            return null
        }

        val newFavResource = FavoriteResourceInfo(resourceId = resourceId, resourceName = resourceName)
        favoriteArtifact.favoriteResources.add(newFavResource)
        return newFavResource
    }

    fun removeFavoriteResource(tenantName: String, packageId: String, artifactId: String, resourceName: String) {
        val tenant = state.tenants.find { it.name == tenantName }
        tenant?.let {
            val favoritePackage = it.favoritePackages.find { pkg -> pkg.packageId == packageId }
            favoritePackage?.let { pkg ->
                val favoriteArtifact = pkg.favoriteArtifacts.find { artifact -> artifact.artifactId == artifactId }
                favoriteArtifact?.let { artifact ->
                    artifact.favoriteResources.removeIf { resource -> resource.resourceName == resourceName }
                }
            }
        }
    }

    private fun createCredentialAttributes(url: String, clientID: String): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName(url, clientID)
        )
    }
}

@Tag("TenantInfo")
data class TenantInfo(
    @Attribute var name: String = "",
    @Attribute var url: String = "",
    @Attribute var tokenUrl: String = "",
    @Attribute var clientID: String = "",
    @Attribute var clientSecret: String = "",
    @Tag("favoritePackages") var favoritePackages: MutableList<FavoritePackageInfo> = mutableListOf()
)

@Tag("FavoritePackageInfo")
data class FavoritePackageInfo(
    @Attribute var packageId: String = "",
    @Attribute var packageName: String = "",
    @Attribute var autoLoad: Boolean = false,
    @Tag("favoriteArtifacts") var favoriteArtifacts: MutableList<FavoriteArtifactInfo> = mutableListOf()
)

@Tag("FavoriteArtifactInfo")
data class FavoriteArtifactInfo(
    @Attribute var artifactId: String = "",
    @Attribute var artifactName: String = "",
    @Attribute var autoLoad: Boolean = false,
    @Tag("favoriteResources") var favoriteResources: MutableList<FavoriteResourceInfo> = mutableListOf()
)

@Tag("FavoriteResourceInfo")
data class FavoriteResourceInfo(
    @Attribute var resourceId: String = "",
    @Attribute var resourceName: String = ""
)
