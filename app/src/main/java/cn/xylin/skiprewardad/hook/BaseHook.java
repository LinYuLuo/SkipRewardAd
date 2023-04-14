package cn.xylin.skiprewardad.hook;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public abstract class BaseHook {
    protected Context context;
    protected Class<?> claza, clazb;
    private StringBuffer buffer;
    private Handler handler;
    
    public BaseHook(Context ctx) {
        context = ctx;
        try {
            if (isTarget()) {
                runHook();
            }
        } catch (Throwable e) {
            log(e.getMessage());
        }
    }
    
    protected abstract void runHook() throws Throwable;
    
    protected abstract String targetPackageName();
    
    protected boolean isTarget() {
        return targetPackageName().equals(context.getPackageName());
    }
    
    protected boolean isDebug() {
        return false;
    }
    
    protected final boolean callMethod(Object target, String methodName, Object... params) {
        try {
            XposedHelpers.callMethod(target, methodName, params);
            return true;
        } catch (Throwable ignore) {
        }
        return false;
    }
    
    protected final void log(Object... params) {
        splitLog("", params);
    }
    
    protected final void splitLog(String splitStr, Object... params) {
        if (!isDebug()) {
            return;
        }
        if (buffer == null) {
            buffer = new StringBuffer();
        } else {
            buffer.setLength(0);
        }
        int index = 1;
        int count = params.length;
        for (Object param : params) {
            buffer.append(param);
            if (index >= count) {
                break;
            }
            buffer.append(splitStr);
            index++;
        }
        XposedBridge.log(buffer.toString());
    }
    
    protected final Handler getHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }
    
    protected final Class<?> findClass(String name) {
        return XposedHelpers.findClassIfExists(name, context.getClassLoader());
    }
}
