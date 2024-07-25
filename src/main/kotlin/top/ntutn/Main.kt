package top.ntutn

import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import com.sun.jna.platform.win32.*
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.Window
import java.io.File


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

    val dimension: Dimension = Toolkit.getDefaultToolkit().screenSize

    val width = dimension.width
    val height = dimension.height

    println("Screen [$width * $height]")
    application {
        val desktopChildrenDirs = remember { mutableStateListOf<File>(elements = subDirs.toTypedArray()) }

        var nextWindowTop = 16
        var nextWindowRight = 16
        desktopChildrenDirs.forEachIndexed { dirIndex, dir ->
            // fixme 文件夹监听后新插入顺序会乱
            DesktopFolderWindow(dir, onCloseWindowRequest = ::exitApplication, onPlacingWindow = { window: Window ->
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
        LaunchedEffect(null) {
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
                        if (dirs.isEmpty()) {
                            // todo show tip message
                            exitApplication()
                        } else {
                            desktopChildrenDirs.clear()
                            desktopChildrenDirs.addAll(dirs)
                        }
                    }
                })
            })

            monitor.start()
        }
    }
}

