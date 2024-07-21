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
import androidx.compose.ui.window.*
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.AlphaComposite
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.filechooser.FileSystemView


fun main() {
    val desktopDir = Shell32Util.getSpecialFolderPath(ShlObj.CSIDL_DESKTOP, false).let(::File)
    val subDirs = desktopDir.listFiles { file -> file.isDirectory }?.toMutableList() ?: mutableListOf()

    if (subDirs.isEmpty()) {
        val demoDir = File(desktopDir, "ZDesktop")
        demoDir.mkdir()
        // todo 插入一篇说明文档
        subDirs.add(demoDir)
    }
    println(subDirs.joinToString())

    val dimension: Dimension = Toolkit.getDefaultToolkit().getScreenSize()

    val width = dimension.width
    val height = dimension.height

    println("Screen [$width * $height]")
    application {
        var nextWindowTop = 16
        var nextWindowRight = 16
        subDirs.forEachIndexed { dirIndex, dir ->
            val dialogState = rememberDialogState()
            var icon by remember { mutableStateOf<Painter?>(null) }
            DialogWindow(
                onCloseRequest = ::exitApplication,
                resizable = false,
                undecorated = false,
                transparent = false,
                title = dir.name,
                state = dialogState,
                icon = icon
            ) {
                LaunchedEffect(window) {
                    insertWindowToDesktop(HWND(Pointer(window.windowHandle)))
                    println(window.size)
                    window.setLocation(width - nextWindowRight - window.width, nextWindowTop)
                    nextWindowTop += window.height
                    nextWindowTop += 16
                    if (nextWindowTop + window.height > height) {
                        // 这一列放不下，左边继续排
                        nextWindowTop = 0
                        nextWindowRight += window.width + 16
                    }
                    // 获取桌面图标的方式
                    val dirIcon = FileSystemView.getFileSystemView().getSystemIcon(dir)

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
                            Text(dir.name)
                            Button(onClick = {
                                Shell32.INSTANCE.ShellExecuteEx(ShellAPI.SHELLEXECUTEINFO().also {
                                    it.lpFile = dir.absolutePath
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
                                    it.lpFile = dir.absolutePath
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