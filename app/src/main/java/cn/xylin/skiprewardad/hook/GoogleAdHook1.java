package cn.xylin.skiprewardad.hook;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class GoogleAdHook1 extends BaseHook {
    private Object defReward, listener;
    private Class<?> clazc;
    public GoogleAdHook1(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        try {
            defReward = findClass("com.google.android.gms.ads.rewarded.RewardItem").getDeclaredField("DEFAULT_REWARD").get(null);
        } catch (Throwable ignore) {
            return;
        }
        claza = findClass("com.google.android.gms.ads.rewarded.RewardedAd");
        clazb = findClass("com.google.android.gms.ads.rewarded.RewardedAdLoadCallback");
        clazc = findClass("com.google.android.gms.ads.OnUserEarnedRewardListener");
        if (claza == null || clazb == null || clazc == null) {
            return;
        }
        XposedBridge.hookAllMethods(claza, "load", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (Object obj : param.args) {
                    if (!clazb.isInstance(obj) || isHooked(clazb.getName())) {
                        continue;
                    }
                    XposedBridge.hookAllMethods(obj.getClass(), "onAdLoaded", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args.length == 0 || param.args[0] == null) {
                                return;
                            }
                            Class<?> clazz = param.args[0].getClass();
                            if (isHooked(clazz.getName())) {
                                return;
                            }
                            XposedBridge.hookAllMethods(clazz, "setFullScreenContentCallback", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.args.length == 1 && param.args[0] == null) {
                                        listener = param.args[0];
                                    }
                                }
                            });
                            XposedBridge.hookAllMethods(clazz, "show", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (listener != null) {
                                        for (Object obj : param.args) {
                                            if (clazc.isInstance(obj)) {
                                                callMethod(listener, "onAdShowedFullScreenContent");
                                                callMethod(obj, "onUserEarnedReward", defReward);
                                                callMethod(listener, "onAdDismissedFullScreenContent");
                                                param.setResult(null);
                                                log("GoogleAd1-发放奖励");
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    });
                    break;
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
