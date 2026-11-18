import os,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class ReportTests(unittest.TestCase):
 def run_report(self,artifacts,jobs):
  with tempfile.TemporaryDirectory() as t:
   for n,c in artifacts.items():Path(t,n).write_text(c)
   env=os.environ.copy();env.update({"REPORT_WORKFLOW":"Example","GITHUB_RUN_NUMBER":"4","GITHUB_RUN_ID":"44","GITHUB_REPOSITORY":"dnctf/ghost-town-recomp","GITHUB_REF_NAME":"main","GITHUB_SHA":"abc","GITHUB_EVENT_NAME":"push","REPORT_JOB_RESULTS":jobs,"REPORT_ARTIFACT_DIR":t})
   subprocess.run(["bash",str(ROOT/"tools/build-report.sh"),t],env=env,check=True,capture_output=True,text=True)
   return next(Path(t).glob("*.md")).read_text()
 def test_failure_missing_data(self):
  x=self.run_report({},'{"compile":{"result":"failure","outputs":{}}}')
  for h in ["# LAPORAN BUILD","## 1. Ringkasan tahap","## 2. Smoke ladder","## 3. Kegagalan","## 4. Versi komponen","## 5. Ukuran dan artefak","## 6. Performa build","## 7. Perubahan sejak run sebelumnya","## 8. Tingkat bukti dan gerbang","## 9. Observasi dan rekomendasi","## 10. Tautan"]:self.assertIn("> "+h,x)
  self.assertIn("TIDAK TERSEDIA",x);self.assertIn("> **Hasil keseluruhan:** GAGAL",x)
 def test_success_log(self):
  x=self.run_report({"a-box64-version.log":"exit_code=0\nduration_seconds=1\nBox64 arm64 v0.4.5\n"},'{"smoke":{"result":"success","outputs":{}}}')
  self.assertIn("| a | box64 --version | 0 | 1 | PASS |",x)
if __name__=="__main__":unittest.main()
