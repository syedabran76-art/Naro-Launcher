#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path("pojav")
MANIFEST = ROOT / "app_pojavlauncher/src/main/AndroidManifest.xml"
STORAGE = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/TestStorageActivity.java"
LAUNCHER = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/LauncherActivity.java"
BRIDGE = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/NaroLaunchBridgeActivity.java"

REQUEST = "com.narostudio.narolauncher.EXTRA_LAUNCH_GAME"
INTERNAL = "net.kdt.pojavlaunch.EXTRA_NARO_LAUNCH_GAME"

for path in (MANIFEST, STORAGE, LAUNCHER):
    if not path.is_file():
        raise SystemExit(f"Missing expected Pojav source file: {path}")

# Add the exported entry point used by Naro.
bridge_source = f'''package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * External launch entry point for Naro Launcher.
 * Pojav remains responsible for storage initialization and game launching.
 */
public final class NaroLaunchBridgeActivity extends Activity {{
    @Override
    protected void onCreate(Bundle savedInstanceState) {{
        super.onCreate(savedInstanceState);

        if (!getIntent().getBooleanExtra("{REQUEST}", false)) {{
            Toast.makeText(this, "Invalid Naro launch request", Toast.LENGTH_LONG).show();
            finish();
            return;
        }}

        Intent intent = new Intent(this, TestStorageActivity.class);
        intent.putExtra("{INTERNAL}", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }}
}}
'''

if BRIDGE.exists():
    if BRIDGE.read_text() != bridge_source:
        raise SystemExit(f"{BRIDGE} already exists with different content; refusing to overwrite.")
else:
    BRIDGE.write_text(bridge_source)
    print("Created", BRIDGE)

# Register the bridge in Pojav's application manifest.
manifest_text = MANIFEST.read_text()
activity_tag = '''        <activity
            android:name="net.kdt.pojavlaunch.NaroLaunchBridgeActivity"
            android:exported="true" />'''
if "net.kdt.pojavlaunch.NaroLaunchBridgeActivity" not in manifest_text:
    marker = '''        <activity
            android:name="net.kdt.pojavlaunch.MissingStorageActivity"'''
    if marker not in manifest_text:
        raise SystemExit("Could not locate manifest activity insertion point.")
    manifest_text = manifest_text.replace(marker, activity_tag + "\n" + marker, 1)
    MANIFEST.write_text(manifest_text)
    print("Registered bridge in manifest")
else:
    print("Manifest bridge registration already exists")

# Carry the launch request through Pojav's normal storage startup.
storage_text = STORAGE.read_text()
forward_line = f'''        if (getIntent().getBooleanExtra("{INTERNAL}", false)) {{
            intent.putExtra("{INTERNAL}", true);
        }}
'''
if forward_line.strip() not in storage_text:
    marker = '''        Intent intent =  new Intent(this, LauncherActivity.class);
        startActivity(intent);'''
    if marker not in storage_text:
        raise SystemExit("Could not locate TestStorageActivity launcher handoff.")
    storage_text = storage_text.replace(
        marker,
        '''        Intent intent =  new Intent(this, LauncherActivity.class);
''' + forward_line + '''        startActivity(intent);''',
        1
    )
    STORAGE.write_text(storage_text)
    print("Added launch-request forwarding to storage startup")
else:
    print("Storage launch forwarding already exists")

# Ask Pojav to launch only after its existing listener has been registered.
launcher_text = LAUNCHER.read_text()
listener = '''        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);'''
trigger = f'''
        if (getIntent().getBooleanExtra("{INTERNAL}", false)) {{
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        }}
'''
if trigger.strip() not in launcher_text:
    if listener not in launcher_text:
        raise SystemExit("Could not locate Pojav's game-launch listener registration.")
    launcher_text = launcher_text.replace(listener, listener + trigger, 1)
    LAUNCHER.write_text(launcher_text)
    print("Added launch trigger after listener registration")
else:
    print("Launcher trigger already exists")

# Parse the manifest to catch malformed XML.
ET.parse(MANIFEST)
print("Pojav Naro bridge patch completed.")
