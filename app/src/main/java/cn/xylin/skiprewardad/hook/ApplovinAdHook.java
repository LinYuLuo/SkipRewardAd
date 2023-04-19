package cn.xylin.skiprewardad.hook;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class ApplovinAdHook extends BaseHook {
    private Object listener, maxAd, defReward;
    public ApplovinAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.applovin.mediation.ads.MaxRewardedAd");
        if (claza == null) {
            return;
        }
        try {
            defReward = findClass("com.applovin.impl.mediation.MaxRewardImpl").getDeclaredMethod("createDefault").invoke(null);
        } catch (Throwable ignore) {
            return;
        }
        XposedBridge.hookAllMethods(claza, "setListener", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args.length == 1 && param.args[0] != null) {
                    listener = param.args[0];
                    if (isHooked(listener.getClass().getName())) {
                        return;
                    }
                    XposedBridge.hookAllMethods(listener.getClass(), "onAdLoaded", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args.length == 1 && param.args[0] != null) {
                                maxAd = param.args[0];
                            }
                        }
                    });
                }
            }
        });
        XposedBridge.hookAllMethods(claza, "showAd", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (listener != null && maxAd != null) {
                    callMethod(listener, "onRewardedVideoStarted", maxAd);
                    callMethod(listener, "onUserRewarded", maxAd, defReward);
                    callMethod(listener, "onRewardedVideoCompleted", maxAd);
                    param.setResult(null);
                    log("ApplovinAd-发放奖励");
                }
            }
        });
    }
    @Override
    protected String targetPackageName() {
        return null;
    }
    @Override
    protected boolean isTarget() {
        return true;
    }
}
