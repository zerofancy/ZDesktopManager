package top.ntutn.util

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND

object DesktopWindowDiagnostics {
    fun diagnoseDesktopWindows() {
        println("=== 桌面窗口结构诊断 ===")
        
        // 获取桌面窗口
        val desktopHwnd = User32.INSTANCE.GetDesktopWindow()
        println("桌面窗口: ${getWindowInfo(desktopHwnd)}")
        
        // 查找Progman窗口
        val progmanHwnd = User32.INSTANCE.FindWindowEx(desktopHwnd, null, "Progman", null)
        println("Progman窗口: ${getWindowInfo(progmanHwnd)}")
        
        // 检查Progman的子窗口
        if (progmanHwnd != null) {
            println("Progman子窗口:")
            listChildWindows(progmanHwnd, 1)
        }
        
        // 查找WorkerW窗口
        println("\nWorkerW窗口:")
        var workerW: HWND? = HWND(Pointer.NULL)
        do {
            workerW = User32.INSTANCE.FindWindowEx(desktopHwnd, workerW, "WorkerW", null)
            if (workerW != null) {
                println(getWindowInfo(workerW))
                // 检查WorkerW的子窗口
                println("  WorkerW子窗口:")
                listChildWindows(workerW, 2)
            }
        } while (workerW != null)
        
        // 查找SHELLDLL_DefView窗口
        println("\nSHELLDLL_DefView窗口:")
        findWindowByClass("SHELLDLL_DefView", desktopHwnd)
        
        println("=== 诊断完成 ===")
    }
    
    private fun getWindowInfo(hwnd: HWND?): String {
        if (hwnd == null || hwnd.pointer == Pointer.NULL) {
            return "不存在"
        }
        
        val className = CharArray(256)
        User32.INSTANCE.GetClassName(hwnd, className, className.size)
        val classNameStr = className.joinToString("").takeWhile { it != '\u0000' }
        
        val title = CharArray(256)
        User32.INSTANCE.GetWindowText(hwnd, title, title.size)
        val titleStr = title.joinToString("").takeWhile { it != '\u0000' }
        
        return "[${hwnd.pointer}] 类名: $classNameStr, 标题: $titleStr"
    }
    
    private fun listChildWindows(parent: HWND, indentLevel: Int) {
        val indent = "  ".repeat(indentLevel)
        var child: HWND? = User32.INSTANCE.FindWindowEx(parent, null, null, null)
        
        while (child != null) {
            println("$indent${getWindowInfo(child)}")
            // 递归列出子窗口
            listChildWindows(child, indentLevel + 1)
            child = User32.INSTANCE.FindWindowEx(parent, child, null, null)
        }
    }
    
    private fun findWindowByClass(className: String, parent: HWND) {
        var child: HWND? = User32.INSTANCE.FindWindowEx(parent, null, className, null)
        
        while (child != null) {
            println(getWindowInfo(child))
            child = User32.INSTANCE.FindWindowEx(parent, child, className, null)
        }
        
        // 递归查找所有子窗口
        var nextChild: HWND? = User32.INSTANCE.FindWindowEx(parent, null, null, null)
        while (nextChild != null) {
            findWindowByClass(className, nextChild)
            nextChild = User32.INSTANCE.FindWindowEx(parent, nextChild, null, null)
        }
    }
}