# DayLoop P0-05 Exact Alarm Spike Harness

This is a disposable Android SDK-only spike, not the production DayLoop app structure. It exists to prove exact-alarm capability checks, ordinary process-death delivery, fallback scheduling, and stale delivery guards before the production alarm engine is designed.

Build with:

```powershell
.\build.ps1
```

Install with:

```powershell
adb install -r .\build\exact-alarm-spike.apk
adb shell am start -n com.dayloop.spike.exactalarm/.MainActivity
```

The app logs to the Activity and to logcat tag `DayLoopAlarmSpike`.

P0-06 notification UI actions:

```powershell
adb shell pm grant com.dayloop.spike.exactalarm android.permission.POST_NOTIFICATIONS
adb shell am start -n com.dayloop.spike.exactalarm/.MainActivity -a dayloop.spike.CHECK_UI
adb shell am start -n com.dayloop.spike.exactalarm/.MainActivity -a dayloop.spike.POST_HIGH
adb shell am start -n com.dayloop.spike.exactalarm/.MainActivity -a dayloop.spike.POST_FULL_SCREEN
```

P0-07 reboot recovery actions:

```powershell
adb shell appops set com.dayloop.spike.exactalarm SCHEDULE_EXACT_ALARM allow
adb shell am broadcast -n com.dayloop.spike.exactalarm/.UiCommandReceiver -a dayloop.spike.SCHEDULE_EXACT --el delayMs 300000
adb reboot
adb wait-for-device
adb shell getprop sys.boot_completed
adb shell run-as com.dayloop.spike.exactalarm cat shared_prefs/alarm_spike.xml
```


## P0-08 interval one-shot command

Schedule a disposable interval chain without polling:

`	ext
adb shell am broadcast -n com.dayloop.spike.exactalarm/.UiCommandReceiver -a dayloop.spike.SCHEDULE_INTERVAL --el intervalMs 15000 --ei maxDeliveries 3 --ei windowStartMinute 0 --ei windowEndMinute 1440
` 

Expected evidence is INTERVAL_START, one DELIVERED_VALID per fired occurrence, INTERVAL_NEXT after each non-final valid delivery, and INTERVAL_CHAIN_STOP after the configured maximum delivery count.
