package com.websnake.recentstv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import com.websnake.recentstv.data.RecentsSettings;
import com.websnake.recentstv.data.CardPadding;
import com.websnake.recentstv.ui.settings.SetRow;
import com.websnake.recentstv.ui.settings.SettingsSection;
import com.websnake.recentstv.data.AppDetailsMode;
import com.websnake.recentstv.data.KillAllLocation;
import com.websnake.recentstv.data.RecentTask;
import com.websnake.recentstv.data.InstalledApp;
import com.websnake.recentstv.data.RecentTasksRepository;
import com.websnake.recentstv.ui.theme.AccentColor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Runs against the installed app. Exercises complete held/repeated/released key sequences. */
public class InputSmokeTest extends Instrumentation {
    private boolean presetsOnly;
    private boolean animationOnly;
    private boolean stabilityOnly;
    private boolean closingOnly;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        animationOnly=args!=null && "true".equals(args.getString("animationOnly"));
        presetsOnly=args!=null && "true".equals(args.getString("presetsOnly"));
        stabilityOnly=args!=null && "true".equals(args.getString("stabilityOnly"));
        closingOnly=args!=null && "true".equals(args.getString("closingOnly"));
        start();
    }

    private Object value(Activity activity, String getter) {
        AtomicReference<Object> out = new AtomicReference<>();
        runOnMainSync(() -> {
            try {
                try {
                    Method m = activity.getClass().getDeclaredMethod(getter);
                    m.setAccessible(true);
                    out.set(m.invoke(activity));
                } catch (NoSuchMethodException noGetter) {
                    String name=Character.toLowerCase(getter.charAt(3))+getter.substring(4);
                    java.lang.reflect.Field field=activity.getClass().getDeclaredField(name);
                    field.setAccessible(true);
                    out.set(field.get(activity));
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        return out.get();
    }
    private void require(boolean okay, String message) {
        if (!okay) throw new AssertionError(message);
    }
    private void event(Activity a, int action, int code, int repeat, long downTime) {
        runOnMainSync(() -> a.dispatchKeyEvent(new KeyEvent(
            downTime, SystemClock.uptimeMillis(), action, code, repeat)));
    }
    private void tap(Activity a, int code) {
        long t=SystemClock.uptimeMillis();
        event(a,KeyEvent.ACTION_DOWN,code,0,t);
        event(a,KeyEvent.ACTION_UP,code,0,t);
        SystemClock.sleep(100);
    }
    private void steps(Activity a, int code, int n) {
        for(int i=0;i<n;i++) tap(a,code);
    }
    private void go(Activity a, SetRow row) {
        SettingsSection wanted=SettingsSection.Companion.forRow(row);
        SettingsSection currentSection=(SettingsSection)value(a,"getSection");
        if(currentSection!=wanted) {
            if(currentSection!=null) tap(a,KeyEvent.KEYCODE_BACK);
            int target=wanted==null ? com.websnake.recentstv.ui.settings.SettingsMenu.INSTANCE.getReset() : wanted.ordinal();
            int root=(Integer)value(a,"getRootFocus");
            steps(a,root<target ? KeyEvent.KEYCODE_DPAD_DOWN : KeyEvent.KEYCODE_DPAD_UP,Math.abs(target-root));
            if(wanted!=null) tap(a,KeyEvent.KEYCODE_DPAD_CENTER);
        } else if(wanted==null) {
            int root=(Integer)value(a,"getRootFocus");
            steps(a,KeyEvent.KEYCODE_DPAD_DOWN,com.websnake.recentstv.ui.settings.SettingsMenu.INSTANCE.getReset()-root);
        }
        if(wanted==null) return;
        int target=wanted.getRows().indexOf(row);
        int current=(Integer)value(a,"getSetFocus");
        steps(a, current < target ? KeyEvent.KEYCODE_DPAD_DOWN : KeyEvent.KEYCODE_DPAD_UP,
            Math.abs(target-current));
    }
    private Activity open(Class<?> activityClass) {
        return startActivitySync(new Intent(getTargetContext(),activityClass)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void pass(String name) {
        Bundle b=new Bundle(); b.putString("stream","PASS: "+name+System.lineSeparator()); sendStatus(1,b);
    }
    private void awaitOverlay(Activity a) {
        for(int i=0;i<100 && !"OVERLAY".equals(String.valueOf(value(a,"getScene")));i++) SystemClock.sleep(100);
        require("OVERLAY".equals(String.valueOf(value(a,"getScene"))),"Overlay did not resume");
    }
    private void focusPackage(Activity recents,String pkg) {
        List<RecentTask> tasks=(List<RecentTask>)value(recents,"getTasks");
        int target=-1;
        for(int i=0;i<tasks.size();i++) if(pkg.equals(tasks.get(i).getPackageName())) target=i;
        require(target>=0,"Protected task missing from carousel");
        RecentsSettings config=(RecentsSettings)value(recents,"getSettings");
        if(config.getKillAllLocation()==KillAllLocation.APP_ROW && Boolean.TRUE.equals(value(recents,"getPrivileged"))) target++;
        int current=(Integer)value(recents,"getFocus");
        boolean olderRight="OLDER_RIGHT".equals(((RecentsSettings)value(recents,"getSettings")).getDirection().name());
        int key=(target>current)==olderRight ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
        steps(recents,key,Math.abs(target-current));
    }
    private void heldMenu(Activity recents) {
        long now=SystemClock.uptimeMillis();
        event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0,now);
        SystemClock.sleep(650);
        event(recents,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0,now);
        require(value(recents,"getCtxTask")!=null,"Context menu did not open");
    }
    private void saveScreen(String name) throws Exception {
        SystemClock.sleep(250);
        try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(getTargetContext().getFilesDir(),name))) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);
        }
    }
    private void openAppList(Activity settings, SetRow row) {
        go(settings,row);
        runOnMainSync(()->{
            long now=SystemClock.uptimeMillis();
            settings.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));
            try {
                Method loading=settings.getClass().getDeclaredMethod("getAppsLoading"); loading.setAccessible(true);
                require(Boolean.TRUE.equals(loading.invoke(settings)),"App chooser did not enter loading state synchronously");
                settings.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN,0));
                Method focus=settings.getClass().getDeclaredMethod("getExcFocus"); focus.setAccessible(true);
                require(Integer.valueOf(0).equals(focus.invoke(settings)),"Loading chooser accepted navigation");
            } catch(Exception e) { throw new RuntimeException(e); }
        });
        for(int i=0;i<100 && Boolean.TRUE.equals(value(settings,"getAppsLoading"));i++) SystemClock.sleep(100);
        require(Boolean.FALSE.equals(value(settings,"getAppsLoadError")),"App chooser reported a load error");
        require(!((List<?>)value(settings,"getApps")).isEmpty(),"App chooser did not load installed apps");
    }
    private void topControls(Activity recents) throws Exception {
        Activity config=open(SettingsActivity.class);
        go(config,SetRow.KILL_ALL);
        steps(config,KeyEvent.KEYCODE_DPAD_LEFT,2); tap(config,KeyEvent.KEYCODE_DPAD_RIGHT);
        go(config,SetRow.PRIVILEGED); tap(config,KeyEvent.KEYCODE_DPAD_RIGHT);
        go(config,SetRow.CONFIRM_KILL_ALL); tap(config,KeyEvent.KEYCODE_DPAD_RIGHT);
        go(config,SetRow.INDIVIDUAL_PROTECTION); tap(config,KeyEvent.KEYCODE_DPAD_RIGHT);
        go(config,SetRow.SETTINGS_BUTTON); tap(config,KeyEvent.KEYCODE_DPAD_RIGHT);
        tap(config,KeyEvent.KEYCODE_BACK); tap(config,KeyEvent.KEYCODE_BACK);
        awaitOverlay(recents);
        require(Boolean.TRUE.equals(value(recents,"hasTopKillAll")),"Porter top Kill All is missing");
        require(!String.valueOf(value(recents,"buildItems")).contains("KillAll"),"Top Kill All also appeared in the app row");
        List<RecentTask> recent=(List<RecentTask>)value(recents,"getTasks");
        Activity protection=open(SettingsActivity.class);
        go(protection,SetRow.EXCEPTIONS);
        runOnMainSync(()->{
            long now=SystemClock.uptimeMillis();
            protection.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));
            protection.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK,0));
        });
        require("SETTINGS".equals(String.valueOf(value(protection,"getScreen"))) && Boolean.FALSE.equals(value(protection,"getAppsLoading")),"BACK did not cancel an in-progress list load");
        openAppList(protection,SetRow.PROTECTED_APPS);
        Method setLoading=protection.getClass().getDeclaredMethod("setAppsLoading",boolean.class);
        setLoading.setAccessible(true);
        runOnMainSync(()->{try{setLoading.invoke(protection,true);}catch(Exception e){throw new RuntimeException(e);}});
        saveScreen("smoke-protected-loading.png");
        runOnMainSync(()->{try{setLoading.invoke(protection,false);}catch(Exception e){throw new RuntimeException(e);}});
        for(int i=0;i<40 && ((List<?>)value(protection,"getApps")).isEmpty();i++) SystemClock.sleep(100);
        List<InstalledApp> installed=(List<InstalledApp>)value(protection,"getApps");
        int match=-1;
        for(int i=0;i<installed.size() && match<0;i++) {
            String pkg=installed.get(i).getPackageName();
            for(RecentTask task:recent) if(pkg.equals(task.getPackageName())) { match=i; break; }
        }
        require(match>=0,"No recent app found in protection chooser");
        String protectedPackage=installed.get(match).getPackageName();
        steps(protection,KeyEvent.KEYCODE_DPAD_DOWN,match);
        RecentsSettings protectionBefore=(RecentsSettings)value(protection,"getSettings");
        if(!protectionBefore.getProtectedApps().contains(protectedPackage)) tap(protection,KeyEvent.KEYCODE_DPAD_CENTER);
        SystemClock.sleep(200);
        RecentsSettings protectionAfter=(RecentsSettings)value(protection,"getSettings");
        require(protectionAfter.getProtectedApps().contains(protectedPackage),"Protection choice was not saved");
        require(protectionBefore.getExceptions().equals(protectionAfter.getExceptions()),"Protection changed the hidden-app list");
        tap(protection,KeyEvent.KEYCODE_BACK);
        require(String.valueOf(value(protection,"protectedSummary")).startsWith(protectionAfter.getProtectedApps().size()+" "),"Protected app count does not match saved choices");
        tap(protection,KeyEvent.KEYCODE_BACK); tap(protection,KeyEvent.KEYCODE_BACK);
        awaitOverlay(recents);
        List<RecentTask> candidates=(List<RecentTask>)value(recents,"killAllCandidates");
        for(RecentTask candidate:candidates) require(!protectedPackage.equals(candidate.getPackageName()),"Protected app remained a Kill All candidate");
        require(((List<RecentTask>)value(recents,"getTasks")).stream().anyMatch(t->protectedPackage.equals(t.getPackageName())),"Protection hid the recent app");
        pass("Protected apps remain visible, are omitted from Kill All candidates, and do not alter hidden-app preferences");
        focusPackage(recents,protectedPackage);
        int protectedCount=((List<?>)value(recents,"getTasks")).size();
        tap(recents,KeyEvent.KEYCODE_DPAD_DOWN); tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(-1).equals(value(recents,"getPeekDir")) && value(recents,"getDismissing")==null,"DOWN armed removal of a protected app");
        heldMenu(recents); tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(2).equals(value(recents,"getCtxSelected")),"Protected context-menu close remained selectable");
        saveScreen("smoke-privileged-menu.png");
        tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        for(int i=0;i<30 && ((RecentsSettings)value(recents,"getSettings")).getProtectedApps().contains(protectedPackage);i++) SystemClock.sleep(100);
        require(!((RecentsSettings)value(recents,"getSettings")).getProtectedApps().contains(protectedPackage),"Unprotect menu action did not persist");
        require(((List<?>)value(recents,"getTasks")).size()==protectedCount,"Unprotect removed the app");
        heldMenu(recents); tap(recents,KeyEvent.KEYCODE_DPAD_DOWN); tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(2).equals(value(recents,"getCtxSelected")),"Protect menu action unavailable in privileged mode");
        tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        for(int i=0;i<30 && !((RecentsSettings)value(recents,"getSettings")).getProtectedApps().contains(protectedPackage);i++) SystemClock.sleep(100);
        require(((RecentsSettings)value(recents,"getSettings")).getProtectedApps().contains(protectedPackage),"Protect menu action did not persist");
        pass("Privileged context menu protects/unprotects immediately without hiding the app");
        RecentTask protectedTask=((List<RecentTask>)value(recents,"getTasks")).stream().filter(t->protectedPackage.equals(t.getPackageName())).findFirst().get();
        require(!new RecentTasksRepository(getTargetContext()).dismiss(protectedTask,true,java.util.Collections.singleton(protectedPackage)),"Repository accepted a protected individual close");
        require(((List<?>)value(recents,"getTasks")).size()==protectedCount,"Protected close changed the carousel");
        Activity individual=open(SettingsActivity.class);
        go(individual,SetRow.INDIVIDUAL_PROTECTION); tap(individual,KeyEvent.KEYCODE_DPAD_LEFT);
        tap(individual,KeyEvent.KEYCODE_BACK); tap(individual,KeyEvent.KEYCODE_BACK);
        awaitOverlay(recents); focusPackage(recents,protectedPackage);
        tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(1).equals(value(recents,"getPeekDir")),"Disabling individual protection did not enable the close preview");
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        heldMenu(recents); tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(1).equals(value(recents,"getCtxSelected")),"Disabled individual protection still blocks context close");
        tap(recents,KeyEvent.KEYCODE_BACK);
        require(((List<RecentTask>)value(recents,"killAllCandidates")).stream().noneMatch(t->protectedPackage.equals(t.getPackageName())),"Individual toggle disabled bulk protection");
        pass("Individual protection blocks DOWN and context close; OFF permits preview while bulk protection remains");
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        require(Boolean.TRUE.equals(value(recents,"getGearFocused")),"UP did not focus gear");
        tap(recents,KeyEvent.KEYCODE_DPAD_LEFT);
        require(Boolean.TRUE.equals(value(recents,"getTopKillFocused")),"LEFT did not focus top Kill All");
        List<?> before=(List<?>)value(recents,"getTasks");
        int countBefore=before.size();
        long opening=SystemClock.uptimeMillis();
        event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0,opening);
        require(Boolean.TRUE.equals(value(recents,"getKillPrompt")),"Kill All did not open review dialog");
        require(Boolean.FALSE.equals(value(recents,"getConfirmKill")),"Kill All did not default to Cancel");
        for(int i=1;i<=6;i++) event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,i,opening);
        event(recents,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0,opening);
        require(Boolean.TRUE.equals(value(recents,"getKillPrompt")) && Boolean.FALSE.equals(value(recents,"getKilling")),"Opening hold confirmed Kill All");
        tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.FALSE.equals(value(recents,"getKillPrompt")),"Default Cancel did not close review");
        require(((List<?>)value(recents,"getTasks")).size()==countBefore,"Cancel changed recent apps");
        tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.TRUE.equals(value(recents,"getKillPrompt")),"Second review did not open");
        tap(recents,KeyEvent.KEYCODE_DPAD_RIGHT);
        require(((Boolean)value(recents,"getConfirmKill"))==!((List<?>)value(recents,"killAllCandidates")).isEmpty(),"Review selection disagrees with eligible apps");
        tap(recents,KeyEvent.KEYCODE_BACK);
        require(((List<?>)value(recents,"getTasks")).size()==countBefore && Boolean.FALSE.equals(value(recents,"getKilling")),"BACK from selected confirmation acted on apps");
        pass("Kill All review defaults to Cancel; opening hold/repeats/release and BACK cannot close apps");
        tap(recents,KeyEvent.KEYCODE_DPAD_RIGHT);
        require(Boolean.TRUE.equals(value(recents,"getGearFocused")) && Boolean.FALSE.equals(value(recents,"getTopKillFocused")),"RIGHT did not return to gear");
        tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Boolean.FALSE.equals(value(recents,"getGearFocused")),"DOWN did not return to app row");
        pass("Porter top Kill All is separate from app row; UP/LEFT/RIGHT/DOWN navigation works without activation");
        Activity hidden=open(SettingsActivity.class);
        go(hidden,SetRow.SETTINGS_BUTTON); tap(hidden,KeyEvent.KEYCODE_DPAD_LEFT);
        tap(hidden,KeyEvent.KEYCODE_BACK); tap(hidden,KeyEvent.KEYCODE_BACK);
        awaitOverlay(recents);
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        require(Boolean.FALSE.equals(value(recents,"getGearFocused")) && Boolean.TRUE.equals(value(recents,"getTopKillFocused")),"First UP did not select top Kill All");
        require(Boolean.TRUE.equals(value(recents,"getSettingsHintVisible")),"Top Kill All did not show settings notification");
        SystemClock.sleep(2800);
        require(Boolean.TRUE.equals(value(recents,"getTopKillFocused")),"Kill All lost focus after a pause");
        tap(recents,KeyEvent.KEYCODE_DPAD_LEFT);
        require(Boolean.TRUE.equals(value(recents,"getTopKillFocused")) && Boolean.FALSE.equals(value(recents,"getGearFocused")),"Hidden gear is separately selectable");
        for(int i=1;i<=4;i++) event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP,i,SystemClock.uptimeMillis());
        require(Boolean.TRUE.equals(value(recents,"getTopKillFocused")) && "OVERLAY".equals(String.valueOf(value(recents,"getScene"))),"Held UP repeats opened settings");
        ActivityMonitor monitor=addMonitor(SettingsActivity.class.getName(),null,false);
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        Activity fallback=waitForMonitorWithTimeout(monitor,3000);
        removeMonitor(monitor);
        require(fallback!=null,"Fresh second UP refocused Kill All instead of opening settings");
        pass("Hidden settings with top Kill All uses a notification; UP selects Kill All, second UP opens Settings");
        go(fallback,SetRow.PRIVILEGED); tap(fallback,KeyEvent.KEYCODE_DPAD_LEFT);
        tap(fallback,KeyEvent.KEYCODE_BACK); tap(fallback,KeyEvent.KEYCODE_BACK);
        awaitOverlay(recents);
        require(Boolean.FALSE.equals(value(recents,"hasTopKillAll")),"Fallback exposes top Kill All");
        require(!String.valueOf(value(recents,"buildItems")).contains("KillAll"),"Fallback exposes row Kill All");
        pass("Fallback hides both Kill All locations");
        Object appFocus=value(recents,"getFocus");
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        require(appFocus.equals(value(recents,"getFocus")) && Boolean.FALSE.equals(value(recents,"getGearFocused")) && Boolean.FALSE.equals(value(recents,"getTopKillFocused")),"Notification stole focus from the selected app");
        require(Boolean.TRUE.equals(value(recents,"getSettingsHintVisible")),"First UP did not show notification");
        saveScreen("smoke-focus-notification.png");
        SystemClock.sleep(3200);
        require(Boolean.FALSE.equals(value(recents,"getSettingsHintVisible")) && appFocus.equals(value(recents,"getFocus")),"Notification did not expire without changing focus");
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        for(int i=1;i<=4;i++) event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP,i,SystemClock.uptimeMillis());
        require(Boolean.TRUE.equals(value(recents,"getSettingsHintVisible")) && "OVERLAY".equals(String.valueOf(value(recents,"getScene"))),"Held UP opened Settings");
        tap(recents,KeyEvent.KEYCODE_DPAD_RIGHT);
        require(Boolean.FALSE.equals(value(recents,"getSettingsHintVisible")),"Browsing did not clear the notification");
        Object nextFocus=value(recents,"getFocus");
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        require(nextFocus.equals(value(recents,"getFocus")),"First UP changed app focus");
        ActivityMonitor fallbackMonitor=addMonitor(SettingsActivity.class.getName(),null,false);
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        Activity fallbackSettings=waitForMonitorWithTimeout(fallbackMonitor,3000);
        removeMonitor(fallbackMonitor);
        require(fallbackSettings!=null,"Second UP did not open fallback Settings");
        openAppList(fallbackSettings,SetRow.EXCEPTIONS);
        Method setExceptionLoading=fallbackSettings.getClass().getDeclaredMethod("setAppsLoading",boolean.class);
        setExceptionLoading.setAccessible(true);
        runOnMainSync(()->{try{setExceptionLoading.invoke(fallbackSettings,true);}catch(Exception e){throw new RuntimeException(e);}});
        saveScreen("smoke-exceptions-loading.png");
        runOnMainSync(()->{try{setExceptionLoading.invoke(fallbackSettings,false);}catch(Exception e){throw new RuntimeException(e);}});
        tap(fallbackSettings,KeyEvent.KEYCODE_BACK);
        tap(fallbackSettings,KeyEvent.KEYCODE_BACK);
        tap(fallbackSettings,KeyEvent.KEYCODE_BACK);
        awaitOverlay(recents);
        pass("First UP preserves app focus; notification expires or clears on browsing; fresh second UP opens Settings");
    }
    private void rootRow(Activity settings, int index) {
        if (value(settings,"getSection")!=null) tap(settings,KeyEvent.KEYCODE_BACK);
        int current=(Integer)value(settings,"getRootFocus");
        steps(settings,current<index ? KeyEvent.KEYCODE_DPAD_DOWN : KeyEvent.KEYCODE_DPAD_UP,Math.abs(index-current));
    }
    private void testPresets() throws Exception {
        Activity settings=open(SettingsActivity.class);
        SystemClock.sleep(600);
        RecentsSettings before=(RecentsSettings)value(settings,"getSettings");
        int presets=com.websnake.recentstv.ui.settings.SettingsMenu.INSTANCE.getPresets();
        rootRow(settings,presets);tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        require("PRESETS".equals(String.valueOf(value(settings,"getScreen"))),"Preset screen did not open");
        require(Boolean.FALSE.equals(value(settings,"getCanRestoreAppearance")),"Restore available before a preset was applied");
        steps(settings,KeyEvent.KEYCODE_DPAD_DOWN,com.websnake.recentstv.data.AppearancePreset.values().length);
        tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.FALSE.equals(value(settings,"getPresetPrompt")),"Unavailable Restore opened a review");
        steps(settings,KeyEvent.KEYCODE_DPAD_UP,com.websnake.recentstv.data.AppearancePreset.values().length);
        require(com.websnake.recentstv.data.AppearancePreset.DEFAULT.appearance().equals(new RecentsSettings()),"Default preset differs from Reset defaults");
        RecentsSettings clean=com.websnake.recentstv.data.AppearancePreset.CLEAN.appearance();
        require(clean.getShowAppNames() && clean.getCardPadding()==CardPadding.NORMAL && clean.getKillAllLocation()==KillAllLocation.APP_ROW,"Clean does not match requested title/padding/Kill All choices");
        saveScreen("smoke-presets.png");
        tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.FALSE.equals(value(settings,"getConfirmPreset")),"Preset review not cancel-first");
        event(settings,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,3,SystemClock.uptimeMillis());
        require(Boolean.TRUE.equals(value(settings,"getPresetPrompt")),"Opening hold dismissed preset review");
        tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        require(before.equals(value(settings,"getSettings")),"Cancel changed settings");
        tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
        tap(settings,KeyEvent.KEYCODE_BACK);
        require(before.equals(value(settings,"getSettings")),"BACK from Apply changed settings");
        pass("Preset review is cancel-first; held OK, Cancel and BACK do not change settings");
        String[] visual={"Position","Direction","Size","Shape","CardPadding","Accent","CustomAccent","BgDim","BgTint","ShowHeader","ShowSettingsButton","ShowHelp","ShowAppNames","AppTextPosition","AppDetailsMode","CloseHintStyle","SmoothMotion","CloseAnimationSpeed","KillAllLocation"};
        String[] kept={"Exceptions","ProtectedApps","ProtectIndividualCloses","ExcludeHiddenFromKillAll","ConfirmKillAll","ForceStopOnKill","UsePrivileged","AutoCheckUpdates","ShowUpdateNotification"};
        RecentsSettings last=before;
        for(com.websnake.recentstv.data.AppearancePreset preset:com.websnake.recentstv.data.AppearancePreset.values()) {
            if(preset.ordinal()>0) tap(settings,KeyEvent.KEYCODE_DPAD_DOWN);
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            require(Boolean.FALSE.equals(value(settings,"getConfirmPreset")),"Next review retained Apply selection");
            if(preset.ordinal()==0) saveScreen("smoke-preset-review.png");
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            SystemClock.sleep(400);
            RecentsSettings actual=(RecentsSettings)value(settings,"getSettings");
            RecentsSettings expected=preset.appearance();
            for(String field:visual) {
                Method m=RecentsSettings.class.getMethod("get"+field);
                require(java.util.Objects.equals(m.invoke(actual),m.invoke(expected)),preset+" visual field mismatch: "+field);
            }
            for(String field:kept) {
                Method m=RecentsSettings.class.getMethod("get"+field);
                require(java.util.Objects.equals(m.invoke(actual),m.invoke(before)),preset+" changed operational setting: "+field);
            }
            last=actual;
            require(Boolean.TRUE.equals(value(settings,"getCanRestoreAppearance")),"Preset did not save a restore point");
            pass(preset+" applies appearance atomically and preserves lists, service and closing choices");
        }
        tap(settings,KeyEvent.KEYCODE_BACK);
        require(Integer.valueOf(presets).equals(value(settings,"getRootFocus")),"Preset BACK lost root focus");
        int about=com.websnake.recentstv.ui.settings.SettingsMenu.INSTANCE.getAbout();
        rootRow(settings,about);tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        require("ABOUT".equals(String.valueOf(value(settings,"getScreen"))),"About did not open");
        saveScreen("smoke-about.png");
        tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        require(last.equals(value(settings,"getSettings")),"Read-only About changed settings");
        tap(settings,KeyEvent.KEYCODE_BACK);
        require(Integer.valueOf(about).equals(value(settings,"getRootFocus")),"About BACK lost root focus");
        rootRow(settings,com.websnake.recentstv.ui.settings.SettingsMenu.INSTANCE.getReset());
        tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.TRUE.equals(value(settings,"getResetPrompt")),"Moved Reset row no longer opens review");
        tap(settings,KeyEvent.KEYCODE_BACK);tap(settings,KeyEvent.KEYCODE_BACK);
        Activity reopened=open(SettingsActivity.class);SystemClock.sleep(600);
        require(last.equals(value(reopened,"getSettings")),"Appearance did not persist across activity recreation");
        pass("About is read-only; root focus returns correctly; Reset remains reachable; preset persists on reopening");
        require(Boolean.TRUE.equals(value(reopened,"getCanRestoreAppearance")),"Restore point did not survive reopening");
        // Change a closing preference after the checkpoint: Restore must keep the new value.
        go(reopened,SetRow.CONFIRM_KILL_ALL);tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);SystemClock.sleep(200);
        RecentsSettings beforeRestore=(RecentsSettings)value(reopened,"getSettings");
        rootRow(reopened,presets);tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);
        steps(reopened,KeyEvent.KEYCODE_DPAD_DOWN,com.websnake.recentstv.data.AppearancePreset.values().length);
        tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.FALSE.equals(value(reopened,"getConfirmPreset")),"Restore review not cancel-first");
        saveScreen("smoke-restore-review.png");
        event(reopened,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,4,SystemClock.uptimeMillis());
        require(Boolean.TRUE.equals(value(reopened,"getPresetPrompt")),"Held OK dismissed Restore review");
        tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);
        require(beforeRestore.equals(value(reopened,"getSettings")),"Cancel Restore changed settings");
        tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);tap(reopened,KeyEvent.KEYCODE_DPAD_RIGHT);tap(reopened,KeyEvent.KEYCODE_BACK);
        require(beforeRestore.equals(value(reopened,"getSettings")),"BACK Restore changed settings");
        tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);tap(reopened,KeyEvent.KEYCODE_DPAD_RIGHT);tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);
        SystemClock.sleep(400);
        RecentsSettings restored=(RecentsSettings)value(reopened,"getSettings");
        for(String field:visual) {
            Method m=RecentsSettings.class.getMethod("get"+field);
            require(java.util.Objects.equals(m.invoke(restored),m.invoke(before)),"Restore did not return the ORIGINAL look: "+field);
        }
        for(String field:kept) {
            Method m=RecentsSettings.class.getMethod("get"+field);
            require(java.util.Objects.equals(m.invoke(restored),m.invoke(beforeRestore)),"Restore reverted an operational choice: "+field);
        }
        require(Boolean.FALSE.equals(value(reopened,"getCanRestoreAppearance")),"Restore point was not consumed");
        tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.FALSE.equals(value(reopened,"getPresetPrompt")),"Consumed Restore opened again");
        // A new preset session must create a fresh checkpoint, including restored custom color.
        steps(reopened,KeyEvent.KEYCODE_DPAD_UP,com.websnake.recentstv.data.AppearancePreset.values().length);
        tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);tap(reopened,KeyEvent.KEYCODE_DPAD_RIGHT);tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);
        SystemClock.sleep(250);
        require(Boolean.TRUE.equals(value(reopened,"getCanRestoreAppearance")),"New session did not save a checkpoint");
        steps(reopened,KeyEvent.KEYCODE_DPAD_DOWN,com.websnake.recentstv.data.AppearancePreset.values().length);
        tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);tap(reopened,KeyEvent.KEYCODE_DPAD_RIGHT);tap(reopened,KeyEvent.KEYCODE_DPAD_CENTER);
        SystemClock.sleep(300);
        require(restored.equals(value(reopened,"getSettings")),"New restore cycle lost the saved appearance");
        pass("Restore keeps the pre-FIRST-preset appearance and custom color across preset changes/reopening, preserves later closing choices, and supports a new session");
    }

    private void phase(String name) throws Exception {
        try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(getTargetContext().getFilesDir(),"stability-phase.txt"))) {
            file.write(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    private void awaitMode(Activity recents,String scene,boolean privileged) {
        for(int i=0;i<300;i++) {
            if(scene.equals(String.valueOf(value(recents,"getScene"))) && Boolean.valueOf(privileged).equals(value(recents,"getPrivileged"))) {
                if("OVERLAY".equals(scene)) {
                    List<RecentTask> tasks=(List<RecentTask>)value(recents,"getTasks");
                    require(!tasks.isEmpty(),"Mode has no app entries");
                    for(RecentTask task:tasks) require(task.isUsageEntry()!=privileged,"Mode contains an entry from the previous data source");
                }
                require(!recents.isFinishing(),"Mode change finished the overlay");
                return;
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("Timed out awaiting "+scene+" privileged="+privileged+", actual="+value(recents,"getScene")+"/"+value(recents,"getPrivileged"));
    }
    private void testStability() throws Exception {
        Activity recents=open(RecentsActivity.class);
        awaitMode(recents,"OVERLAY",true);
        saveScreen("stability-native.png");
        int startFocus=(Integer)value(recents,"getFocus");
        tap(recents,KeyEvent.KEYCODE_DPAD_LEFT);tap(recents,KeyEvent.KEYCODE_DPAD_RIGHT);
        require(Integer.valueOf(startFocus).equals(value(recents,"getFocus")),"LEFT/RIGHT did not return to selected app");
        long held=SystemClock.uptimeMillis();
        event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0,held);SystemClock.sleep(650);
        for(int i=1;i<=8;i++) event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,i,held);
        event(recents,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0,held);
        require(value(recents,"getCtxTask")!=null,"Held OK/release hid the menu");
        phase("STOP_ONE");
        awaitMode(recents,"OVERLAY",false);
        require(value(recents,"getCtxTask")==null,"Native menu survived service death");
        require(Boolean.FALSE.equals(value(recents,"getKillPrompt")),"Stale Kill All review survived mode change");
        require(!String.valueOf(value(recents,"buildItems")).contains("KillAll"),"Fallback retained Kill All");
        tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(-1).equals(value(recents,"getPeekDir")),"Fallback allowed close preview");
        require(Boolean.FALSE.equals(value(recents,"getBusy")),"Service death left input busy");
        saveScreen("stability-fallback.png");
        pass("Live Porter death switches to usage history in the same activity, clears native menu/close controls and keeps input usable");
        phase("DENY_HISTORY");awaitMode(recents,"USAGE",false);
        saveScreen("stability-permission-denied.png");
        pass("Revoking Usage Access while open shows the permission explanation without crashing");
        phase("ALLOW_HISTORY");awaitMode(recents,"OVERLAY",false);
        pass("Restoring Usage Access while open repopulates usage history automatically");
        phase("RESTART_ONE");awaitMode(recents,"OVERLAY",true);
        saveScreen("stability-reconnected.png");
        pass("Restarted Porter automatically restores actual tasks and privileged controls in the same activity");
        phase("DENY_NATIVE");
        // Host acknowledges the app-op change before this assertion; the phase remains until then.
        java.io.File ack=new java.io.File(getTargetContext().getFilesDir(),"stability-ack.txt");
        for(int i=0;i<300 && !ack.exists();i++) SystemClock.sleep(100);
        require(ack.exists(),"Host did not acknowledge native-mode permission change");
        SystemClock.sleep(1800);awaitMode(recents,"OVERLAY",true);
        phase("STOP_TWO");awaitMode(recents,"USAGE",false);
        phase("RESTART_TWO");awaitMode(recents,"OVERLAY",true);
        pass("Porter tasks remain available without Usage Access; another live stop shows permission UI and restart recovers");
        phase("RESTORE_ACCESS");
        java.io.File accessAck=new java.io.File(getTargetContext().getFilesDir(),"stability-access-restored.txt");
        for(int i=0;i<300 && !accessAck.exists();i++) SystemClock.sleep(100);
        require(accessAck.exists(),"Host did not restore Usage Access");
        awaitMode(recents,"OVERLAY",true);
        Object activeScene=value(recents,"getScene");
        Method setScene=recents.getClass().getDeclaredMethod("setScene",activeScene.getClass());setScene.setAccessible(true);
        Object emptyScene=java.lang.Enum.valueOf((Class)activeScene.getClass(),"EMPTY");
        runOnMainSync(()->{try{setScene.invoke(recents,emptyScene);}catch(Exception e){throw new RuntimeException(e);}});
        saveScreen("stability-empty.png");
        ActivityMonitor monitor=addMonitor(SettingsActivity.class.getName(),null,false);
        tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        Activity settings=waitForMonitorWithTimeout(monitor,3000);removeMonitor(monitor);
        require(settings!=null,"Empty UP did not open Settings");
        tap(settings,KeyEvent.KEYCODE_BACK);awaitMode(recents,"OVERLAY",true);
        runOnMainSync(()->{try{setScene.invoke(recents,emptyScene);}catch(Exception e){throw new RuntimeException(e);}});
        tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);require(recents.isFinishing(),"Empty OK did not exit");
        pass("Injected empty-state UP opens Settings and OK exits; no actual tasks removed");
        phase("DONE");
    }

    private static final String ALPHA="com.websnake.recentstv.fixture.alpha";
    private static final String BETA="com.websnake.recentstv.fixture.beta";
    private static final String GAMMA="com.websnake.recentstv.fixture.gamma";
    private void fixtureGuard(Activity recents) {
        require(Boolean.TRUE.equals(value(recents,"getPrivileged")),"Real closing needs authorized Porter");
        for(RecentTask task:(List<RecentTask>)value(recents,"getTasks"))
            require(java.util.Arrays.asList(ALPHA,BETA,GAMMA).contains(task.getPackageName()),"SAFETY: non-fixture task in overlay: "+task.getPackageName());
        for(RecentTask task:(List<RecentTask>)value(recents,"killAllCandidates"))
            require(ALPHA.equals(task.getPackageName()),"SAFETY: unexpected bulk candidate: "+task.getPackageName());
    }
    private void closingPhase(String name) throws Exception {
        try(java.io.FileOutputStream f=new java.io.FileOutputStream(new java.io.File(getTargetContext().getFilesDir(),"closing-phase.txt"))) {
            f.write(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        java.io.File ack=new java.io.File(getTargetContext().getFilesDir(),"closing-ack-"+name);
        for(int i=0;i<400 && !ack.exists();i++) SystemClock.sleep(100);
        require(ack.exists(),"Host verification timed out: "+name);
    }
    private void idle(Activity recents) {
        for(int i=0;i<200;i++) {
            if(Boolean.FALSE.equals(value(recents,"getBusy")) && Boolean.FALSE.equals(value(recents,"getKilling")) && value(recents,"getDismissing")==null) return;
            SystemClock.sleep(100);
        }
        throw new AssertionError("Closing animation/action stayed busy");
    }
    private void configureClose(Activity recents,SetRow row,boolean enabled) {
        Activity config=open(SettingsActivity.class);go(config,row);
        tap(config,enabled ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT);
        tap(config,KeyEvent.KEYCODE_BACK);tap(config,KeyEvent.KEYCODE_BACK);
        awaitOverlay(recents);
    }
    private void killFocus(Activity recents) {
        fixtureGuard(recents);
        require(((RecentsSettings)value(recents,"getSettings")).getKillAllLocation()==KillAllLocation.APP_ROW,"Unexpected bulk placement");
        int current=(Integer)value(recents,"getFocus");
        boolean olderRight="OLDER_RIGHT".equals(((RecentsSettings)value(recents,"getSettings")).getDirection().name());
        steps(recents,olderRight ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT,current);
        require(Integer.valueOf(0).equals(value(recents,"getFocus")),"Kill All not focused");
    }
    private void testClosing() throws Exception {
        Activity recents=open(RecentsActivity.class);awaitMode(recents,"OVERLAY",true);fixtureGuard(recents);
        require(((List<?>)value(recents,"getTasks")).size()==2,"Expected only Alpha and protected Beta; Gamma must be hidden");
        focusPackage(recents,ALPHA);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(1).equals(value(recents,"getPeekDir")),"First DOWN did not arm close");
        saveScreen("closing-close-hint.png");tap(recents,KeyEvent.KEYCODE_DPAD_UP);
        require(Integer.valueOf(-1).equals(value(recents,"getPeekDir")),"UP did not cancel close preview");
        closingPhase("CANCEL_INDIVIDUAL");
        fixtureGuard(recents);focusPackage(recents,ALPHA);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        idle(recents);closingPhase("REMOVED_NO_FORCE_STOP");
        pass("Real DOWN close removes all Alpha tasks; canceled preview kept tasks; force-stop Off leaves its test service running");
        configureClose(recents,SetRow.FORCE_STOP,true);
        closingPhase("RESPAWN_ALPHA");awaitMode(recents,"OVERLAY",true);fixtureGuard(recents);
        focusPackage(recents,ALPHA);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        idle(recents);closingPhase("REMOVED_WITH_FORCE_STOP");
        pass("Force-stop On removes Alpha tasks, stops its process/service and sets Android's stopped flag");
        focusPackage(recents,BETA);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(Integer.valueOf(-1).equals(value(recents,"getPeekDir")),"Protected Beta accepted DOWN close");
        heldMenu(recents);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        require(!Integer.valueOf(1).equals(value(recents,"getCtxSelected")),"Protected context Close remains selectable");
        saveScreen("closing-protected-menu.png");tap(recents,KeyEvent.KEYCODE_BACK);
        closingPhase("PROTECTED_BETA");
        configureClose(recents,SetRow.INDIVIDUAL_PROTECTION,false);
        fixtureGuard(recents);focusPackage(recents,BETA);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
        idle(recents);closingPhase("INDIVIDUAL_PROTECTION_OFF");
        pass("Protected Beta blocks individual close; turning individual protection Off permits real closing without removing bulk protection");
        closingPhase("RESPAWN_BULK");awaitMode(recents,"OVERLAY",true);fixtureGuard(recents);
        configureClose(recents,SetRow.INDIVIDUAL_PROTECTION,true);
        killFocus(recents);tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.TRUE.equals(value(recents,"getKillPrompt")) && Boolean.FALSE.equals(value(recents,"getConfirmKill")),"Bulk review not cancel-first");
        saveScreen("closing-bulk-review.png");tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        closingPhase("CANCEL_BULK");
        killFocus(recents);tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);fixtureGuard(recents);
        tap(recents,KeyEvent.KEYCODE_DPAD_RIGHT);tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        idle(recents);closingPhase("BULK_CONFIRMED");
        pass("Confirmed Kill All closes only Alpha; protected Beta and hidden Gamma retain their real tasks and running services");
        closingPhase("RESPAWN_IMMEDIATE");awaitMode(recents,"OVERLAY",true);fixtureGuard(recents);
        configureClose(recents,SetRow.CONFIRM_KILL_ALL,false);
        killFocus(recents);tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
        require(Boolean.FALSE.equals(value(recents,"getKillPrompt")),"Confirmation Off opened a review");
        idle(recents);closingPhase("BULK_IMMEDIATE");
        killFocus(recents);tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);idle(recents);
        closingPhase("ALL_PROTECTED");
        pass("Confirmation Off performs immediate fixture-only Kill All; all-protected case leaves Beta and hidden Gamma untouched");
        saveScreen("closing-final-protected.png");
    }

    @Override public void onStart() {
        if(animationOnly) {
            Bundle result=new Bundle();
            try {
                Activity recents=open(RecentsActivity.class);
                SystemClock.sleep(2000);
                awaitMode(recents,"OVERLAY",false);
                saveScreen("animation-before.png");
                // History mode is mandatory here: this tests drawing/timing only,
                // and the completion callback cannot remove a real Android task.
                require(Boolean.FALSE.equals(value(recents,"getPrivileged")),"Animation-only test requires history mode");
                List<RecentTask> tasks=(List<RecentTask>)value(recents,"getTasks");
                require(!tasks.isEmpty(),"No preview card to animate");
                boolean motion=((RecentsSettings)value(recents,"getSettings")).getSmoothMotion();
                runOnMainSync(() -> {
                    try {
                        Method setter=recents.getClass().getDeclaredMethod("setDismissing",kotlin.Pair.class);
                        setter.setAccessible(true);setter.invoke(recents,new kotlin.Pair<Integer,Integer>(tasks.get(0).getTaskId(),1));
                    } catch(Exception e) { throw new RuntimeException(e); }
                });
                long started=SystemClock.uptimeMillis();
                SystemClock.sleep(motion?((RecentsSettings)value(recents,"getSettings")).getCloseAnimationSpeed().getDurationMs()-50:100);
                if(motion) require(value(recents,"getDismissing")!=null,"Animation ended before the visible drop completed");
                while(value(recents,"getDismissing")!=null && SystemClock.uptimeMillis()-started<5000) SystemClock.sleep(20);
                saveScreen("animation-after.png");
                require(value(recents,"getDismissing")==null,"Dismissal animation did not complete; scene="+value(recents,"getScene"));
                result.putString("stream","PASS: motion="+motion+" animation lifecycle; no real task was closed\n");
                result.putBoolean("passed",true);finish(Activity.RESULT_OK,result);
            } catch(Throwable e) { result.putString("stream",android.util.Log.getStackTraceString(e));result.putBoolean("passed",false);finish(Activity.RESULT_CANCELED,result); }
            return;
        }
        if(closingOnly) {
            Bundle result=new Bundle();
            try { testClosing();result.putBoolean("passed",true);finish(Activity.RESULT_OK,result); }
            catch(Throwable e) { result.putString("stream",android.util.Log.getStackTraceString(e));result.putBoolean("passed",false);finish(Activity.RESULT_CANCELED,result); }
            return;
        }
        if(stabilityOnly) {
            Bundle result=new Bundle();
            try { testStability();result.putBoolean("passed",true);finish(Activity.RESULT_OK,result); }
            catch(Throwable e) { result.putString("stream",android.util.Log.getStackTraceString(e));result.putBoolean("passed",false);finish(Activity.RESULT_CANCELED,result); }
            return;
        }
        if(presetsOnly) {
            Bundle result=new Bundle();
            try { testPresets(); result.putBoolean("passed",true);finish(Activity.RESULT_OK,result); }
            catch(Throwable e) { result.putString("stream",android.util.Log.getStackTraceString(e));result.putBoolean("passed",false);finish(Activity.RESULT_CANCELED,result); }
            return;
        }
        Bundle result=new Bundle();
        try {
            Activity recents=open(RecentsActivity.class);
            for(int i=0;i<100 && !"OVERLAY".equals(String.valueOf(value(recents,"getScene")));i++)
                SystemClock.sleep(100);
            require("OVERLAY".equals(String.valueOf(value(recents,"getScene"))),"Expected populated recent-app overlay");
            RecentsSettings migrated=(RecentsSettings)value(recents,"getSettings");
            require(migrated.getAppDetailsMode()==AppDetailsMode.ALWAYS && migrated.getKillAllLocation()==KillAllLocation.HIDDEN,
                "Legacy details/Kill All preferences did not migrate");
            pass("Legacy enabled details and hidden Kill All migrate without changing user intent");
            require(migrated.getCustomAccent()!=null && migrated.getCustomAccent()==0xFFB3FFFF,"Removed cyan preset did not retain its color as custom");
            Object settingsBefore=value(recents,"getSettings");
            long held=SystemClock.uptimeMillis();
            event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0,held);
            SystemClock.sleep(650);
            for(int i=1;i<=8;i++) {
                event(recents,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,i,held);
                SystemClock.sleep(60);
            }
            require(value(recents,"getCtxTask")!=null,"Held repeats dismissed the context menu");
            event(recents,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0,held);
            SystemClock.sleep(300);
            require(value(recents,"getCtxTask")!=null,"Releasing opening press dismissed context menu");
            require(settingsBefore.equals(value(recents,"getSettings")),"Opening menu changed settings");
            pass("Long OK hold, eight repeated downs, and release keep menu open");
            tap(recents,KeyEvent.KEYCODE_DPAD_DOWN);
            require(Integer.valueOf(3).equals(value(recents,"getCtxSelected")),"Fallback menu exposes unused task/protection actions");
            saveScreen("smoke-fallback-menu.png");
            tap(recents,KeyEvent.KEYCODE_BACK);
            require(value(recents,"getCtxTask")==null,"BACK did not close menu");
            require("OVERLAY".equals(String.valueOf(value(recents,"getScene"))),"BACK from menu closed overlay");
            pass("Context-menu BACK returns to overlay");

            topControls(recents);

            Activity settings=open(SettingsActivity.class);
            SystemClock.sleep(400);
            Object saved=value(settings,"getSettings");
            for(SetRow row:new SetRow[]{SetRow.POSITION,SetRow.DIRECTION,SetRow.SIZE,SetRow.SHAPE,
                SetRow.PADDING,SetRow.BG_DIM,SetRow.BG_TINT,
                SetRow.APP_DETAILS,SetRow.KILL_ALL,SetRow.CLOSE_HINT}) {
                go(settings,row);
                tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
                event(settings,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER,2,SystemClock.uptimeMillis());
                require(saved.equals(value(settings,"getSettings")),"OK changed adjustable row "+row);
            }
            pass("OK and held ENTER do not advance adjustable settings, including card padding");
            go(settings,SetRow.PADDING);
            tap(settings,KeyEvent.KEYCODE_DPAD_LEFT);
            tap(settings,KeyEvent.KEYCODE_DPAD_LEFT);
            tap(settings,KeyEvent.KEYCODE_DPAD_LEFT);
            SystemClock.sleep(200);
            runOnMainSync(() -> {
                for(int i=0;i<2;i++) {
                    long now=SystemClock.uptimeMillis();
                    settings.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT,0));
                    settings.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT,0));
                }
            });
            for(int i=0;i<30 && ((RecentsSettings)value(settings,"getSettings")).getCardPadding()!=CardPadding.NORMAL;i++)
                SystemClock.sleep(100);
            require(((RecentsSettings)value(settings,"getSettings")).getCardPadding()==CardPadding.NORMAL,
                "Rapid RIGHT inputs lost a padding adjustment");
            pass("Rapid consecutive adjustments preserve both steps");
            go(settings,SetRow.BG_DIM);
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            SystemClock.sleep(250);
            RecentsSettings modified=(RecentsSettings)value(settings,"getSettings");
            require(modified.getBgDim()!=((RecentsSettings)saved).getBgDim(),"RIGHT did not adjust dim");
            pass("RIGHT still adjusts the selected value");

            go(settings,SetRow.HEADER);
            tap(settings,KeyEvent.KEYCODE_DPAD_LEFT);
            go(settings,SetRow.SETTINGS_BUTTON);
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            SystemClock.sleep(200);
            modified=(RecentsSettings)value(settings,"getSettings");
            require(!modified.getShowHeader() && modified.getShowSettingsButton(),"Header and settings gear are not independent");
            tap(settings,KeyEvent.KEYCODE_DPAD_LEFT);
            SystemClock.sleep(200);
            modified=(RecentsSettings)value(settings,"getSettings");
            require(!modified.getShowSettingsButton(),"Settings gear switch did not hide the button");
            pass("Header and settings button are independently configurable");

            go(settings,SetRow.APP_NAMES);
            tap(settings,KeyEvent.KEYCODE_DPAD_LEFT);
            go(settings,SetRow.APP_DETAILS);
            steps(settings,KeyEvent.KEYCODE_DPAD_LEFT,2);
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            SystemClock.sleep(200);
            modified=(RecentsSettings)value(settings,"getSettings");
            require(!modified.getShowAppNames() && modified.getAppDetailsMode()==AppDetailsMode.ALWAYS,"App name and details are not independent");
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            SystemClock.sleep(200);
            modified=(RecentsSettings)value(settings,"getSettings");
            require(modified.getAppDetailsMode()==AppDetailsMode.PRIVILEGED_ONLY,"Privileged-only details option unavailable");
            require(!modified.getAppDetailsMode().visible(false) && modified.getAppDetailsMode().visible(true),"Privileged-only details use the wrong mode");
            steps(settings,KeyEvent.KEYCODE_DPAD_LEFT,2);
            SystemClock.sleep(200);
            modified=(RecentsSettings)value(settings,"getSettings");
            require(!modified.getShowAppNames() && modified.getAppDetailsMode()==AppDetailsMode.OFF,"Icon-only profile did not hide both text fields");
            pass("App names and details are independent; icon-only profile is available");

            go(settings,SetRow.KILL_ALL);
            steps(settings,KeyEvent.KEYCODE_DPAD_LEFT,2);
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            require(((RecentsSettings)value(settings,"getSettings")).getKillAllLocation()==KillAllLocation.TOP_CONTROLS,"Top-controls choice unavailable");
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            require(((RecentsSettings)value(settings,"getSettings")).getKillAllLocation()==KillAllLocation.HIDDEN,"Hidden choice unavailable");
            steps(settings,KeyEvent.KEYCODE_DPAD_LEFT,2);
            require(((RecentsSettings)value(settings,"getSettings")).getKillAllLocation()==KillAllLocation.APP_ROW,"App-row choice unavailable");
            modified=(RecentsSettings)value(settings,"getSettings");
            pass("Kill All location supports app row, top controls and hidden");

            go(settings,SetRow.ACCENT);
            Object beforeColor=value(settings,"getSettings");
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            require(Boolean.TRUE.equals(value(settings,"getColorPrompt")),"Custom color chart did not open");
            tap(settings,KeyEvent.KEYCODE_DPAD_DOWN);
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            require(beforeColor.equals(value(settings,"getSettings")),"Color preview persisted before Apply");
            tap(settings,KeyEvent.KEYCODE_BACK);
            require(beforeColor.equals(value(settings,"getSettings")),"Color cancel changed saved settings");
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            tap(settings,KeyEvent.KEYCODE_DPAD_DOWN);
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            SystemClock.sleep(200);
            require(((RecentsSettings)value(settings,"getSettings")).getCustomAccent()!=null,"Applied custom color was not saved");
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            steps(settings,KeyEvent.KEYCODE_DPAD_UP,6);
            steps(settings,KeyEvent.KEYCODE_DPAD_LEFT,12);
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            SystemClock.sleep(200);
            require(((RecentsSettings)value(settings,"getSettings")).getCustomAccent()==null,"Preset did not replace custom color");
            require(((RecentsSettings)value(settings,"getSettings")).getAccent()==AccentColor.GREEN && AccentColor.values().length==5,"Removed extra preset is still present");
            go(settings,SetRow.CONFIRM_KILL_ALL);
            tap(settings,KeyEvent.KEYCODE_DPAD_LEFT);
            require(!((RecentsSettings)value(settings,"getSettings")).getConfirmKillAll(),"Kill All confirmation cannot be disabled");
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            require(((RecentsSettings)value(settings,"getSettings")).getConfirmKillAll(),"Kill All confirmation cannot be enabled");
            modified=(RecentsSettings)value(settings,"getSettings");
            pass("Color chart previews without saving, BACK cancels, Apply saves, and presets remain available");

            go(settings,SetRow.RESET);
            long resetPress=SystemClock.uptimeMillis();
            event(settings,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0,resetPress);
            for(int i=1;i<=4;i++) event(settings,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,i,resetPress);
            event(settings,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0,resetPress);
            require(Boolean.TRUE.equals(value(settings,"getResetPrompt")),"Held reset press skipped confirmation");
            require(Boolean.FALSE.equals(value(settings,"getConfirmReset")),"Reset did not default to Cancel");
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            require(Boolean.FALSE.equals(value(settings,"getResetPrompt")),"Cancel did not close confirmation");
            require(modified.equals(value(settings,"getSettings")),"Cancel changed preferences");
            pass("Reset confirmation defaults to Cancel; repeats and cancel preserve settings");

            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            tap(settings,KeyEvent.KEYCODE_DPAD_RIGHT);
            tap(settings,KeyEvent.KEYCODE_DPAD_CENTER);
            for(int i=0;i<30 && !new RecentsSettings().equals(value(settings,"getSettings"));i++)
                SystemClock.sleep(100);
            require(new RecentsSettings().equals(value(settings,"getSettings")),"Reset profile differs from declared defaults");
            require(((RecentsSettings)value(settings,"getSettings")).getAppDetailsMode()==AppDetailsMode.PRIVILEGED_ONLY,"Default app details is not privileged-only");
            require(Integer.valueOf(0).equals(value(settings,"getSetFocus")),"Reset did not return focus to first setting");
            pass("Confirmed reset restores the complete default profile");
            tap(settings,KeyEvent.KEYCODE_BACK);
            awaitOverlay(recents);
            Object activeScene=value(recents,"getScene");
            Method setScene=recents.getClass().getDeclaredMethod("setScene",activeScene.getClass());
            setScene.setAccessible(true);
            Object emptyScene=java.lang.Enum.valueOf((Class)activeScene.getClass(),"EMPTY");
            runOnMainSync(()->{try{setScene.invoke(recents,emptyScene);}catch(Exception e){throw new RuntimeException(e);}});
            SystemClock.sleep(250);
            try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(getTargetContext().getFilesDir(),"smoke-empty.png"))) {
                getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);
            }
            ActivityMonitor emptyMonitor=addMonitor(SettingsActivity.class.getName(),null,false);
            tap(recents,KeyEvent.KEYCODE_DPAD_UP);
            Activity emptySettings=waitForMonitorWithTimeout(emptyMonitor,3000);
            removeMonitor(emptyMonitor);
            require(emptySettings!=null,"UP from empty state did not open Settings");
            tap(emptySettings,KeyEvent.KEYCODE_BACK);
            awaitOverlay(recents);
            runOnMainSync(()->{try{setScene.invoke(recents,emptyScene);}catch(Exception e){throw new RuntimeException(e);}});
            tap(recents,KeyEvent.KEYCODE_DPAD_CENTER);
            require(recents.isFinishing(),"OK from empty state did not exit");
            pass("Injected empty UI state: UP opens Settings and OK exits, without closing real apps");
            result.putString("stream","Input and reset smoke tests passed."+System.lineSeparator());
            result.putBoolean("passed",true);
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            result.putString("stream",android.util.Log.getStackTraceString(e));
            result.putBoolean("passed",false);
            finish(Activity.RESULT_CANCELED,result);
        }
    }
}



