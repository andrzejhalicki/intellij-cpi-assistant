import com.cpiassistant.nodes.BaseNode
import com.intellij.openapi.actionSystem.DataProvider

class CustomDataProvider(private val baseNode: Any) : DataProvider {
    override fun getData(dataId: String): Any? {
        return baseNode
    }
}