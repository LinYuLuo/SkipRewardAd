package cn.xylin.skiprewardad.hook;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class VungleAdHook extends BaseHook {
    public VungleAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.vungle.warren.Vungle");
        clazb = findClass("com.vungle.warren.PlayAdCallback");
        if (claza == null || clazb == null) {
            return;
        }
        XposedBridge.hookAllMethods(claza, "playAd", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String id = param.args[0].toString();
                for (Object obj : param.args) {
                    if (clazb.isInstance(obj)) {
                        callMethod(obj, "creativeId", id);
                        callMethod(obj, "onAdStart", id);
                        callMethod(obj, "onAdViewed", id);
                        callMethod(obj, "onAdRewarded", id);
                        callMethod(obj, "onAdEnd", id);
                        callMethod(obj, "onAdEnd", id, true, false);
                        param.setResult(null);
                        log("VungleAd-发放奖励");
                    }
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
