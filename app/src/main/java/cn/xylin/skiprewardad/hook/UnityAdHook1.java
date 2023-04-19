package cn.xylin.skiprewardad.hook;

import android.content.Context;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 未测试是否有效
 */
public class UnityAdHook1 extends BaseHook {
    protected Object listener, fakeEnum;
    public UnityAdHook1(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.unity3d.ads.properties.AdsProperties");
        clazb = findClass("com.unity3d.ads.IUnityAdsListener");
        Method target = getOpenMethod();
        if (claza == null || clazb == null || target == null || !getFakeEnum("com.unity3d.ads.UnityAds$FinishState")) {
            return;
        }
        XC_MethodHook listenerHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (Object obj : param.args) {
                    if (clazb.isInstance(obj)) {
                        listener = obj;
                        return;
                    }
                }
            }
        };
        XposedBridge.hookAllMethods(claza, "setListener", listenerHook);
        XposedBridge.hookAllMethods(claza, "addListener", listenerHook);
        XposedBridge.hookMethod(target, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (listener == null) {
                    return;
                }
                for (int i = 0; i < param.args.length - 1; i++) {
                    if (!(param.args[i] instanceof Boolean) || !(param.args[i + 1] instanceof Boolean)) {
                        continue;
                    }
                    if (((Boolean) param.args[i]) && !((Boolean) param.args[i + 1])) {
                        callMethod(listener, "onUnityAdsReady", "rewardedVideo");
                        callMethod(listener, "onUnityAdsStart", "rewardedVideo");
                        callMethod(listener, "onUnityAdsFinish", "rewardedVideo", fakeEnum);
                        if (param.args[0] instanceof Integer) {
                            param.args[0] = null;
                        }
                        log("UnityAd1-发放奖励");
                    }
                }
            }
        });
    }
    
    protected boolean getFakeEnum(String className) {
        Class<?> clazz = findClass(className);
        if (clazz.isEnum()) {
            for (Object obj : clazz.getEnumConstants()) {
                if ("COMPLETED".equals(obj.toString())) {
                    fakeEnum = obj;
                    return true;
                }
            }
        }
        return fakeEnum == null;
    }
    
    protected Method getOpenMethod() {
        Class<?> clazz = findClass("com.unity3d.services.ads.api.AdUnit");
        if (clazz == null) {
            return null;
        }
        Method target = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!"open".equals(method.getName())) {
                continue;
            }
            if (target == null || target.getParameterTypes().length < method.getParameterTypes().length) {
                target = method;
            }
        }
        return target;
    }
    
    @Override
    protected boolean isTarget() {
        return true;
    }
    @Override
    protected String targetPackageName() {
        return null;
    }
}
