#!/usr/bin/env bash
# Runs inside the emulator job: installs the app, flips the coin, saves screenshots.
set -u
OUT=screenshots
mkdir -p "$OUT"
PKG=com.coinflipper.app
adb install -r app/build/outputs/apk/debug/app-debug.apk

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "saved $1"; }

# Tap the first on-screen element whose text or description matches (case-insensitive).
tap_ui() {
  local what="$1" fx="${2:-540}" fy="${3:-960}"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local b
  b=$(adb exec-out cat /sdcard/ui.xml | tr '>' '\n' \
      | grep -iE "(text|content-desc)=\"$what\"" | grep -o 'bounds="[^"]*"' | head -1)
  if [ -n "$b" ]; then
    read -r x1 y1 x2 y2 <<<"$(echo "$b" | grep -oE '[0-9]+' | tr '\n' ' ')"
    adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
  else
    echo "could not find '$what', tapping fallback"
    adb shell input tap "$fx" "$fy"
  fi
}

adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity --ez screenshot_mode true >/dev/null
sleep 4
shot 1-start

for i in 1 2 3 4 5 6 7; do tap_ui "flip"; sleep 2.5; done
tap_ui "flip"; sleep 0.6; shot 2-mid-flip
sleep 2.5
shot 3-result-with-stats

tap_ui "settings" 1000 140; sleep 4
shot 4-settings
tap_ui "silver"; sleep 1
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_ui "flip"; sleep 3
shot 5-silver-coin

# Extra check (not for the store): start normally so Google TEST ads load.
mkdir -p "$OUT/previews"
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity >/dev/null
sleep 20
adb exec-out screencap -p > "$OUT/previews/ads-banner.png"
tap_ui "settings" 1000 140; sleep 4
adb exec-out screencap -p > "$OUT/previews/ads-settings.png"
adb shell input keyevent KEYCODE_BACK
ls -la "$OUT" "$OUT/previews"
