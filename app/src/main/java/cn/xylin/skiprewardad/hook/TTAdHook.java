package cn.xylin.skiprewardad.hook;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TTAdHook extends BaseHook {
    private int hash;
    private Bundle fakeBundle;
    private Method upload, listener;
    private final Set<String> clsSet = Collections.newSetFromMap(new ConcurrentHashMap<>(6));
    
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
                if (clsName.endsWith("TTRewardVideoActivity")) {
                    Class<?> clazz = (Class<?>) param.getResult();
                    XposedBridge.hookAllMethods(clazz, "onStart", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
            claza = act.getClass();
            boolean isBase = false;
            do {
                if (upload == null || listener == null) {
                    Method[] methods = claza.getDeclaredMethods();
                    for (Method method : methods) {
                        Class<?>[] cls = method.getParameterTypes();
                        int mod = method.getModifiers();
                        switch (cls.length) {
                            case 1: {
                                if (!isBase && Modifier.isPrivate(mod) && int.class.isAssignableFrom(cls[0])) {
                                    upload = method;
                                    upload.setAccessible(true);
                                }
                                break;
                            }
                            case 2: {
                                if (!isBase && String.class.isAssignableFrom(cls[0]) && Bundle.class.isAssignableFrom(cls[1])) {
                                    listener = method;
                                    listener.setAccessible(true);
                                }
                                break;
                            }
                        }
                    }
                }
                if (isBase) {
                    break;
                }
                claza = claza.getSuperclass();
                isBase = claza != null && claza.getName().endsWith("TTBaseVideoActivity");
            } while (claza != null);
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
        } catch (Throwable e) {
            log(e);
        }
        return false;
    }
    
    private boolean isHooked(String className) {
        if (clsSet.contains(className)) {
            return true;
        }
        clsSet.add(className);
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
