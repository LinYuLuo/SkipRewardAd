package cn.xylin.skiprewardad.hook;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.Objects;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 未测试是否有效
 */
public class BaiduAdHook extends BaseHook {
    private Field adProd, adListener;
    public BaiduAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        claza = findClass("com.baidu.mobads.sdk.api.RewardVideoAd");
        if (claza == null) {
            return;
        }
        if (adProd == null || adListener == null) {
            try {
                Field[] fields = claza.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getType().getName().startsWith("com.baidu.mobads.sdk.internal.")) {
                        adProd = field;
                        adProd.setAccessible(true);
                        fields = field.getType().getDeclaredFields();
                        for (Field field1 : fields) {
                            if (field1.getType().getName().endsWith("ScreenVideoAdListener")) {
                                adListener = field1;
                                adListener.setAccessible(true);
                                break;
                            }
                        }
                        break;
                    }
                }
                Objects.requireNonNull(adProd);
                Objects.requireNonNull(adListener);
            } catch (Throwable ignore) {
                return;
            }
        }
        XposedHelpers.findAndHookMethod(claza, "show", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object obj = adListener.get(adProd.get(param.thisObject));
                if (obj != null) {
                    callMethod(obj, "onRewardVerify", true);
                    callMethod(obj, "playCompletion");
                    callMethod(obj, "onAdClose", 1F);
                    log("BD-发放奖励");
                    param.setResult(null);
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
