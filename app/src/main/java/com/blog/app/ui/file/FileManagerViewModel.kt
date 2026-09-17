package com.blog.app.ui.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blog.app.data.model.file.FileDirectory
import com.blog.app.data.model.file.FileItem
import com.blog.app.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 文件管理页面状态。
 */
data class FileManagerUiState(
    val path: String? = null,
    val directories: List<FileDirectory> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 文件管理页面业务逻辑。
 */
class FileManagerViewModel(
    private val repository: FileRepository = FileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    /**
     * 加载当前目录。
     */
    fun load(path: String? = _uiState.value.path) {
        _uiState.value = _uiState.value.copy(path = path, isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { repository.list(path) }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        path = path,
                        directories = result.directories,
                        files = result.files,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "文件列表加载失败"
                    )
                }
        }
    }

    /**
     * 进入子目录。
     */
    fun enter(directory: FileDirectory) {
        load(directory.path)
    }

    /**
     * 返回上一级目录。
     */
    fun goBack(): Boolean {
        val path = _uiState.value.path ?: return false
        if (path.isBlank() || path == "/") {
            return false
        }
        val parent = path.trimEnd('/').substringBeforeLast('/', "")
        load(parent.ifBlank { null })
        return true
    }

    /**
     * 创建目录。
     */
    fun createDirectory(name: String, onFinished: () -> Unit = {}) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.createDirectory(_uiState.value.path, cleanName) }
                .onSuccess {
                    load(_uiState.value.path)
                    onFinished()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "创建目录失败")
                }
        }
    }

    /**
     * 删除目录。
     */
    fun deleteDirectory(directory: FileDirectory) {
        viewModelScope.launch {
            runCatching { repository.deleteDirectory(_uiState.value.path.orEmpty(), directory.name) }
                .onSuccess { load(_uiState.value.path) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "删除目录失败")
                }
        }
    }

    /**
     * 删除文件。
     */
    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            runCatching { repository.deleteFiles(listOf(file.id)) }
                .onSuccess { load(_uiState.value.path) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "删除文件失败")
                }
        }
    }
}
