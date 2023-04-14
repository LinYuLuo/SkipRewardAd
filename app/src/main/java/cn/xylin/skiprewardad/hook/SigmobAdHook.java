package cn.xylin.skiprewardad.hook;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 未测试是否有效
 */
public class SigmobAdHook extends BaseHook {
    private String adId;
    private Object rewardInfo;
    public SigmobAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.sigmob.windad.rewardVideo.WindRewardVideoAd");
        clazb = findClass("com.sigmob.windad.rewardVideo.WindRewardInfo");
        if (claza == null || clazb == null) {
            return;
        }
        rewardInfo = XposedHelpers.newInstance(clazb, true);
        XposedHelpers.findAndHookMethod(claza, "onVideoAdLoadSuccess", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                adId = (String) param.args[0];
            }
        });
        XposedBridge.hookAllMethods(claza, "show", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                callMethod(param.thisObject, "onVideoAdPlayStart", adId);
                callMethod(param.thisObject, "onVideoAdPlayComplete", rewardInfo, adId);
                callMethod(param.thisObject, "onVideoAdPlayEnd", adId);
                callMethod(param.thisObject, "onVideoAdClosed", adId);
                log("Sigmob-发放奖励");
                param.setResult(true);
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
