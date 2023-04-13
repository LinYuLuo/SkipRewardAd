package cn.xylin.skiprewardad.hook;

import android.content.Context;
import java.lang.reflect.Field;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class GdtAdHook1 extends BaseHook {
    public GdtAdHook1(Context ctx) {
        super(ctx);
    }
    
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.qq.e.tg.rewardAD.TangramRewardAD");
        if (claza == null) {
            //log("GDT1-TangramRewardAD为空！");
            return;
        }
        Field targetField = XposedHelpers.findFirstFieldByExactType(
                claza,
                findClass("com.qq.e.tg.rewardAD.TangramRewardADListener")
        );
        if (targetField == null) {
            //log("GDT1-TangramRewardADListener字段不存在！");
            return;
        }
        Object result = XposedHelpers.newInstance(findClass("com.qq.e.tg.rewardAD.RewardResult"));
        Object err = XposedHelpers.newInstance(findClass("com.qq.e.comm.util.AdError"));
        if (result == null || err == null) {
            //log(String.format("GDT1-%s实例化失败！", (result == null ? "RewardResult" : "AdError")));
            return;
        }
        XposedHelpers.findAndHookMethod(claza, "loadAD", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //log("GDT1-进入loadAD");
                Object adListener = XposedHelpers.getObjectField(param.thisObject, targetField.getName());
                //使用post方式执行是为了防止无法发放奖励？？？起点测试时不使用post执行会无法发放奖励
                getHandler().post(() -> {
                    log("GDT1-发放奖励");
                    //参考GdtAdHook2
                    callMethod(adListener, "onADLoad");
                    //执行onReward发放奖励，由于奖励逻辑可能在其中一个，所以两个逻辑都执行一次。也有可能两个逻辑都可用甚至互相调用，懒得管了
                    callMethod(adListener, "onReward");
                    callMethod(adListener, "onReward", result);
                    callMethod(adListener, "onVideoComplete");
                    //执行onADClose是为了防止onError弹出错误提示？？？起点测试时不调用就会弹错误提示且无法获得奖励
                    callMethod(adListener, "onADClose");
                    //执行onError是为了方便快速结束调用？？？斗地主测试时不调用就会卡很久
                    callMethod(adListener, "onError", err);
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
