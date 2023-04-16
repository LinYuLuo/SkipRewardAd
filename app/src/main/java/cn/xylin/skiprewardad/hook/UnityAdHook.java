package cn.xylin.skiprewardad.hook;

import android.content.Context;
import java.util.Objects;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 未测试是否有效
 */
public class UnityAdHook extends BaseHook {
    private Object listener, fakeEnum;
    public UnityAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.unity3d.ads.properties.AdsProperties");
        clazb = findClass("com.unity3d.ads.IUnityAdsListener");
        Class<?> clazc = findClass("com.unity3d.services.ads.api.AdUnit");
        if (claza == null || clazb == null || clazc == null) {
            return;
        }
        try {
            Class<?> clazz = findClass("com.unity3d.ads.UnityAds$FinishState");
            if (clazz.isEnum()) {
                for (Object obj : clazz.getEnumConstants()) {
                    if ("COMPLETED".equals(obj.toString())) {
                        fakeEnum = obj;
                        break;
                    }
                }
            }
            Objects.requireNonNull(fakeEnum);
        } catch (Throwable ignore) {
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
        XposedBridge.hookAllMethods(clazc, "open", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args.length != 9 || listener == null) {
                    return;
                }
                if (!(param.args[5] instanceof Boolean) || !(param.args[6] instanceof Boolean)) {
                    return;
                }
                if (((Boolean) param.args[5]) && !((Boolean) param.args[6])) {
                    callMethod(listener, "onUnityAdsReady", "rewardedVideo");
                    callMethod(listener, "onUnityAdsStart", "rewardedVideo");
                    callMethod(listener, "onUnityAdsFinish", "rewardedVideo", fakeEnum);
                    param.args[0] = null;
                    log("UnityAd-发放奖励");
                }
            }
        });
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
