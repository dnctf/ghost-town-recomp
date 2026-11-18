import json, os, subprocess, tempfile, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class ReportTests(unittest.TestCase):
 def run_report(self, artifacts, jobs, workflow="Runtime Example", api_status="failed", run=None, api_jobs=None):
  with tempfile.TemporaryDirectory() as t:
   root=Path(t); art=root/"artifacts"; art.mkdir()
   for n,c in artifacts.items():
    p=art/n; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(c)
   env=os.environ.copy()
   env.update({"REPORT_WORKFLOW":workflow,"GITHUB_RUN_NUMBER":"4","GITHUB_RUN_ID":"44","GITHUB_REPOSITORY":"dnctf/ghost-town-recomp","GITHUB_REF_NAME":"main","GITHUB_SHA":"abc","GITHUB_EVENT_NAME":"push","REPORT_JOB_RESULTS":jobs,"REPORT_ARTIFACT_DIR":str(art),"REPORT_API_STATUS":api_status})
   if run is not None:
    p=root/"run.json"; p.write_text(json.dumps(run)); env["REPORT_RUN_JSON_FILE"]=str(p)
   if api_jobs is not None:
    p=root/"jobs.json"; p.write_text(json.dumps(api_jobs)); env["REPORT_JOBS_JSON_FILE"]=str(p)
   result=subprocess.run(["bash",str(ROOT/"tools/build-report.sh"),str(root/"reports")],env=env,capture_output=True,text=True)
   if result.returncode: raise AssertionError(result.stderr+"\n"+result.stdout)
   return next((root/"reports").glob("*.md")).read_text()
 def test_api_failure_marks_only_api_fields_unavailable(self):
  x=self.run_report({},'{"compile":{"result":"failure"}}',api_status="failed")
  self.assertIn("**Waktu:** TIDAK TERSEDIA",x)
  self.assertIn("durasi total TIDAK TERSEDIA",x)
  self.assertIn("API GitHub: gagal",x)
  self.assertIn("**Hasil keseluruhan:** GAGAL",x)
 def test_apk_workflow_smoke_is_not_applicable(self):
  run={"status":"completed","conclusion":"success","created_at":"2026-11-18T00:00:00Z","run_started_at":"2026-11-18T00:00:01Z","updated_at":"2026-11-18T00:01:01Z","head_branch":"main","head_sha":"abc","event":"push"}
  jobs=[{"name":"Build Android ARM64 APK","status":"completed","conclusion":"success","started_at":"2026-11-18T00:00:10Z","completed_at":"2026-11-18T00:01:00Z","runner_name":"runner","labels":["ubuntu-latest"]}]
  x=self.run_report({},'{"android-build":{"result":"success"}}',workflow="Ghost Town Android APK",api_status="ok",run=run,api_jobs=jobs)
  self.assertIn("N/A (tidak berlaku untuk workflow ini)",x)\n  self.assertIn("Runtime archive: N/A (tidak berlaku)",x)\n  self.assertIn("Rootfs sebelum/sesudah trim: N/A (tidak berlaku)",x)
  self.assertNotIn("| a | box64 --version | TIDAK TERSEDIA | TIDAK TERSEDIA | NOT RUN |",x)
  self.assertIn("durasi total 1m 0s (60s)",x)
  self.assertIn("50s (50s)",x)
 def test_runtime_failure_midway_keeps_partial_ladder(self):
  logs={
   "a-box64-version.log":"exit_code=0\nduration_seconds=1\nBox64 arm64 v0.4.5\n",
   "b-x86_64-utility.log":"exit_code=0\nduration_seconds=1\nWine 11.19\n",
   "c-wine-version.log":"wine-11.19\nexit_code=0\nduration_seconds=1\n",
   "d-wineboot.log":"[RUN] rung=d-wineboot timeout_seconds=600\nSIGSEGV\nexit_code=124\nduration_seconds=600\n"
  }
  x=self.run_report(logs,'{"rootfs":{"result":"failure"}}',workflow="Build Complete Wine Runtime",api_status="failed")
  self.assertIn("| d | box64 wineboot | 124 | 600 | TIMEOUT |",x)
  self.assertIn("| e | box64 wine cmd /c ver | TIDAK TERSEDIA | TIDAK TERSEDIA | NOT RUN |",x)
  self.assertIn("| a | box64 --version | 0 | 1 | PASS |",x)
 def test_api_timestamps_and_job_durations_are_used(self):
  run={"status":"completed","conclusion":"success","created_at":"2026-11-18T00:00:00Z","run_started_at":"2026-11-18T00:00:00Z","updated_at":"2026-11-18T00:00:20Z","head_branch":"main","head_sha":"abc","event":"push"}
  jobs=[{"name":"compile","status":"completed","conclusion":"success","started_at":"2026-11-18T00:00:02Z","completed_at":"2026-11-18T00:00:12Z","runner_name":"ubuntu","labels":["ubuntu-latest"]}]
  x=self.run_report({},'{"compile":{"result":"success"}}',api_status="ok",run=run,api_jobs=jobs)
  self.assertIn("durasi total 20s (20s)",x)
  self.assertIn("10s (10s)",x)
 def test_success_log(self):
  x=self.run_report({"a-box64-version.log":"exit_code=0\nduration_seconds=1\nBox64 arm64 v0.4.5\n"},'{"smoke":{"result":"success"}}')
  self.assertIn("| a | box64 --version | 0 | 1 | PASS |",x)
if __name__=="__main__": unittest.main()
