import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class RuntimeManifestTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads((ROOT / "runtime/manifest.json").read_text())
        cls.gradle = (ROOT / "android/app/build.gradle").read_text()
        cls.package_list = (ROOT / "runtime/rootfs/packages-amd64.txt").read_text().lower()

    def test_app_and_runtime_manifest_versions_match(self):
        match = re.search(r'versionName\s+"([^"]+)"', self.gradle)
        self.assertIsNotNone(match, "Android versionName is missing")
        self.assertEqual(self.manifest["release"], match.group(1))
        self.assertEqual(self.manifest["runtime_pack"]["version"], match.group(1))

    def test_runtime_pack_manifest_schema(self):
        pack = self.manifest["runtime_pack"]
        for key in ("status", "version", "url", "size_bytes", "sha256", "expanded_size_bytes", "minimum_app_version"):
            self.assertIn(key, pack)
        self.assertIn(pack["status"], ("pending", "published"))
        if pack["status"] == "published":
            self.assertTrue(pack["url"].startswith("https://"))
            self.assertGreater(pack["size_bytes"], 0)
            self.assertGreater(pack["expanded_size_bytes"], 0)
            self.assertRegex(pack["sha256"], r"^[0-9a-f]{64}$")
        else:
            self.assertEqual(pack["url"], "")
            self.assertEqual(pack["size_bytes"], 0)
            self.assertEqual(pack["sha256"], "")

    def test_dxvk_is_not_advertised_or_installed_as_a_runtime_component(self):
        self.assertIn("NOT_BUNDLED", self.manifest["components"]["dxvk"]["status"])
        self.assertNotIn("dxvk", self.package_list)

    def test_runtime_pack_and_native_proot_sources_are_pinned(self):
        self.assertEqual(self.manifest["components"]["proot"]["source_commit"], "6f8ebfd8e24887dfba64c3f2d7d5fe9dc059b60e")
        self.assertEqual(self.manifest["components"]["box64"]["source_commit"], "7fab05334445948c918281b54f2a604ff63337f7")
        self.assertIn("libgl1-mesa-dri:amd64", self.package_list)
        self.assertIn("libvulkan1:amd64", self.package_list)
        self.assertIn("RUNTIME_MANIFEST_URL", self.gradle)


if __name__ == "__main__":
    unittest.main()
