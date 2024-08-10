package top.ntutn.ui

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.ntutn.util.User32Extend
import top.ntutn.util.User32ExtendFlags

class JNAMessageBox {
    companion object {
        fun builder(hWnd: Long? = null, buildBlock: Builder.() -> Unit) = Builder(hWnd).apply(buildBlock)
    }

    enum class ButtonFlags(val value: Long) {
        ABOUT_RETRY_IGNORE(User32ExtendFlags.MB_ABORTRETRYIGNORE),
        CANCEL_TRY_CONTINUE(User32ExtendFlags.MB_CANCELTRYCONTINUE),
        OK(User32ExtendFlags.MB_OK),
        OK_CANCEL(User32ExtendFlags.MB_OKCANCEL),
        RETRY_CANCEL(User32ExtendFlags.MB_RETRYCANCEL),
        YES_NO(User32ExtendFlags.MB_YESNO),
        YES_NO_CANCEL(User32ExtendFlags.MB_YESNOCANCEL),
    }

    enum class IconFlags(val value: Long) {
        NONE(0),
        WARNING(User32ExtendFlags.MB_ICONWARNING),
        INFORMATION(User32ExtendFlags.MB_ICONINFORMATION),
        QUESTION(User32ExtendFlags.MB_ICONQUESTION),
        ERROR(User32ExtendFlags.MB_ICONERROR)
    }

    enum class DefaultButton(val value: Long) {
        FIRST(User32ExtendFlags.MB_DEFBUTTON1),
        SECOND(User32ExtendFlags.MB_DEFBUTTON2),
        THIRD(User32ExtendFlags.MB_DEFBUTTON3),
        FORTH(User32ExtendFlags.MB_DEFBUTTON4)
    }

    enum class Return(val value: Int) {
        ABOUT(User32ExtendFlags.MB_RETURN_IDABORT),
        CANCEL(User32ExtendFlags.MB_RETURN_IDCANCEL),
        CONTINUE(User32ExtendFlags.MB_RETURN_IDCONTINUE),
        IGNORE(User32ExtendFlags.MB_RETURN_IDIGNORE),
        NO(User32ExtendFlags.MB_RETURN_IDNO),
        OK(User32ExtendFlags.MB_RETURN_IDOK),
        RETRY(User32ExtendFlags.MB_RETURN_IDRETRY),
        TRY_AGAIN(User32ExtendFlags.MB_RETURN_IDTRYAGAIN),
        YES(User32ExtendFlags.MB_RETURN_IDYES)
    }

    class Builder(hWnd: Long? = null) {
        val building = JNAMessageBox()

        init {
            building.hWnd = hWnd
        }

        fun noIcon() = apply {
            building.iconFlags = IconFlags.NONE
        }

        fun questionIcon() = apply {
            building.iconFlags = IconFlags.QUESTION
        }

        fun informationIcon() = apply {
            building.iconFlags = IconFlags.INFORMATION
        }

        fun warningIcon() = apply {
            building.iconFlags = IconFlags.WARNING
        }

        fun errorIcon() = apply {
            building.iconFlags = IconFlags.ERROR
        }

        fun content(text: String, title: String) = apply {
            building.text = text
            building.title = title
        }

        fun buttonAboutRetryIgnore(
            defaultButton: DefaultButton = DefaultButton.FIRST,
            aboutCallback: (() -> Unit)? = null,
            retryCallback: (() -> Unit)? = null,
            ignoreCallback: (() -> Unit)? = null
        ) = apply {
            building.buttonFlags = ButtonFlags.ABOUT_RETRY_IGNORE
            building.defaultButton = defaultButton
            building.callbacks[Return.ABOUT] = aboutCallback
            building.callbacks[Return.RETRY] = retryCallback
            building.callbacks[Return.IGNORE] = ignoreCallback
        }

