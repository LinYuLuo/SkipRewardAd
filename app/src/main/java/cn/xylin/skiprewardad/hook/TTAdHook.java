package cn.xylin.skiprewardad.hook;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TTAdHook extends BaseHook {
    private static final Set<String> clsSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Method call, upload, reward, msg;
    
    public TTAdHook(Context ctx) {
        super(ctx);
    }
    
    @Override
    protected void runHook() throws Throwable {
        if (findClass("com.bytedance.sdk.openadsdk.TTRewardVideoAd") == null) {
            //log("TTAd-无法找到找到TTRewardVideoAd！");
            return;
        }
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String clsName = (String) param.args[0];
                if (clsName.endsWith("TTRewardVideoActivity")) {
                    Class<?> clazz = (Class<?>) param.getResult();
                    XposedBridge.hookAllMethods(clazz, "onStart", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
            long curTime = System.currentTimeMillis();
            int intValue = (int) (curTime / 1000);
            if (call == null || upload == null || reward == null || msg == null) {
                do {
                    Method[] methods = claza.getDeclaredMethods();
                    for (Method method : methods) {
                        Class<?>[] params = method.getParameterTypes();
                        if (isBase) {
                            if (msg == null && params.length == 1 && Message.class.isAssignableFrom(params[0])) {
                                msg = method;
                                msg.setAccessible(true);
                                break;
                            }
                        } else if (call == null || reward == null || upload == null) {
                            if (params.length == 1) {
                                int mod = method.getModifiers();
                                if (Modifier.isPrivate(mod) && int.class.isAssignableFrom(params[0])) {
                                    upload = method;
                                    upload.setAccessible(true);
                                } else if (Modifier.isPublic(mod) && String.class.isAssignableFrom(params[0])) {
                                    call = method;
                                }
                                continue;
                            }
                            if (params.length == 2 && Bundle.class.isAssignableFrom(params[0]) && boolean.class.isAssignableFrom(
                                    params[1])) {
                                reward = method;
                                reward.setAccessible(true);
                            }
                        }
                    }
                    if (isBase) {
                        break;
                    }
                    claza = claza.getSuperclass();
                    isBase = claza != null && claza.getName().endsWith("TTBaseVideoActivity");
                } while (claza != null);
            }
            if (upload != null) {
                upload.invoke(act, intValue);
                //log("调用--", upload.getName(), "--int方法成功");
            }
            if (reward != null) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("callback_extra_key_reward_valid", true);
                bundle.putInt("callback_extra_key_reward_type", 0);
                reward.invoke(act, bundle, true);
                //log("调用--", reward.getName(), "--Bundle,boolean方法成功");
            }
            act.finish();
            log("TTAd-发放奖励");
            return true;
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
