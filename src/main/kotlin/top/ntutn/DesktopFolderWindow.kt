package top.ntutn

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
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
import top.ntutn.util.IconUtil
import java.awt.Window
import java.io.File

@Composable
fun DesktopFolderWindow(folderFile: File, onCloseWindowRequest: () -> Unit = {}, onPlacingWindow: (Window) -> Unit) {
    val dialogState = rememberDialogState()
    var icon by remember { mutableStateOf<Painter?>(null) }
    DialogWindow(
        onCloseRequest = onCloseWindowRequest,
        resizable = false,
        undecorated = false,
        transparent = false,
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
            val childrenFilesState = remember { mutableStateListOf<File>() }
            DesktopFolderScreen(childrenFiles = childrenFilesState)
            LaunchedEffect(folderFile) {
                val children = withContext(Dispatchers.IO) {
                    folderFile.listFiles { file -> !file.isDirectory} ?: emptyArray()
                }
                childrenFilesState.addAll(children)
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
    DesktopFolderScreen(childrenFiles = childrenFiles.toList())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopFolderScreen(modifier: Modifier = Modifier.fillMaxSize(), childrenFiles: List<File>) {
    Box(modifier = modifier.padding(8.dp)) {
        FlowRow(modifier = Modifier.verticalScroll(rememberScrollState())) {
//            Text(folderFile.name)
//            Button(onClick = {
//                Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
//                    it.lpFile = folderFile.absolutePath
//                    it.nShow = User32.SW_SHOW
//                    it.fMask = 0x0000000c
//                    it.lpVerb = "open"
//                })
//            }) {
//                Text("Launch")
//            }
//            Button(onClick = {
//                Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
//                    it.lpFile = folderFile.absolutePath
//                    it.nShow = User32.SW_SHOW
//                    it.fMask = 0x0000000c
//                    it.lpVerb = "properties"
//                })
////                        Shell32.INSTANCE.ExtractAssociatedIconEx()
//            }) {
//                Text("Properties")
//            }
            childrenFiles.forEach { childFile ->
                DesktopFileCard(childFile)
                Spacer(Modifier.size(8.dp))
            }
        }
    }
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

    Column(modifier = modifier
        .width(64.dp)
        .combinedClickable(onDoubleClick = {
        Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
            it.lpFile = file.absolutePath
            it.nShow = User32.SW_SHOW
            it.fMask = 0x0000000c
            it.lpVerb = "open"
        })
    }, onClick = {

    })) {
        val icon = iconPainter
        Image(modifier = Modifier.size(64.dp), painter = icon, contentDescription = file.name)
        Text(modifier = Modifier, text = fileName, maxLines = 2, overflow = TextOverflow.Ellipsis)
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