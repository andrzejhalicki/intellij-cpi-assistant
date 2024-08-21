import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.awt.Color
import java.awt.Component
import javax.swing.*
import javax.swing.border.LineBorder

class AddTenantDialog : DialogWrapper(true) {
    private val name: JTextField = JBTextField()
    private val serviceKey: JBTextArea = JBTextArea(10,40)

    private lateinit var url: String
    private lateinit var tokenUrl: String
    private lateinit var clientId: String
    private lateinit var clientSecret: String
    private val project: Project? = ProjectManager.getInstance().openProjects.firstOrNull()

    private val tenantStateComponent = project?.service<TenantStateComponent>()
    private val tenants = tenantStateComponent?.getTenants()

    init {
        init()
        title = "Add tenant"
        serviceKey.border = LineBorder(Color.GRAY, 1)
        serviceKey.lineWrap = true
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val nameLabel = JLabel("Name:")

        nameLabel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(nameLabel)

        name.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(name)

        val serviceKeyLabel = JLabel("Service Key:")

        serviceKeyLabel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(serviceKeyLabel)

        serviceKey.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(serviceKey)

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
        var jsonString = serviceKey.text.trim()
        jsonString = jsonString.lines().joinToString("").filterNot { it.isWhitespace() }
        val json = Json { ignoreUnknownKeys = true }
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val oauth = jsonObject["oauth"] as JsonObject
        url = oauth["url"].toString().replace("\"","")
        tokenUrl = oauth["tokenurl"].toString().replace("\"","")
        clientId = oauth["clientid"].toString().replace("\"","")
        clientSecret = oauth["clientsecret"].toString().replace("\"","")

        if (name.isEmpty() || url.isEmpty() || tokenUrl.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty()) {
            setErrorText("Please fill all fields", null)
            return false
        }

        val existingTenant = tenants?.find { it.name == name }
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
        return url
    }

    fun getTokenUrl(): String {
        return tokenUrl
    }

    fun getClientId(): String {
        return clientId
    }

    fun getClientSecret(): String {
        return clientSecret
    }
}