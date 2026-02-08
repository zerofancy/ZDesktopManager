package top.ntutn.util

import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinNT
import java.util.*

object WindowsVersionUtil {
    data class WindowsVersion(
        val major: Int,
        val minor: Int,
        val build: Int
    ) {
        override fun toString(): String {
            return "$major.$minor.$build"
        }
    }
    
    private val cachedVersion by lazy {
        getWindowsVersion()
    }
    
    fun getVersion(): WindowsVersion {
        return cachedVersion
    }
    
    fun isWindows10OrLater(): Boolean {
        val v = cachedVersion
        return (v.major > 10) || (v.major == 10 && v.build >= 10240)
    }
    
    fun isWindows11OrLater(): Boolean {
        val v = cachedVersion
        return (v.major > 10) || (v.major == 10 && v.build >= 22000)
    }
    
    private fun getWindowsVersion(): WindowsVersion {
        val osVersionInfoEx = OSVERSIONINFOEX()
        osVersionInfoEx.dwOSVersionInfoSize = osVersionInfoEx.size()
        
        if (RtlGetVersion(osVersionInfoEx)) {
            return WindowsVersion(
                osVersionInfoEx.dwMajorVersion,
                osVersionInfoEx.dwMinorVersion,
                osVersionInfoEx.dwBuildNumber
            )
        }
        
        // 回退到使用系统属性
        val osVersion = System.getProperty("os.version", "0.0")
        val parts = osVersion.split(".")
        val major = if (parts.isNotEmpty()) parts[0].toIntOrNull() ?: 0 else 0
        val minor = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
        val build = if (parts.size > 2) parts[2].toIntOrNull() ?: 0 else 0
        
        return WindowsVersion(major, minor, build)
    }
    
    @Structure.FieldOrder("dwOSVersionInfoSize", "dwMajorVersion", "dwMinorVersion", "dwBuildNumber", "dwPlatformId", "szCSDVersion", "wServicePackMajor", "wServicePackMinor", "wSuiteMask", "wProductType", "wReserved")
    internal class OSVERSIONINFOEX : Structure() {
        @JvmField var dwOSVersionInfoSize: Int = 0
        @JvmField var dwMajorVersion: Int = 0
        @JvmField var dwMinorVersion: Int = 0
        @JvmField var dwBuildNumber: Int = 0
        @JvmField var dwPlatformId: Int = 0
        @JvmField var szCSDVersion: ByteArray = ByteArray(128)
        @JvmField var wServicePackMajor: Short = 0
        @JvmField var wServicePackMinor: Short = 0
        @JvmField var wSuiteMask: Short = 0
        @JvmField var wProductType: Byte = 0
        @JvmField var wReserved: Byte = 0
        
        override fun getFieldOrder(): List<String> {
            return listOf("dwOSVersionInfoSize", "dwMajorVersion", "dwMinorVersion", "dwBuildNumber", "dwPlatformId", "szCSDVersion", "wServicePackMajor", "wServicePackMinor", "wSuiteMask", "wProductType", "wReserved")
        }
    }
    
    private external fun RtlGetVersion(lpVersionInformation: OSVERSIONINFOEX): Boolean
    
    init {
        Native.register("ntdll")
    }
}