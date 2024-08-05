package top.ntutn.util

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.WinDef.HICON
import com.sun.jna.platform.win32.WinDef.HINSTANCE
import com.sun.jna.win32.W32APIOptions

interface Shell32Extend: Shell32 {
    companion object {
        val instance: Shell32Extend = Native.load("shell32", Shell32Extend::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }

    fun ExtractAssociatedIcon(
        hInst: HINSTANCE,
        pszIconPath: String?,
        piIcon: Pointer
    ): HICON?
}