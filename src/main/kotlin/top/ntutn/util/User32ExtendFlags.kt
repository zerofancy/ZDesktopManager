package top.ntutn.util

object User32ExtendFlags {
    const val MB_ABORTRETRYIGNORE = 0x00000002L
    const val MB_CANCELTRYCONTINUE = 0x00000006L
    @Deprecated("It's hard to handle WM_HELP message in Java/Kotlin")
    const val MB_HELP = 0x00004000L
    const val MB_OK = 0x00000000L
    const val MB_OKCANCEL = 0x00000001L
    const val MB_RETRYCANCEL = 0x00000005L
    const val MB_YESNO = 0x00000004L
    const val MB_YESNOCANCEL = 0x00000003L

    const val MB_ICONEXCLAMATION = 0x00000030L
    const val MB_ICONWARNING = 0x00000030L
    const val MB_ICONINFORMATION = 0x00000040L
    const val MB_ICONASTERISK = 0x00000040L
    const val MB_ICONQUESTION = 0x00000020L
    const val MB_ICONSTOP = 0x00000010L
    const val MB_ICONERROR = 0x00000010L
    const val MB_ICONHAND = 0x00000010L

    const val MB_DEFBUTTON1 = 0x00000000L
    const val MB_DEFBUTTON2 = 0x00000100L
    const val MB_DEFBUTTON3 = 0x00000200L
    const val MB_DEFBUTTON4 = 0x00000300L

    const val MB_APPLMODAL = 0x00000000L
    const val MB_SYSTEMMODAL = 0x00001000L
    const val MB_TASKMODAL = 0x00002000L

    const val MB_DEFAULT_DESKTOP_ONLY = 0x00020000L
    const val MB_RIGHT = 0x00080000L
    const val MB_RTLREADING = 0x00100000L
    const val MB_SETFOREGROUND = 0x00010000L
    const val MB_TOPMOST = 0x00040000L
    const val MB_SERVICE_NOTIFICATION = 0x00200000L

    const val MB_RETURN_IDABORT = 3
    const val MB_RETURN_IDCANCEL = 2
    const val MB_RETURN_IDCONTINUE = 11
    const val MB_RETURN_IDIGNORE = 5
    const val MB_RETURN_IDNO = 7
    const val MB_RETURN_IDOK = 1
    const val MB_RETURN_IDRETRY = 4
    const val MB_RETURN_IDTRYAGAIN = 10
    const val MB_RETURN_IDYES = 6
}