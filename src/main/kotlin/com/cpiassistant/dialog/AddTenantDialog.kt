import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import javax.swing.*

class AddTenantDialog : DialogWrapper(true) {
    private val name: JTextField = JBTextField()
    private val url: JTextField = JBTextField()
    private val tokenUrl: JTextField = JBTextField()
    private val clientId: JTextField = JBTextField()
    private val clientSecret: JTextField = JBTextField()

    private val tenantStateComponent = service<TenantStateComponent>()
    private val tenants = tenantStateComponent.getTenants()

    init {
        init()
        title = "Enter Data"
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val nameLabel = JLabel("Name:")
        panel.add(nameLabel)
        panel.add(name)

        val urlLabel = JLabel("URL:")
        panel.add(urlLabel)
        panel.add(url)

        val tokenUrlLabel = JLabel("Token URL:")
        panel.add(tokenUrlLabel)
        panel.add(tokenUrl)

        val clientIdLabel = JLabel("Client ID:")
        panel.add(clientIdLabel)
        panel.add(clientId)

        val clientSecretLabel = JLabel("Clilent Secret:")
        panel.add(clientSecretLabel)
        panel.add(clientSecret)

        return panel
    }

    override fun doOKAction() {
        if (!validateInput()) {
            return
        }
        super.doOKAction()
    }

    private fun validateInput(): Boolean {
        val name = name.text.trim()
        val url = url.text.trim()
        val tokenUrl = tokenUrl.text.trim()
        val clientId = clientId.text.trim()
        val clientSecret = clientSecret.text.trim()

        if (name.isEmpty() || url.isEmpty() || tokenUrl.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty()) {
            setErrorText("Please fill all fields", null)
            return false
        }

        val existingTenant = tenants.find { it.name == name }
        if(existingTenant != null) {
            setErrorText("Tenant with this name already exists", null)
            return false
        }
        return true
    }

    fun getName(): String {
        return name.text
    }

    fun getURL(): String {
        return url.text
    }

    fun getTokenUrl(): String {
        return tokenUrl.text
    }

    fun getClientId(): String {
        return clientId.text
    }

    fun getClientSecret(): String {
        return clientSecret.text
    }
}