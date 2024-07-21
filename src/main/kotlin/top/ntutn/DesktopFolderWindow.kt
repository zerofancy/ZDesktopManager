package top.ntutn

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
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
import java.awt.AlphaComposite
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.filechooser.FileSystemView

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
            // 获取桌面图标的方式
            val dirIcon = FileSystemView.getFileSystemView().getSystemIcon(folderFile)

            val img = BufferedImage(dirIcon.iconWidth, dirIcon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g2d = img.createGraphics()
            g2d.composite = AlphaComposite.SrcOver.derive(0.5f)
            dirIcon.paintIcon(null, g2d, 0, 0)
            g2d.dispose()

            icon = img.toPainter()
        }
        MaterialTheme {
            DesktopFolderScreen(folderFile = folderFile)
        }
    }
}

@Composable
@Preview
private fun DesktopFolderScreenPreview() {
    DesktopFolderScreen(folderFile = File("src/main/resources/demo-folder"))
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun DesktopFolderScreen(modifier: Modifier = Modifier.fillMaxSize(), folderFile: File = File("")) {
    val childrenFilesState = remember { mutableStateListOf<File>() }
    Box(modifier = modifier) {
        FlowRow {
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
            childrenFilesState.forEach { childFile ->
                Text(modifier = Modifier.combinedClickable(onDoubleClick = {
                    Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
                        it.lpFile = childFile.absolutePath
                        it.nShow = User32.SW_SHOW
                        it.fMask = 0x0000000c
                        it.lpVerb = "open"
                    })
                }, onClick = {

                }), text = childFile.name)
                Spacer(Modifier.size(8.dp))
            }
        }
        LaunchedEffect(folderFile) {
            val children = withContext(Dispatchers.IO) {
                folderFile.listFiles { file -> !file.isDirectory} ?: emptyArray()
            }
            childrenFilesState.addAll(children)
        }
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