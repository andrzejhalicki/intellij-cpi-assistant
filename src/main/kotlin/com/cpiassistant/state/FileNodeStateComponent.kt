import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

@Service
@State(
    name = "FileNodeState",
    storages = [Storage("FileNodeState.xml")]
)
class FileNodeStateComponent : PersistentStateComponent<FileNodeStateComponent.State> {

    private var state = State()

    data class State(
        var fileNodes: MutableList<FileNodeInfo> = mutableListOf()
    )

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun addFileNode(fileNodeInfo: FileNodeInfo) {
        state.fileNodes.add(fileNodeInfo)
    }

    fun getFileNodes(): List<FileNodeInfo> {
        return state.fileNodes
    }
}

@Tag("FileNodeInfo")
data class FileNodeInfo(
    @Attribute var name: String = "",
    @Attribute var path: String = "",
    @Attribute var artifactId: String = "")
