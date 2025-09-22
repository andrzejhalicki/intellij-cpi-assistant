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
        if (attributes != null) {
            PasswordSafe.instance.set(attributes, credentials)
        }
        tenant.clientSecret = ""
        state.tenants.add(tenant)
    }

    fun deleteTenant(tenant: TenantInfo) {
        state.tenants.removeIf { it.url == tenant.url }
    }

    fun getTenants(): List<TenantInfo> {
        return this.state.tenants.filter { tenant -> true }.map { tenant ->
            val attributes = createCredentialAttributes(tenant.url, tenant.clientID)
            val passwordSafe: PasswordSafe = PasswordSafe.instance

            if (attributes != null) {
                val credentials = passwordSafe[attributes]
                if (credentials != null) {
                    tenant.clientSecret = credentials.getPasswordAsString().toString()
                    tenant.clientID = credentials.userName.toString()
                }
            }
            tenant
        }
    }

    fun addFavoritePackage(tenantName: String, packageId: String) {
        val tenant = state.tenants.find { it.name == tenantName }
        tenant?.let {
            if (!it.favoritePackages.contains(packageId)) {
                it.favoritePackages.add(packageId)
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
    @Tag("favoritePackages") var favoritePackages: MutableList<String> = mutableListOf()
)
