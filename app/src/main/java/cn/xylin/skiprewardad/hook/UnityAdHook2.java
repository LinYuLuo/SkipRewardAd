package cn.xylin.skiprewardad.hook;

import android.content.Context;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class UnityAdHook2 extends UnityAdHook1 {
    public UnityAdHook2(Context ctx) {
        super(ctx);
    }
    
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.unity3d.ads.UnityAds");
        clazb = findClass("com.unity3d.ads.IUnityAdsShowListener");
        Method target = getOpenMethod();
        if (claza == null || clazb == null || target == null || !getFakeEnum("com.unity3d.ads.UnityAds$UnityAdsShowCompletionState")) {
            return;
        }
        XposedBridge.hookAllMethods(claza, "show", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (Object obj : param.args) {
                    if (clazb.isInstance(obj)) {
                        listener = obj;
                        return;
                    }
                }
            }
        });
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
                        callMethod(listener, "onUnityAdsShowStart", "rewardedVideo");
                        callMethod(listener, "onUnityAdsShowComplete", "rewardedVideo", fakeEnum);
                        if (param.args[0] instanceof Integer) {
                            param.args[0] = null;
                        }
                        log("UnityAd2-发放奖励");
                    }
                }
            }
        });
    }
}
