package cn.xylin.skiprewardad.hook;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
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
            log("未捕捉的错误！", e.getMessage());
        }
    }
    
    protected abstract void runHook() throws Throwable;
    
    protected abstract String targetPackageName();
    
    protected boolean isTarget() {
        return targetPackageName().equals(context.getPackageName());
    }
    
    protected boolean isDebug() {
        return true;
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
    
    protected final void methodHook(Class<?> clazz, String methodName, boolean isReplace, final Object returnValue, Class<?>... params) {
        if (claza == null) {
            return;
        }
        Object[] paramsHook;
        if (params == null || params.length == 0) {
            paramsHook = new Object[1];
        } else {
            paramsHook = new Object[params.length + 1];
        }
        for (int index = 0; index < paramsHook.length; index++) {
            if (index == (paramsHook.length - 1)) {
                paramsHook[index] = isReplace ? new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) {
                        return returnValue;
                    }
                } : new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(returnValue);
                    }
                };
                break;
            }
            paramsHook[index] = params[index];
        }
        XposedHelpers.findAndHookMethod(clazz, methodName, paramsHook);
    }
    
    protected final void staticMethodHook(Class<?> clazz, String methodName, Object... hooks) {
        if (hooks.length < 1) {
            return;
        }
        if (!(hooks[hooks.length - 1] instanceof XC_MethodHook)) {
            return;
        }
        try {
            Class<?>[] params = new Class[hooks.length == 1 ? 0 : hooks.length - 1];
            for (int i = 0; i < params.length; i++) {
                params[i] = hooks[i].getClass();
            }
            Method method = clazz.getDeclaredMethod(methodName, params);
            XposedBridge.hookMethod(method, (XC_MethodHook) hooks[hooks.length-1]);
        } catch (NoSuchMethodException ignore) {
        }
    }
    
    protected final void staticMethodHook(Class<?> clazz, String methodName, boolean isReplace, final Object returnValue, Class<?>... params) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, params);
            XposedBridge.hookMethod(method, isReplace ? new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) {
                    return returnValue;
                }
            } : new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(returnValue);
                }
            });
        } catch (NoSuchMethodException ignore) {
        }
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
