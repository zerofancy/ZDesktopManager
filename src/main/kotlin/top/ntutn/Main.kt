package top.ntutn

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.ShlObj
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import top.ntutn.ui.JNAMessageBox
import top.ntutn.util.ApplicationUtil
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.Window
import java.io.File
import kotlin.system.exitProcess

object App

fun main() {
    val isFirstInstance = ApplicationUtil.ensureSingleInstance("top.ntutn.KDesktopManager")

    if (!isFirstInstance) {
        println("You should only run this for one time.")
        JNAMessageBox.builder {
            content("You should only run this for one time.", "ZDesktopManager")
            errorIcon()
        }.build().showSync()
        return
    }

    val desktopDir = Shell32Util.getSpecialFolderPath(ShlObj.CSIDL_DESKTOP, false).let(::File)
    val subDirs = desktopDir.listFiles { file -> file.isDirectory }?.toMutableList() ?: mutableListOf()

    // 有些应用安装刷新桌面时会短暂返回空
//    if (subDirs.isEmpty()) {
//        JNAMessageBox.builder {
//            content("桌面上没有任何文件夹，是否要创建示例？", "ZDesktopManager")
//            questionIcon()
//            yesNo(yesCallback = {
//                val demoDir = File(desktopDir, "ZDesktop")
//                demoDir.mkdir()
//                subDirs.add(demoDir)
//
//                val outputFile = File(demoDir, "Guide.pdf")
//                if (!outputFile.exists()) {
//                    val classLoader = App::class.java.classLoader
//                    classLoader.getResourceAsStream("guide/Guide.pdf")?.use { ins ->
//                        outputFile.outputStream().use { ous ->
//                            ins.copyTo(ous)
//                        }
//                    }
//                }
//            }, noCallback = {
//                exitProcess(0)
//            })
//        }.build().showSync()
//    }

    val dimension: Dimension = Toolkit.getDefaultToolkit().screenSize

    val width = dimension.width
    val height = dimension.height

    println("Screen [$width * $height]")
    application {
        val desktopChildrenDirs = remember { mutableStateListOf<File>(elements = subDirs.toTypedArray()) }

        if (desktopChildrenDirs.isEmpty()) {
            JNAMessageBox.builder {
                content("桌面上不存在任何文件夹，管理工具即将退出。", "ZDesktopManager")
                warningIcon()
                buttonOk {
                    exitApplication()
                }
            }.build().showAsync()
        }
        var nextWindowTop = 16
        var nextWindowRight = 16
        desktopChildrenDirs.forEachIndexed { dirIndex, dir ->
            DesktopFolderWindow(dir, onCloseWindowRequest = {}, onPlacingWindow = { window: Window ->
                window.setLocation(width - nextWindowRight - window.width, nextWindowTop)
                nextWindowTop += window.height
                nextWindowTop += 16
                if (nextWindowTop + window.height > height) {
                    // 这一列放不下，左边继续排
                    nextWindowTop = 16
                    nextWindowRight += window.width + 16
                }
            })
        }
        Tray(icon = painterResource("icon.png"), tooltip = "桌面管理工具", menu = {
            Item("Exit", onClick = {
                JNAMessageBox.builder {
                    content("确实要退出桌面管理工具吗？", "ZDesktopManager")
                    informationIcon()
                    yesNo(yesCallback = ::exitApplication)
                }.build().showAsync()
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

