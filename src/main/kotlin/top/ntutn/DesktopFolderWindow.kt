package top.ntutn

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import top.ntutn.util.IconUtil
import java.awt.Window
import java.io.File
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DesktopFolderWindow(folderFile: File, onCloseWindowRequest: () -> Unit = {}, onPlacingWindow: (Window) -> Unit) {
    val dialogState = rememberDialogState()
    var icon by remember { mutableStateOf<Painter?>(null) }
    DialogWindow(
        onCloseRequest = onCloseWindowRequest,
        resizable = false,
        undecorated = true,
        transparent = true,
        title = folderFile.name,
        state = dialogState,
        icon = icon
    ) {
        LaunchedEffect(window) {
            insertWindowToDesktop(HWND(Pointer(window.windowHandle)))
            onPlacingWindow(window)
            icon = IconUtil.getFileIconPainter(folderFile, 64.dp, 64.dp)
        }
        MaterialTheme {
            val openFunction: () -> Unit = {
                Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
                    it.lpFile = folderFile.absolutePath
                    it.nShow = User32.SW_SHOW
                    it.fMask = 0x0000000c
                    it.lpVerb = if (folderFile.isDirectory) "explore" else "open"
                    it.hwnd = HWND(Pointer(window.windowHandle)) // https://www.experts-exchange.com/questions/10299966/Application-hangs-after-ShellExecute.html
                })
            }
            val childrenFilesState = remember { mutableStateListOf<File>() }
            FileContextMenu(folderFile, openFunction) {
                Column {
                    WindowDraggableArea {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .border(width = 1.dp, color = Color.DarkGray)
                                .background(color = Color(0xdd999999))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(folderFile.nameWithoutExtension)
                        }
                    }
                    DesktopFolderScreen(
                        folderFile.nameWithoutExtension,
                        childrenFiles = childrenFilesState,
                        modifier = Modifier.fillMaxSize()
                            .onExternalDrag(onDrop = { externalDragValue ->
                                when (val dragData = externalDragValue.dragData) {
                                    is DragData.Text -> {
                                        val text = dragData.readText()
                                        println("Saving drop text")
                                        val outFile = File(folderFile, UUID.randomUUID().toString() + ".txt")
                                        outFile.writeText(text)
                                    }
                                    is DragData.FilesList -> {
                                        dragData.readFiles().forEach {
                                            println("Receiving $it")
                                        }
                                    }
                                }
                            })
                    )
                }
            }
            LaunchedEffect(folderFile) {
                val children = withContext(Dispatchers.IO) {
                    folderFile.listFiles { file -> !file.isHidden} ?: emptyArray()
                }
                childrenFilesState.clear()
                childrenFilesState.addAll(children)
            }
            DisposableEffect(folderFile) {
                val monitor = FileAlterationMonitor(3_000L, FileAlterationObserver(folderFile).also {
                    it.addListener(object : FileAlterationListenerAdaptor() {
                        override fun onDirectoryCreate(directory: File?) {
                            super.onDirectoryCreate(directory)
                            updateFile()
                        }

                        override fun onDirectoryChange(directory: File?) {
                            super.onDirectoryChange(directory)
                            updateFile()
                        }

                        override fun onDirectoryDelete(directory: File?) {
                            super.onDirectoryDelete(directory)
                            updateFile()
                        }

                        override fun onFileChange(file: File?) {
                            super.onFileChange(file)
                            updateFile()
                        }

                        override fun onFileCreate(file: File?) {
                            super.onFileCreate(file)
                            updateFile()
                        }

                        override fun onFileDelete(file: File?) {
                            super.onFileDelete(file)
                            updateFile()
                        }

                        private fun updateFile() {
                            val files = folderFile.listFiles { file -> !file.isHidden }?.toList() ?: emptyList<File>()
                            childrenFilesState.clear()
                            childrenFilesState.addAll(files)
                        }
                    })
                })

                monitor.start()
                onDispose {
                    monitor.stop()
                }
            }
        }
    }
}

