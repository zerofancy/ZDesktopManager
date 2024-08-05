package top.ntutn.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.unit.Dp
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinDef.HBITMAP
import com.sun.jna.platform.win32.WinDef.HICON
import com.sun.jna.platform.win32.WinGDI.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.swing.filechooser.FileSystemView


object IconUtil {
    val emptyPainter: Painter = object : Painter() {
        override val intrinsicSize: Size
            get() = Size.Unspecified

        override fun DrawScope.onDraw() {
        }
    }

    private fun toImage(hicon: HICON?): BufferedImage? {
        var bitmapHandle: HBITMAP? = null
        val user32 = User32Extend.instance
        val gdi32 = GDI32.INSTANCE

        try {
            val info = ICONINFO()
            if (!user32.GetIconInfo(hicon, info)) return null

            info.read()
            bitmapHandle = Optional.ofNullable(info.hbmColor).orElse(info.hbmMask)

            val bitmap = BITMAP()
            if (gdi32.GetObject(bitmapHandle, bitmap.size(), bitmap.pointer) > 0) {
                bitmap.read()

                val width = bitmap.bmWidth.toInt()
                val height = bitmap.bmHeight.toInt()

                val deviceContext = user32.GetDC(null)
                val bitmapInfo = BITMAPINFO()

                bitmapInfo.bmiHeader.biSize = bitmapInfo.bmiHeader.size()
                require(
                    gdi32.GetDIBits(
                        deviceContext, bitmapHandle, 0, 0, Pointer.NULL, bitmapInfo,
                        DIB_RGB_COLORS
                    ) != 0
                ) { "GetDIBits should not return 0" }

                bitmapInfo.read()

                val pixels = Memory(bitmapInfo.bmiHeader.biSizeImage.toLong())
                bitmapInfo.bmiHeader.biCompression = BI_RGB
                bitmapInfo.bmiHeader.biHeight = -height

                require(
                    gdi32.GetDIBits(
                        deviceContext, bitmapHandle, 0, bitmapInfo.bmiHeader.biHeight, pixels, bitmapInfo,
                        DIB_RGB_COLORS
                    ) != 0
                ) { "GetDIBits should not return 0" }

                val colorArray = pixels.getIntArray(0, width * height)
                val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                image.setRGB(0, 0, width, height, colorArray, 0, width)

                return image
            }
        } finally {
            gdi32.DeleteObject(hicon)
            Optional.ofNullable(bitmapHandle).ifPresent { hObject: HBITMAP? ->
                gdi32.DeleteObject(
                    hObject
                )
            }
        }

        return null
    }

    suspend fun getFileIconPainter(file: File, width: Dp, height: Dp): Painter = withContext(Dispatchers.IO) {
        val iconFile = if (file.isFile && file.extension == "url") {
            // url文件图标需要单独加载
            kotlin.runCatching {
                file.readLines().find { it.contains("IconFile") }?.split("=")?.last()?.let(::File)
            }.getOrNull() ?: file
        } else {
            file
        }

        kotlin.runCatching {

            val fileIcon = FileSystemView.getFileSystemView().getSystemIcon(iconFile, width.value.toInt(), height.value.toInt())
            val img = BufferedImage(fileIcon.iconWidth, fileIcon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g2d = img.createGraphics()
            fileIcon.paintIcon(null, g2d, 0, 0)
            g2d.dispose()
            return@withContext img.toPainter()
        }.onFailure {
            println("Loading icon for $iconFile failed, retrying with win32 api...")
            it.printStackTrace()
        }

        // try load with win32 api
        val hInstance = Kernel32.INSTANCE.GetModuleHandle(null)
        val indexMemory = Memory(Long.SIZE_BITS.toLong())
        val icon = Shell32Extend.instance.ExtractAssociatedIcon(hInstance, iconFile.absolutePath, indexMemory)
        return@withContext toImage(icon)?.toPainter() ?: emptyPainter
    }
}