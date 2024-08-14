package top.ntutn.util

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.W32APIOptions

@Suppress("FunctionName")
interface User32Extend: User32 {
    companion object {
        val instance: User32Extend = Native.load("user32", User32Extend::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }

    /**
     * int MessageBox(
     *   [in, optional] HWND    hWnd,
     *   [in, optional] LPCTSTR lpText,
     *   [in, optional] LPCTSTR lpCaption,
     *   [in]           UINT    uType
     * );
     */
    fun MessageBox(hWnd: HWND, lpText: String, lpCaption: String, uType: Long): Int
}