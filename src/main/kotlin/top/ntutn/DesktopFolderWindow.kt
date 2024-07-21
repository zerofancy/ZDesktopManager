package top.ntutn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
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
        var text by remember { mutableStateOf("Hello, World!") }
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xaa9CCC65))) {
                Column {
                    Text(folderFile.name)
                    Button(onClick = {
                        Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
                            it.lpFile = folderFile.absolutePath
                            it.nShow = User32.SW_SHOW
                            it.fMask = 0x0000000c
                            it.lpVerb = "open"
                        })
                    }) {
                        Text("Launch")
                    }
                    Button(onClick = {
                        text = "Exit"
                        Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
                            it.lpFile = folderFile.absolutePath
                            it.nShow = User32.SW_SHOW
                            it.fMask = 0x0000000c
                            it.lpVerb = "properties"
                        })
//                        Shell32.INSTANCE.ExtractAssociatedIconEx()
                    }) {
                        Text("Properties")
                    }
                }

            }

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