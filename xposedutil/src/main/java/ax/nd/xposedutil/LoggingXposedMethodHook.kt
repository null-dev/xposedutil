package ax.nd.xposedutil

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import java.lang.Exception

object LoggingXposedMethodHook : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) {
        val method = param.method
        val methodName = method.name
        val className = method.declaringClass.canonicalName
        Log.d(DEBUG_LOG_TAG, "[$methodName] called, full call path: $className.$methodName")
        Log.d(DEBUG_LOG_TAG, "[$methodName] called, args: ${param.args.contentToString()}")
        Log.d(DEBUG_LOG_TAG, "[$methodName] this: ${param.thisObject}")
        Log.d(DEBUG_LOG_TAG, "[$methodName] stack trace:", Exception())
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        val method = param.method
        val methodName = method.name
        Log.d(DEBUG_LOG_TAG, "[$methodName] result: ${param.result}")
    }
}