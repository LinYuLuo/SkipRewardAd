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
                /*if (clsName.endsWith("tf.gv")) {
                    Class<?> clazz = (Class<?>) param.getResult();
                    XposedHelpers.findAndHookMethod(clazz, "w", String.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            log("捕捉到--", param.method.getName(), "--String方法，参数为", param.args[0], "，返回结果为", param.getResult());
                            //param.setResult(false);
                        }
                    });
                }*/
            }
        });
        XposedBridge.hookAllMethods(Bundle.class, "getBoolean", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null && "callback_extra_key_reward_valid".equals(param.args[0])) {
                    param.setResult(true);
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
            do {
                /*Field[] fields = claza.getDeclaredFields();
                for (Field field : fields) {
                    if (Modifier.isFinal(field.getModifiers())) {
                        continue;
                    }
                    if (int.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        if (field.getInt(act) == 0) {
                            field.setInt(act, intValue);
                            log("获取一个int字段--", field.getName(), "--设置为", intValue);
                        }
                    }
                }*/
                Method[] methods = claza.getDeclaredMethods();
                for (Method method : methods) {
                    Class<?>[] params = method.getParameterTypes();
                    if (!isBase && params.length == 2 && reward == null) {
                        if (Bundle.class.isAssignableFrom(params[0]) && boolean.class.isAssignableFrom(params[1])) {
                            reward = method;
                            reward.setAccessible(true);
                        }
                    }
                    if (params.length == 1 && (call == null || upload == null || msg == null)) {
                        if (!isBase) {
                            if (Modifier.isPrivate(method.getModifiers()) && int.class.isAssignableFrom(params[0])) {
                                upload = method;
                                upload.setAccessible(true);
                            }
                            if (Modifier.isPublic(method.getModifiers()) && String.class.isAssignableFrom(params[0])) {
                                call = method;
                            }
                        } else if (Message.class.isAssignableFrom(params[0])) {
                            msg = method;
                            msg.setAccessible(true);
                            break;
                        }
                    }
                }
                if (isBase) {
                    break;
                }
                clazb = claza;
                claza = claza.getSuperclass();
                isBase = claza != null && claza.getName().endsWith("TTBaseVideoActivity");
                log("获取一次基类，当前是--", claza != null ? claza.getName() : "null", "--是Base吗？", isBase, "；之前为--", clazb.getName());
            } while (claza != null);
            if (upload != null) {
                upload.invoke(act, intValue);
                log("调用--", upload.getName(), "--int方法成功");
            }
            if (reward != null) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("callback_extra_key_reward_valid", true);
                bundle.putInt("callback_extra_key_reward_type", 0);
                reward.invoke(act, bundle, true);
                log("调用--", reward.getName(), "--Bundle,boolean方法成功");
            }
            if (call != null) {
                call.invoke(act, "onVideoComplete");
                log("调用--", call.getName(), "--String方法成功");
            }
            if (msg != null) {
                Message message = new Message();
                message.what = 300;
                msg.invoke(act, message);
                message.what = 400;
                msg.invoke(act, message);
                message.what = 500;
                msg.invoke(act, message);
                message.what = 600;
                msg.invoke(act, message);
                message.what = 700;
                msg.invoke(act, message);
                message.what = 1200;
                msg.invoke(act, message);
                log("调用--", msg.getName(), "--Message方法成功");
            }
            act.finish();
            //            getHandler().postDelayed(act::finish, 50L);
            log("TTAd-发放奖励");
            return true;
        } catch (Throwable e) {
            XposedBridge.log(e);
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
    
    /*protected void runHook() throws Throwable{
        claza = findClass("com.bytedance.sdk.openadsdk.TTRewardVideoAd");
        clazb = findClass("com.bytedance.sdk.openadsdk.TTRewardVideoAd$RewardAdInteractionListener");
        if (claza == null || clazb == null) {
            //log("TTAd-无法找到找到对应的类！", (claza == null ? "TTRewardVideoAd为空！" : "TTRewardVideoAd$RewardAdInteractionListener为空！"));
            return;
        }
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String clsName = (String) param.args[0];

                if (clsName == null || !clsName.contains("com.bytedance.sdk.openadsdk")) {
                    return;
                }
                Class<?> realClass = (Class<?>) param.getResult();
                if (realClass == null || !claza.isAssignableFrom(realClass) || Modifier.isAbstract(realClass.getModifiers()) || isHooked(
                        clsName)) {
                    //log("TTAd-", clsName, (realClass == null ? "为空！" : !claza.isAssignableFrom(realClass) ? "不是TTRewardVideoAd" : Modifier.isAbstract(realClass.getModifiers()) ? "是抽象类" : "已被HOOK"));
                    return;
                }
                if (getRewardAdInteractionListenerHook == null) {
                    getRewardAdInteractionListenerHook = new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object realObj = getRealTargetObject(clazb, param.args);
                            if (realObj == null) {
                                return;
                            }
                            log("TTAd-发放奖励");
                            getHandler().post(() -> {
                                callMethod(realObj, "onRewardVerify", true, 1, "", 0, "");
                                callMethod(realObj, "onRewardArrived", true, 1, new Bundle());
                                callMethod(realObj, "onVideoComplete");
                                callMethod(realObj, "onAdClose");
                            });
                            param.setResult(null);
                        }
                    };
                }
                try {
                    //log("TTAd-捕捉到关键类：", clsName);
                    XposedBridge.hookAllMethods(realClass,
                            "setRewardAdInteractionListener",
                            getRewardAdInteractionListenerHook
                    );
                    XposedBridge.hookAllMethods(realClass,
                            "setRewardPlayAgainInteractionListener",
                            getRewardAdInteractionListenerHook
                    );
                    XposedBridge.hookAllMethods(realClass, "showRewardVideoAd", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null);
                        }
                    });
                } catch (Throwable e) {
                    //喜马拉雅测试时有出现java.lang.NoClassDefFoundError错误，懒得管了
                    clsVector.remove(clsName);
                    //log("TTAd-未知错误！",e);
                }
            }
        });
    }*/
    @Override
    protected String targetPackageName() {
        return null;
    }
    
    @Override
    protected boolean isTarget() {
        return true;
    }
}
