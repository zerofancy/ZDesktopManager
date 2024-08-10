package top.ntutn.util

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinError

object ApplicationUtil {
    fun ensureSingleInstance(appName: String): Boolean {
        val mutex = Kernel32.INSTANCE.CreateMutex(null, false, appName)
        val timeOuted = Kernel32.INSTANCE.WaitForSingleObject(mutex, 10) == WinError.WAIT_TIMEOUT
        val hasError = Kernel32.INSTANCE.GetLastError() != 0
        val addedMutex =  !(timeOuted || hasError)
        if (addedMutex) {
            Runtime.getRuntime().addShutdownHook(Thread {
                Kernel32.INSTANCE.ReleaseMutex(mutex)
            })
        }
        return addedMutex
    }
}