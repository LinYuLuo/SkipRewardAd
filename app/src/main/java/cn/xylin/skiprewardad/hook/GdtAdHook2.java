package cn.xylin.skiprewardad.hook;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.HashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class GdtAdHook2 extends BaseHook {
    public GdtAdHook2(Context ctx) {
        super(ctx);
    }
    
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.qq.e.ads.rewardvideo.RewardVideoAD");
        if (claza == null) {
            return;
        }
        Field targetField = XposedHelpers.findFirstFieldByExactType(
                claza,
                findClass("com.qq.e.ads.rewardvideo.RewardVideoADListener")
        );
        if (targetField == null) {
            return;
        }
        Object err = XposedHelpers.newInstance(findClass("com.qq.e.comm.util.AdError"));
        if (err == null) {
            return;
        }
        XposedHelpers.findAndHookMethod(claza, "loadAD", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //log("GDT2-进入loadAD");
                Object adListener = XposedHelpers.getObjectField(param.thisObject, targetField.getName());
                getHandler().post(() -> {
                    //执行onADLoad通知APP广告已加载完成？？？车轮测试时不调用就会导致无法获得奖励
                    callMethod(adListener, "onADLoad");
                    //以下调用参考GdtAdHook1
                    callMethod(adListener, "onReward", new HashMap<String, Object>());
                    callMethod(adListener, "onVideoComplete");
                    callMethod(adListener, "onADClose");
                    callMethod(adListener, "onError", err);
                    log("GDT2-发放奖励");
                });
                param.setResult(null);
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
