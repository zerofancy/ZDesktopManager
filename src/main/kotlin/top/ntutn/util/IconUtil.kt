package top.ntutn.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.unit.Dp
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.filechooser.FileSystemView

object IconUtil {
    val emptyPainter: Painter = object : Painter() {
        override val intrinsicSize: Size
            get() = Size.Unspecified

        override fun DrawScope.onDraw() {
        }
    }

    fun getFileIconPainter(file: File, width: Dp, height: Dp): Painter {
        kotlin.runCatching {
            val dirIcon = FileSystemView.getFileSystemView().getSystemIcon(file, width.value.toInt(), height.value.toInt())

            val img = BufferedImage(dirIcon.iconWidth, dirIcon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g2d = img.createGraphics()
            dirIcon.paintIcon(null, g2d, 0, 0)
            g2d.dispose()
            return img.toPainter()
        }
        return emptyPainter
    }
}