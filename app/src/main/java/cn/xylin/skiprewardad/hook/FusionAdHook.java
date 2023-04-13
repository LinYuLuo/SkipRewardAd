package cn.xylin.skiprewardad.hook;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class FusionAdHook extends BaseHook {
    public FusionAdHook(Context ctx) {
        super(ctx);
    }
    
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.leyou.fusionsdk.FusionAdSDK");
        clazb = findClass("com.leyou.fusionsdk.ads.rewardvideo.RewardVideoAdListener");
        if (claza == null || clazb == null) {
            //log("Fusion-无法找到找到对应的类！", (claza == null ? "FusionAdSDK为空！" : "RewardVideoAdListener为空！"));
            return;
        }
        log("进入FusionAdHook");
        XposedBridge.hookAllMethods(claza, "loadRewardVideoAd", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                log("Fusion-进入loadRewardVideoAd");
                for (Object obj : param.args) {
                    if (!clazb.isInstance(obj)) {
                        continue;
                    }
                    log("Fusion-发放奖励");
                    callMethod(obj, "onReward", "");
                    param.setResult(null);
                    return;
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
    
    @Override
    protected boolean isDebug() {
        return true;
    }
}