        fun buttonCancelTryContinue(
            defaultButton: DefaultButton = DefaultButton.FIRST,
            cancelCallback: (() -> Unit)? = null,
            tryCallback: (() -> Unit)? = null,
            continueCallback: (() -> Unit)? = null
        ) = apply {
            building.buttonFlags = ButtonFlags.CANCEL_TRY_CONTINUE
            building.defaultButton = defaultButton
            building.callbacks[Return.CANCEL] = cancelCallback
            building.callbacks[Return.TRY_AGAIN] = tryCallback
            building.callbacks[Return.CONTINUE] = continueCallback
        }

        fun buttonOk(okCallback: (() -> Unit)? = null) = apply {
            building.buttonFlags = ButtonFlags.OK
            building.defaultButton = DefaultButton.FIRST
            building.callbacks[Return.OK] = okCallback
        }

        fun buttonOkCancel(
            defaultButton: DefaultButton = DefaultButton.FIRST,
            okCallback: (() -> Unit)? = null,
            cancelCallback: (() -> Unit)? = null
        ) = apply {
            building.buttonFlags = ButtonFlags.OK_CANCEL
            building.defaultButton = defaultButton
            building.callbacks[Return.OK] = okCallback
            building.callbacks[Return.CANCEL] = cancelCallback
        }

        fun retryCancel(
            defaultButton: DefaultButton = DefaultButton.FIRST,
            retryCallback: (() -> Unit)? = null,
            cancelCallback: (() -> Unit)? = null
        ) = apply {
            building.buttonFlags = ButtonFlags.RETRY_CANCEL
            building.defaultButton = defaultButton
            building.callbacks[Return.RETRY] = retryCallback
            building.callbacks[Return.CANCEL] = cancelCallback
        }

        fun yesNo(
            defaultButton: DefaultButton = DefaultButton.FIRST,
            yesCallback: (() -> Unit)? = null,
            noCallback: (() -> Unit)? = null
        ) =
            apply {
                building.buttonFlags = ButtonFlags.YES_NO
                building.defaultButton = defaultButton
                building.callbacks[Return.YES] = yesCallback
                building.callbacks[Return.NO] = noCallback
            }

        fun yesNoCancel(
            defaultButton: DefaultButton = DefaultButton.FIRST,
            yesCallback: (() -> Unit)? = null,
            noCallback: (() -> Unit)? = null,
            cancelCallback: (() -> Unit)? = null
        ) = apply {
            building.buttonFlags = ButtonFlags.YES_NO_CANCEL
            building.defaultButton = defaultButton
            building.callbacks[Return.YES] = yesCallback
            building.callbacks[Return.NO] = noCallback
            building.callbacks[Return.CANCEL] = cancelCallback
        }

        fun build() = building
    }

    var hWnd: Long? = null
    var title = ""
    var text = ""
    var buttonFlags = ButtonFlags.OK
    var iconFlags = IconFlags.NONE
    var defaultButton = DefaultButton.FIRST
    var isTopMost = false

    private val uType: Long
        get() {
            var tmp = buttonFlags.value or iconFlags.value or defaultButton.value
            if (isTopMost) {
                tmp = tmp or User32ExtendFlags.MB_TOPMOST
            }
            return tmp
        }

    val callbacks = hashMapOf<Return, (() -> Unit)?>()

    fun showSync() {
        val res = User32Extend.instance.MessageBox(WinDef.HWND(hWnd?.let { Pointer(it) }), text, title, uType)
        val returnValue = Return.entries.find { it.value == res } ?: return
        callbacks[returnValue]?.invoke()
    }

    suspend fun showInIOScope() = withContext(Dispatchers.IO) {
        User32Extend.instance.MessageBox(WinDef.HWND(hWnd?.let { Pointer(it) }), text, title, uType)
    }

    fun showAsync() = GlobalScope.launch {
        val res = showInIOScope()
        val returnValue = Return.entries.find { it.value == res } ?: return@launch
        callbacks[returnValue]?.invoke()
    }
}
