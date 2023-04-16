package cn.xylin.skiprewardad.hook;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 未测试是否有效
 */
public class MintegralAdHook extends BaseHook {
    private Object fakeReward, tempListener, tempParam;
    private XC_MethodHook setListenerHook, callbackHook;
    private final Set<String> clsSet = Collections.newSetFromMap(new ConcurrentHashMap<>(6));
    public MintegralAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.mbridge.msdk.reward.player.MBRewardVideoActivity");
        clazb = findClass("com.mbridge.msdk.out.RewardInfo");
        if (claza == null || clazb == null) {
            return;
        }
        fakeReward = XposedHelpers.newInstance(clazb, true, 0);
        XposedHelpers.findAndHookMethod(claza, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (tempListener != null && tempParam != null) {
                    callMethod(tempListener, "onVideoComplete", tempParam);
                    callMethod(tempListener, "onAdClose", tempParam, fakeReward);
                    ((Activity) param.thisObject).finish();
                    log("MintegralAD-发放奖励");
                }
            }
        });
        hook(findClass("com.mbridge.msdk.out.MBBidRewardVideoHandler"));
        hook(findClass("com.mbridge.msdk.out.MBRewardVideoHandler"));
    }
    
    private void hook(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (callbackHook == null) {
            callbackHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length > 0 && param.args[0] != null) {
                        tempListener = param.thisObject;
                        tempParam = param.args[0];
                    }
                }
            };
        }
        if (setListenerHook == null) {
            setListenerHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length <= 0 || param.args[0] == null) {
                        return;
                    }
                    for (Object obj : param.args) {
                        if (obj != null && !isHooked(obj.getClass().getName())) {
                            XposedBridge.hookAllMethods(obj.getClass(), "onLoadSuccess", callbackHook);
                        }
                    }
                }
            };
        }
        XposedBridge.hookAllMethods(clazz, "setRewardVideoListener", setListenerHook);
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
