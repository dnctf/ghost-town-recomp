import tempfile, unittest
from pathlib import Path
from game_installation_probe import inspect, pe_arch

def write_pe(path: Path, machine=0x8664):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = bytearray(256)
    data[0:2] = b"MZ"
    data[0x3C:0x40] = (0x80).to_bytes(4, "little")
    data[0x80:0x84] = b"PE\0\0"
    data[0x84:0x86] = machine.to_bytes(2, "little")
    path.write_bytes(data)

class ProbeTests(unittest.TestCase):
    def test_data_only_is_not_complete(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            data = root / "Overcooked2_Data"
            (data / "Managed").mkdir(parents=True)
            (data / "globalgamemanagers").write_bytes(b"unity")
            (data / "Managed" / "Assembly-CSharp.dll").write_bytes(b"MZ")
            report = inspect(root)
            self.assertEqual(report["executable_count"], 0)
            self.assertIn("alone is not a runnable", report["summary"])

    def test_complete_mono_layout_is_only_installation_complete(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_pe(root / "Overcooked2.exe")
            (root / "UnityPlayer.dll").write_bytes(b"dll")
            data = root / "Overcooked2_Data"
            (data / "Managed").mkdir(parents=True)
            (data / "globalgamemanagers").write_bytes(b"unity")
            (data / "Managed" / "Assembly-CSharp.dll").write_bytes(b"MZ")
            item = inspect(root)["installations"][0]
            self.assertEqual(item["architecture"], "x86_64")
            self.assertEqual(item["backend_hint"], "MONO")
            self.assertEqual(item["status"], "COMPLETE_WINDOWS_INSTALLATION_RUNTIME_NOT_INCLUDED")

    def test_il2cpp_layout(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_pe(root / "MovingOut2.exe")
            (root / "UnityPlayer.dll").write_bytes(b"dll")
            data = root / "MovingOut2_Data"
            metadata = data / "il2cpp_data" / "Metadata"
            metadata.mkdir(parents=True)
            (data / "globalgamemanagers").write_bytes(b"unity")
            (metadata / "global-metadata.dat").write_bytes(b"metadata")
            item = inspect(root)["installations"][0]
            self.assertEqual(item["backend_hint"], "IL2CPP")

if __name__ == "__main__":
    unittest.main()
