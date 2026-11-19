package com.overcookedrecomp.app.runtime
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class GameValidationPipelineTest{
 @Test fun detectsValidMonoLayoutAndHashesAssembly(){val root=File(System.getProperty("java.io.tmpdir"),"validator-test-"+System.nanoTime());val data=File(root,"Game_Data");data.mkdirs();File(root,"Game.exe").writeText("exe");File(data,"globalgamemanagers").writeText("unity");File(data,"Managed").mkdirs();File(data,"Managed/Assembly-CSharp.dll").writeText("managed");val g=GameInstallation("g","Game",root.path,File(root,"Game.exe").path,data.path,"x86_64");val report=GameValidationPipeline().validate(g);assertTrue(report.ready);assertEquals(64,report.fingerprint.length);root.deleteRecursively()}
 @Test fun rejectsExecutableOutsideInstallRoot(){val root=File(System.getProperty("java.io.tmpdir"),"validator-root-"+System.nanoTime());val outside=File(root.parentFile,"external-"+System.nanoTime()+".exe");root.mkdirs();outside.writeText("x");val report=GameValidationPipeline().validate(GameInstallation("g","G",root.path,outside.path,File(root,"Data").path,"x86_64"));assertEquals(ValidationCode.EXE_OUTSIDE_ROOT,report.code);outside.delete();root.deleteRecursively()}
}