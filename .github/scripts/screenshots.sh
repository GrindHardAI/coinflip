#!/usr/bin/env bash
# Runs inside the emulator job: installs the app, flips the coin, saves screenshots.
set -u
OUT=screenshots
mkdir -p "$OUT"
PKG=com.coinflipper.app
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "saved $1"; }

# Tap the Flip button (found on screen); falls back to the screen centre.
tap_flip() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local b
  b=$(adb exec-out cat /sdcard/ui.xml | grep -o 'text="Flip"[^>]*bounds="[^"]*"' | grep -o 'bounds="[^"]*"' | head -1)
  if [ -n "$b" ]; then
    read -r x1 y1 x2 y2 <<<"$(echo "$b" | grep -oE '[0-9]+' | tr '\n' ' ')"
    adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
  else
    adb shell input tap 540 960
  fi
}

start_app() {
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/.MainActivity >/dev/null
  sleep 4
}

adb shell cmd uimode night no
start_app
shot 1-start
for i in 1 2 3 4 5 6 7; do tap_flip; sleep 2.5; done
tap_flip; sleep 0.6; shot 2-mid-flip
sleep 2.5
shot 3-result-with-stats

adb shell cmd uimode night yes
start_app
shot 4-dark-mode
adb shell cmd uimode night no
ls -la "$OUT"
