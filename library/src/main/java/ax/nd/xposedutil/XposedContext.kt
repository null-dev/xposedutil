package ax.nd.xposedutil

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

typealias Hook = (param: XC_MethodHook.MethodHookParam) -> Unit
typealias ContextBuilder = (context: XposedContext) -> Unit

/**
 * Hook calls to a method only when those calls are made from another specific method
 */
class XposedContext(val packageParam: XC_LoadPackage.LoadPackageParam, parent: XposedContext?,
                    clazz: Class<*>, methodName: String, parameterTypes: Array<out Any>) {
    private val curContext = ThreadLocal.withInitial<XC_MethodHook.MethodHookParam> { null }
    private val enter = mutableListOf<Hook>()
    private val exit = mutableListOf<Hook>()

    val curParam: XC_MethodHook.MethodHookParam? get() = curContext.get()
    val requireCurParam: XC_MethodHook.MethodHookParam get() = requireNotNull(curContext.get()) {
        "Not in context!"
    }

    init {
        XposedHelpers.findAndHookMethod(clazz, methodName, *parameterTypes, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if(parent != null) { // Ensure in parent context if we have parent
                    if (parent.curContext.get() == null) {
                        return
                    }
                }

                // If we are already in context, method has recursed!
                if(curContext.get() != null) {
                    Log.w(DEBUG_LOG_TAG, "Method hooked by XposedContext is recursive! Stack trace:", Exception())
                    return
                }
                curContext.set(param) // Enter context
                for(hook in enter) {
                    hook(param)
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if(curContext.get() != null) { // Ensure in context
                    try {
                        for (hook in exit) {
                            hook(param)
                        }
                    } finally {
                        curContext.remove() // Leave context
                    }
                }
            }
        })
    }

    /**
     * Hook to run immediately after entering this context
     */
    fun enter(block: Hook) {
        enter.add(block)
    }
    /**
     * Hook to run immediately before exiting this context
     */
    fun exit(block: Hook) {
        exit.add(block)
    }

    fun withContext(clazz: Class<*>, methodName: String, vararg parameterTypes: Any, contextBuilder: ContextBuilder): XposedContext {
        val context = XposedContext(packageParam, this, clazz, methodName, parameterTypes)
        contextBuilder(context)
        return context
    }

    fun withContext(className: String, methodName: String, vararg parameterTypes: Any, contextBuilder: ContextBuilder): XposedContext {
        val context = XposedContext(packageParam, this, XposedHelpers.findClass(className, packageParam.classLoader), methodName, parameterTypes)
        contextBuilder(context)
        return context
    }
}

fun XC_LoadPackage.LoadPackageParam.withContext(clazz: Class<*>, methodName: String, vararg parameterTypes: Any, contextBuilder: ContextBuilder): XposedContext {
    val context = XposedContext(this, null, clazz, methodName, parameterTypes)
    contextBuilder(context)
    return context
}

fun XC_LoadPackage.LoadPackageParam.withContext(className: String, methodName: String, vararg parameterTypes: Any, contextBuilder: ContextBuilder): XposedContext {
    val context = XposedContext(this, null, XposedHelpers.findClass(className, classLoader), methodName, parameterTypes)
    contextBuilder(context)
    return context
}
