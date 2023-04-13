package cn.xylin.skiprewardad;
import android.app.Application;
import android.content.Context;
import cn.xylin.skiprewardad.hook.FusionAdHook;
import cn.xylin.skiprewardad.hook.GdtAdHook1;
import cn.xylin.skiprewardad.hook.GdtAdHook2;
import cn.xylin.skiprewardad.hook.TTAdHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Start implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam load) {
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                startHook((Context) param.args[0]);
            }
        });
    }
    
    private void startHook(Context baseContext) {
        new GdtAdHook1(baseContext);
        new GdtAdHook2(baseContext);
        new FusionAdHook(baseContext);
        new TTAdHook(baseContext);
    }
}
