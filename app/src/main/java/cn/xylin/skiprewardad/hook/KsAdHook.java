package cn.xylin.skiprewardad.hook;

import android.content.Context;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 未测试是否有效
 */
public class KsAdHook extends BaseHook {
    private final Set<String> clsSet = Collections.newSetFromMap(new ConcurrentHashMap<>(6));
    private XC_MethodHook showRewardHook;
    private Object listener;
    private Class<?> clazc;
    public KsAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.kwad.sdk.KsAdSDKImpl");
        clazb = findClass("com.kwad.sdk.api.KsLoadManager$RewardVideoAdListener");
        clazc = findClass("com.kwad.sdk.api.KsRewardVideoAd$RewardAdInteractionListener");
        if (claza == null || clazb == null || clazc == null) {
            return;
        }
        showRewardHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (listener == null) {
                    return;
                }
                callMethod(listener, "onVideoPlayStart");
                callMethod(listener, "onRewardVerify");
                callMethod(listener, "onRewardStepVerify", 0, 0);
                callMethod(listener, "onExtraRewardVerify", 0);
                callMethod(listener, "onVideoPlayEnd");
                callMethod(listener, "onPageDismiss");
                param.setResult(null);
                log("KsAd-发放奖励");
            }
        };
        XposedHelpers.findAndHookMethod(claza, "getAdManager", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object result = param.getResult();
                if (result == null || isHooked(result.getClass().getName())) {
                    return;
                }
                XposedBridge.hookAllMethods(result.getClass(), "loadRewardVideoAd", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        for (Object obj : param.args) {
                            if (!clazb.isInstance(obj) || isHooked(obj.getClass().getName())) {
                                continue;
                            }
                            XposedHelpers.findAndHookMethod(obj.getClass(),
                                    "onRewardVideoAdLoad",
                                    List.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                            if (param.args[0] instanceof List && !((List<?>) param.args[0]).isEmpty()) {
                                                Class<?> target = ((List<?>) param.args[0]).get(0).getClass();
                                                if (isHooked(target.getName())) {
                                                    return;
                                                }
                                                XposedBridge.hookAllMethods(target,
                                                        "setRewardAdInteractionListener",
                                                        new XC_MethodHook() {
                                                            @Override
                                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                                for (Object obj : param.args) {
                                                                    if (clazc.isInstance(obj)) {
                                                                        listener = obj;
                                                                        return;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                );
                                                XposedBridge.hookAllMethods(target, "showRewardVideoAd", showRewardHook);
                                            }
                                        }
                                    }
                            );
                            return;
                        }
                    }
                });
            }
        });
    }
    
    private boolean isHooked(String className) {
        if (clsSet.contains(className)) {
            return true;
        }
        clsSet.add(className);
        return false;
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