@Composable
@Preview
private fun DesktopFolderScreenPreview() {
    val childrenFiles = listOf(
        File("src/main/resources/demo-folder/Hello.html"),
        File("src/main/resources/demo-folder/Hello.md"),
    )
    DesktopFolderScreen("Demo", childrenFiles = childrenFiles.toList())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopFolderScreen(currentDirName: String, childrenFiles: List<File>, modifier: Modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = modifier
        .background(Color(0xaa999999))
    ) {
        Box(
            modifier = Modifier
        ) {
            val state = rememberScrollState()
            FlowRow(
                modifier = Modifier.verticalScroll(state)
                    .padding(8.dp)
            ) {
                childrenFiles.forEach { childFile ->
                    DesktopFileCard(childFile)
                    Spacer(Modifier.size(8.dp))
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = state
                )
            )
        }

    }
}

@Composable
private fun FileContextMenu(file: File, openFunction: () -> Unit, content: @Composable () -> Unit) {
    ContextMenuArea(items = {
        listOf(
            ContextMenuItem(if (file.isDirectory) "浏览..." else "打开", openFunction),
            ContextMenuItem("属性") {
                Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
                    it.lpFile = file.absolutePath
                    it.nShow = User32.SW_SHOW
                    it.fMask = 0x0000000c
                    it.lpVerb = "properties"
                })
            }
        )
    }, content = content)
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopFileCard(file: File, modifier: Modifier = Modifier) {
    var iconPainter by remember { mutableStateOf(IconUtil.emptyPainter) }
    val fileName by remember { derivedStateOf {
        if (file.extension == "lnk" || file.extension == "url") {
            file.nameWithoutExtension
        } else {
            file.name
        }
    } }
    val openFunction: () -> Unit = { // fixme 这个API有时好像会导致卡死，需要异步+超时处理
        Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
            it.lpFile = file.absolutePath
            it.nShow = User32.SW_SHOW
            it.fMask = 0x0000000c
            it.lpVerb = if (file.isDirectory) "explore" else "open"
        })
    }
    FileContextMenu(file, openFunction) {
        TooltipArea(tooltip = {
            // composable tooltip content
            Surface(
                modifier = Modifier.shadow(4.dp),
            ) {
                Text(
                    text = fileName,
                    modifier = Modifier//.padding(10.dp)
                )
            }
        },
            modifier = modifier,
            delayMillis = 600, // in milliseconds
            tooltipPlacement = TooltipPlacement.CursorPoint(
                alignment = Alignment.BottomEnd,
                offset = DpOffset.Zero // tooltip offset
            )) {
            Column(modifier = Modifier
                .width(64.dp)
                .combinedClickable(onDoubleClick = openFunction, onClick = {

                })) {
                val icon = iconPainter
                Image(modifier = Modifier.size(64.dp), painter = icon, contentDescription = file.name)
                Text(modifier = Modifier, text = fileName, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    LaunchedEffect(file) {
        iconPainter = IconUtil.getFileIconPainter(file, 64.dp, 64.dp)
    }
}

private fun insertWindowToDesktop(childHwnd: HWND) {

    // find defView in Program
    val program: HWND? = User32.INSTANCE.FindWindowEx(HWND(Pointer.NULL), HWND(Pointer.NULL), "Progman", null)
    var defView: HWND? = User32.INSTANCE.FindWindowEx(program, HWND(Pointer.NULL), "SHELLDLL_DefView", null)
    var container = program.takeIf { program?.pointer != null && defView?.pointer != null }

    if (container == null) {
        // find defView in WorkerW
        val desktopHwnd = User32.INSTANCE.GetDesktopWindow()
        var workerW: HWND? = HWND(Pointer.NULL)
        do {
            workerW = User32.INSTANCE.FindWindowEx(desktopHwnd, workerW, "WorkerW", null)
            defView = User32.INSTANCE.FindWindowEx(workerW, HWND(Pointer.NULL), "SHELLDLL_DefView", null);
        } while (defView?.pointer == Pointer.NULL && workerW?.pointer != Pointer.NULL)
        container = workerW
    }

    User32.INSTANCE.SetParent(childHwnd, container)
}