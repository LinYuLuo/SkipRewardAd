package cn.xylin.skiprewardad.hook;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TTAdHook extends BaseHook {
    private static final String BASE_ACT = "TTBaseVideoActivity";
    private static final String REWARD_ACT_1 = "TTRewardVideoActivity";
    private static final String REWARD_ACT_2 = "TTRewardVideoLandscapeActivity";
    private int hash;
    private Bundle fakeBundle;
    private Method upload, listener;
    
    public TTAdHook(Context ctx) {
        super(ctx);
    }
    
    @Override
    protected void runHook() throws Throwable {
        if (findClass("com.bytedance.sdk.openadsdk.TTRewardVideoAd") == null) {
            return;
        }
        fakeBundle = new Bundle();
        fakeBundle.putBoolean("callback_extra_key_reward_valid", true);
        fakeBundle.putBoolean("callback_extra_key_video_complete_reward", true);
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String clsName = (String) param.args[0];
                if (clsName.endsWith(BASE_ACT)) {
                    Class<?> clazz = (Class<?>) param.getResult();
                    XposedBridge.hookAllMethods(clazz, "onStart", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String clsName = param.thisObject.getClass().getName();
                            if (!clsName.endsWith(REWARD_ACT_1) && !clsName.endsWith(REWARD_ACT_2)) {
                                return;
                            }
                            int hashCode = param.thisObject.hashCode();
                            if (hashCode == hash) {
                                return;
                            }
                            hash = hashCode;
                            callReward(((Activity) param.thisObject));
                        }
                    });
                }
            }
        });
    }
    
    private boolean callReward(Activity act) {
        try {
            if (upload == null || listener == null) {
                claza = act.getClass();
                boolean isTarget = claza.getName().endsWith(REWARD_ACT_1);
                do {
                    if (isTarget) {
                        Method[] methods = claza.getDeclaredMethods();
                        for (Method method : methods) {
                            if (!Modifier.isPrivate(method.getModifiers())) {
                                continue;
                            }
                            Class<?>[] cls = method.getParameterTypes();
                            if (cls.length == 2 && String.class.isAssignableFrom(cls[0]) && Bundle.class.isAssignableFrom(cls[1])) {
                                listener = method;
                                listener.setAccessible(true);
                            } else if (cls.length == 1 && int.class.isAssignableFrom(cls[0])) {
                                upload = method;
                                upload.setAccessible(true);
                            }
                        }
                        break;
                    }
                    claza = claza.getSuperclass();
                    isTarget = claza != null && claza.getName().endsWith(REWARD_ACT_1);
                } while (claza != null);
            }
            if (listener != null) {
                listener.invoke(act, "onAdShow", null);
                if (upload != null) {
                    upload.invoke(act, 0);
                }
                fakeBundle.putInt("callback_extra_key_reward_type", 0);
                listener.invoke(act, "onRewardVerify", fakeBundle);
                fakeBundle.putInt("callback_extra_key_reward_type", 1);
                listener.invoke(act, "onRewardArrived", fakeBundle);
                listener.invoke(act, "onVideoComplete", null);
                listener.invoke(act, "onAdClose", null);
                act.finish();
                log("TTAd-发放奖励");
                return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
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
