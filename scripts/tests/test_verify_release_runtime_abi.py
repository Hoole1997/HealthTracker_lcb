import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile


SPEC = importlib.util.spec_from_file_location("release_abi", Path(__file__).parents[1] / "verify_release_runtime_abi.py")
ABI = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ABI)


class RuntimeAbiTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.mapping = self.root / "mapping.txt"
        self.archive = self.root / "app.aab"
        lines = []
        for name in ABI.REQUIRED_CLASSES:
            lines.append(f"{name} -> {name}:")
            if name == "kotlin.jvm.internal.Intrinsics":
                lines.extend(f"    1:2:void {method}(java.lang.Object):1:2 -> {method}" for method in ABI.INTRINSICS_METHODS)
            if name == "androidx.activity.OnBackPressedDispatcher":
                lines.append("    void addCallback(androidx.lifecycle.LifecycleOwner,androidx.activity.OnBackPressedCallback) -> addCallback")
            if name in ABI.MODEL_FIELDS:
                lines.extend(f"    java.lang.Object {field} -> {field}" for field in ABI.MODEL_FIELDS[name])
        lines.append("com.daily.health.manager.SomeBusinessClass -> a:")
        self.mapping.write_text("\n".join(lines) + "\n")
        self.write_archive(ABI.REQUIRED_CLASSES)

    def write_archive(self, classes):
        data = b"dex\n035\x00" + b"\x00".join(("L" + name.replace(".", "/") + ";").encode() for name in classes)
        with zipfile.ZipFile(self.archive, "w") as archive:
            archive.writestr("base/dex/classes.dex", data)

    def test_kept_runtime_still_allows_business_code_obfuscation(self):
        self.assertEqual([], ABI.verify_archive(self.archive, self.mapping))

    def test_detects_the_reproduced_intrinsics_rename(self):
        self.mapping.write_text(self.mapping.read_text().replace("kotlin.jvm.internal.Intrinsics -> kotlin.jvm.internal.Intrinsics:", "kotlin.jvm.internal.Intrinsics -> c10:"))
        self.assertTrue(any("Intrinsics -> c10" in error for error in ABI.verify_archive(self.archive, self.mapping)))

    def test_detects_native_method_rename(self):
        self.mapping.write_text(self.mapping.read_text().replace(" -> checkNotNullParameter", " -> a"))
        self.assertTrue(any("method renamed" in error for error in ABI.verify_archive(self.archive, self.mapping)))

    def test_mapping_alone_does_not_pass_when_dex_is_missing_runtime(self):
        self.write_archive([name for name in ABI.REQUIRED_CLASSES if name != "kotlin.jvm.internal.Intrinsics"])
        self.assertTrue(any("absent from packaged DEX" in error for error in ABI.verify_archive(self.archive, self.mapping)))

    def test_detects_removed_runtime_class(self):
        self.mapping.write_text(self.mapping.read_text().replace("kotlinx.coroutines.Dispatchers -> kotlinx.coroutines.Dispatchers:\n", ""))
        self.assertTrue(any("class missing" in error for error in ABI.verify_archive(self.archive, self.mapping)))

    def test_detects_the_reproduced_jetpack_callback_rename(self):
        self.mapping.write_text(self.mapping.read_text().replace(" -> addCallback", " -> a"))
        self.assertTrue(any("OnBackPressedDispatcher.addCallback -> a" in error for error in ABI.verify_archive(self.archive, self.mapping)))

    def test_callback_overload_must_include_lifecycle_owner(self):
        self.mapping.write_text(self.mapping.read_text().replace("androidx.lifecycle.LifecycleOwner,androidx.activity.OnBackPressedCallback)", "androidx.activity.OnBackPressedCallback)"))
        self.assertTrue(any("signature missing" in error for error in ABI.verify_archive(self.archive, self.mapping)))

    def test_detects_unannotated_json_field_rename(self):
        self.mapping.write_text(self.mapping.read_text().replace(" low -> low", " low -> a"))
        self.assertTrue(any("model field renamed" in error for error in ABI.verify_archive(self.archive, self.mapping)))

    def test_r8_may_omit_identity_field_mappings(self):
        self.mapping.write_text("\n".join(
            line for line in self.mapping.read_text().splitlines() if "    java.lang.Object " not in line
        ) + "\n")
        self.assertEqual([], ABI.verify_archive(self.archive, self.mapping))

    def test_scoped_warnings_are_allowed_but_global_suppression_is_not(self):
        rules = self.root / "rules.pro"
        rules.write_text("-dontwarn org.conscrypt.**\n-keepattributes SourceFile,LineNumberTable\n")
        self.assertEqual([], ABI.audit_rule_files(self.root))
        rules.write_text("-dontwarn **\n-ignorewarnings\n-keepattributes !LineNumberTable\n")
        self.assertEqual(3, len(ABI.audit_rule_files(self.root)))


if __name__ == "__main__":
    unittest.main()
