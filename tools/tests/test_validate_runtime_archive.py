import importlib.util
import io
import tarfile
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "validate_runtime_archive.py"
SPEC = importlib.util.spec_from_file_location("validate_runtime_archive", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class RuntimeArchiveValidationTests(unittest.TestCase):
    def make_archive(self, members):
        handle = tempfile.NamedTemporaryFile(suffix=".tar.gz", delete=False)
        handle.close()
        with tarfile.open(handle.name, "w:gz") as archive:
            for name, kind, link in members:
                info = tarfile.TarInfo(name)
                if kind == "dir":
                    info.type = tarfile.DIRTYPE
                    archive.addfile(info)
                elif kind == "sym":
                    info.type = tarfile.SYMTYPE
                    info.linkname = link
                    archive.addfile(info)
                elif kind == "hard":
                    info.type = tarfile.LNKTYPE
                    info.linkname = link
                    archive.addfile(info)
                else:
                    info.size = 1
                    archive.addfile(info, io.BytesIO(b"x"))
        self.addCleanup(Path(handle.name).unlink, missing_ok=True)
        return handle.name

    def base_members(self):
        return [(name, "file", "") for name in sorted(MODULE.REQUIRED)]

    def test_accepts_guest_absolute_symlink(self):
        members = self.base_members() + [
            ("rootfs/usr/bin/perl5.36.0", "file", ""),
            ("rootfs/usr/bin/perl", "sym", "/usr/bin/perl5.36.0"),
        ]
        MODULE.validate(self.make_archive(members))

    def test_rejects_symlink_escape(self):
        members = self.base_members() + [
            ("rootfs/usr/bin/escape", "sym", "../../../../../../../../data/local/tmp/evil"),
        ]
        with self.assertRaisesRegex(ValueError, "escapes guest rootfs"):
            MODULE.validate(self.make_archive(members))

    def test_rejects_absolute_link_outside_rootfs(self):
        members = self.base_members() + [("bin/proot-link", "sym", "/system/bin/sh")]
        with self.assertRaisesRegex(ValueError, "outside rootfs"):
            MODULE.validate(self.make_archive(members))

    def test_rejects_hardlink_to_missing_target(self):
        members = self.base_members() + [("rootfs/usr/bin/perl", "hard", "rootfs/usr/bin/missing")]
        with self.assertRaisesRegex(ValueError, "hardlink target"):
            MODULE.validate(self.make_archive(members))

    def test_rejects_hardlink_to_symlink(self):
        members = self.base_members() + [
            ("rootfs/usr/bin/perl-target", "sym", "/usr/bin/perl-real"),
            ("rootfs/usr/bin/perl", "hard", "rootfs/usr/bin/perl-target"),
        ]
        with self.assertRaisesRegex(ValueError, "preceding regular file"):
            MODULE.validate(self.make_archive(members))


if __name__ == "__main__":
    unittest.main()
