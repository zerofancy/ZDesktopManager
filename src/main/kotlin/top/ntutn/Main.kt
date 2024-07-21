package top.ntutn

import androidx.compose.ui.window.*
import com.sun.jna.platform.win32.*
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
        var nextWindowTop = 16
        var nextWindowRight = 16
        subDirs.forEachIndexed { dirIndex, dir ->
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
    }
}

