#!/usr/bin/env python3
"""Validate the Java/Kotlin ABI referenced by the protected launcher's native VM.

This reads existing R8 outputs; it does not compile or execute Android code.
"""

import argparse
import os
from pathlib import Path
import re
import sys
import zipfile


MODEL_FIELDS = {
    "com.daily.health.manager.config.models.PushConfig": {"paidChannel", "organicChannel", "pushMessages"},
    "com.daily.health.manager.data.enums.BloodSugarRanges": {
        "low", "lowHigh", "normalLow", "normalHigh", "prediabetesLow", "prediabetesHigh", "diabetesLow",
    },
    "com.healthtracker.earthquake.model.EarthquakeProperties": {"mag", "place", "time"},
}
REQUIRED_CLASSES = (
    "kotlin.Unit",
    "kotlin.jvm.internal.Intrinsics",
    "kotlin.jvm.internal.Lambda",
    "kotlin.jvm.functions.Function0",
    "kotlin.jvm.functions.Function6",
    "kotlin.coroutines.Continuation",
    "kotlin.collections.CollectionsKt",
    "kotlin.text.StringsKt",
    "kotlinx.coroutines.CoroutineScope",
    "kotlinx.coroutines.Dispatchers",
    "kotlinx.coroutines.BuildersKt",
    "androidx.activity.OnBackPressedDispatcher",
    "androidx.activity.OnBackPressedCallback",
    "androidx.lifecycle.LifecycleOwner",
    "androidx.compose.runtime.Composer",
    "androidx.compose.ui.platform.ComposeView",
) + tuple(MODEL_FIELDS)
INTRINSICS_METHODS = {
    "checkNotNull", "checkNotNullParameter", "checkNotNullExpressionValue",
    "areEqual", "throwUninitializedPropertyAccessException",
}
NATIVE_ABI_PREFIXES = (
    "kotlin.", "kotlinx.", "androidx.", "com.jiagu.sdk.",
    "com.health.tracker.launcher.tool.",
    "com.leafmotivation.quizguessoncolor.",
)
CLASS_MAPPING = re.compile(r"^(\S+) -> (\S+):$")


def audit_rule_files(root: Path) -> list[str]:
    errors = []
    for directory, children, files in os.walk(root):
        children[:] = [name for name in children if name not in {".git", ".gradle", "build", "node_modules"}]
        for name in files:
            if not name.endswith(".pro"):
                continue
            path = Path(directory) / name
            for number, raw in enumerate(path.read_text().splitlines(), 1):
                rule = raw.split("#", 1)[0].strip()
                tokens = rule.split()
                if not tokens:
                    continue
                forbidden = tokens[0] in {"-ignorewarnings", "-dontshrink", "-dontoptimize", "-dontobfuscate"}
                if tokens[0] == "-dontwarn":
                    forbidden |= len(tokens) == 1 or tokens[1] in {"*", "**", "**.*"}
                if tokens[0] == "-keepattributes":
                    forbidden |= "!SourceFile" in rule or "!LineNumberTable" in rule
                if forbidden:
                    errors.append(f"{path.relative_to(root)}:{number}: unsafe global rule: {rule}")
    return errors


def verify_archive(archive: Path, mapping: Path) -> list[str]:
    errors = []
    required_mappings = {}
    methods = set()
    callback_found = False
    current_class = None
    with mapping.open() as source:
        for raw in source:
            match = CLASS_MAPPING.match(raw.strip()) if not raw.startswith(" ") else None
            if match:
                original, renamed = match.groups()
                current_class = original
                if original in REQUIRED_CLASSES:
                    required_mappings[original] = renamed
                # Desugaring's generated helpers are not a stable native ABI.
                if (original in REQUIRED_CLASSES or original.startswith(NATIVE_ABI_PREFIXES)) and "$$ExternalSynthetic" not in original:
                    if original != renamed:
                        errors.append(f"Native ABI class renamed/removed: {original} -> {renamed}")
                continue
            if " -> " not in raw or raw.lstrip().startswith("#"):
                continue
            signature, renamed = raw.strip().rsplit(" -> ", 1)
            if "(" not in signature:
                field = signature.split()[-1]
                if current_class in MODEL_FIELDS and field in MODEL_FIELDS[current_class]:
                    # R8 may omit identity field mappings; any explicit rename breaks this JSON contract.
                    if field != renamed:
                        errors.append(f"Reflection model field renamed: {current_class}.{field} -> {renamed}")
                continue
            method = signature.split("(", 1)[0].split()[-1].rsplit(".", 1)[-1]
            if current_class == "kotlin.jvm.internal.Intrinsics" and method in INTRINSICS_METHODS:
                methods.add(method)
                if renamed != method:
                    errors.append(f"Native ABI method renamed: Intrinsics.{method} -> {renamed}")
            if current_class == "androidx.activity.OnBackPressedDispatcher" and method == "addCallback":
                parameters = signature.split("(", 1)[1].split(")", 1)[0].replace(" ", "")
                if parameters == "androidx.lifecycle.LifecycleOwner,androidx.activity.OnBackPressedCallback":
                    callback_found = True
                    if renamed != "addCallback":
                        errors.append(f"Native ABI method renamed: OnBackPressedDispatcher.addCallback -> {renamed}")

    for name in REQUIRED_CLASSES:
        if name not in required_mappings:
            errors.append(f"Required runtime class missing from mapping: {name}")
    for method in sorted(INTRINSICS_METHODS - methods):
        errors.append(f"Required runtime method missing: Intrinsics.{method}")
    if not callback_found:
        errors.append("Required runtime signature missing: OnBackPressedDispatcher.addCallback(LifecycleOwner,OnBackPressedCallback)")

    # Check packaged DEX as well as mapping; a keep rule on an absent dependency is not sufficient.
    missing = {name: ("L" + name.replace(".", "/") + ";").encode() for name in REQUIRED_CLASSES}
    with zipfile.ZipFile(archive) as package:
        for name in package.namelist():
            if name.endswith(".dex"):
                dex = package.read(name)
                missing = {symbol: descriptor for symbol, descriptor in missing.items() if descriptor not in dex}
    errors.extend(f"Required runtime descriptor absent from packaged DEX: {name}" for name in missing)
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--rules-root", type=Path)
    parser.add_argument("--archive", type=Path, help="Existing minified APK or AAB")
    parser.add_argument("--mapping", type=Path)
    parser.add_argument("--configuration", type=Path, help="Merged R8 configuration for upstream diagnostics")
    args = parser.parse_args()
    if bool(args.archive) != bool(args.mapping):
        parser.error("--archive and --mapping must be supplied together")
    if not args.rules_root and not args.archive:
        parser.error("supply --rules-root or an archive/mapping pair")
    errors = audit_rule_files(args.rules_root) if args.rules_root else []
    if args.archive:
        errors.extend(verify_archive(args.archive, args.mapping))
    if args.configuration:
        upstream_suppressions = sum(
            line.strip() == "-ignorewarnings" for line in args.configuration.read_text().splitlines()
        )
        if upstream_suppressions:
            print(f"WARNING: merged R8 configuration contains {upstream_suppressions} upstream -ignorewarnings rules; "
                  "packaged ABI checks remain mandatory. Ask the SDK vendor to remove global suppression.")
    if errors:
        for error in errors[:30]:
            print("ERROR: " + error, file=sys.stderr)
        if len(errors) > 30:
            print(f"... {len(errors) - 30} additional ABI violations", file=sys.stderr)
        return 1
    print("Release runtime ABI and R8 rule checks passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
