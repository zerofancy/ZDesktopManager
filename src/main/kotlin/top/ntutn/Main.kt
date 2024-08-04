package top.ntutn

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.ShlObj
import com.sun.jna.platform.win32.WinDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import top.ntutn.util.ApplicationUtil
import top.ntutn.util.IconUtil
import top.ntutn.util.User32Extend
import top.ntutn.util.User32ExtendFlags
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.Window
import java.io.File


fun main() {
    val isFirstInstance = ApplicationUtil.ensureSingleInstance("top.ntutn.KDesktopManager")

    if (!isFirstInstance) {
        println("You should only run this for one time.")
        User32Extend.instance.MessageBox(WinDef.HWND(Pointer.NULL), "You should only run this for one time.", "ZDesktopManager", User32ExtendFlags.MB_ICONERROR)
        return
    }

    val desktopDir = Shell32Util.getSpecialFolderPath(ShlObj.CSIDL_DESKTOP, false).let(::File)
    val subDirs = desktopDir.listFiles { file -> file.isDirectory }?.toMutableList() ?: mutableListOf()

    if (subDirs.isEmpty()) {
        val demoDir = File(desktopDir, "ZDesktop")
        demoDir.mkdir()
        // todo 插入一篇说明文档
        subDirs.add(demoDir)
    }
    println(subDirs.joinToString())

    val dimension: Dimension = Toolkit.getDefaultToolkit().screenSize

    val width = dimension.width
    val height = dimension.height

    println("Screen [$width * $height]")
    application {
        val desktopChildrenDirs = remember { mutableStateListOf<File>(elements = subDirs.toTypedArray()) }

        var nextWindowTop = 16
        var nextWindowRight = 16
        desktopChildrenDirs.forEachIndexed { dirIndex, dir ->
            DesktopFolderWindow(dir, onCloseWindowRequest = {}, onPlacingWindow = { window: Window ->
                window.setLocation(width - nextWindowRight - window.width, nextWindowTop)
                nextWindowTop += window.height
                nextWindowTop += 16
                if (nextWindowTop + window.height > height) {
                    // 这一列放不下，左边继续排
                    nextWindowTop = 0
                    nextWindowRight += window.width + 16
                }
            })
        }
        Tray(icon = IconUtil.emptyPainter, tooltip = "桌面管理工具", menu = {
            val scope = rememberCoroutineScope()
            Item("Exit", onClick = {
                scope.launch {
                    val res = withContext(Dispatchers.IO) {
                        User32Extend.instance.MessageBox(WinDef.HWND(Pointer.NULL), "确实要退出桌面管理工具吗？", "ZDesktopManager", User32ExtendFlags.MB_ICONQUESTION or User32ExtendFlags.MB_YESNO)
                    }
                    if (res == User32ExtendFlags.MB_RETURN_IDYES) {
                        exitApplication()
                    }
                }
            })
        })

        LaunchedEffect(desktopDir) {
            val monitor = FileAlterationMonitor(5_000L, FileAlterationObserver(desktopDir).also {
                it.addListener(object : FileAlterationListenerAdaptor() {
                    override fun onDirectoryCreate(directory: File?) {
                        super.onDirectoryCreate(directory)
                        updateDir()
                    }

                    override fun onDirectoryDelete(directory: File?) {
                        super.onDirectoryDelete(directory)
                        updateDir()
                    }

                    override fun onDirectoryChange(directory: File?) {
                        super.onDirectoryChange(directory)
                        updateDir()
                    }

                    private fun updateDir() {
                        val dirs = desktopDir.listFiles { file -> file.isDirectory }?.toList() ?: emptyList<File>()
                        desktopChildrenDirs.clear()
                        desktopChildrenDirs.addAll(dirs)
                    }
                })
            })

            monitor.start()
        }
    }
}

