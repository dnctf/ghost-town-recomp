package com.overcookedrecomp.app.runtime
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
class GameValidationPipelineTest{
 private fun writePe(file:File){RandomAccessFile(file,"rw").use{r->r.setLength(320);r.seek(0);r.writeByte(0x4d);r.writeByte(0x5a);r.seek(0x3c);r.writeInt(0x40000000);r.seek(0x40);r.writeByte(0x50);r.writeByte(0x45);r.writeByte(0);r.writeByte(0);r.writeByte(0x64);r.writeByte(0x86);r.writeByte(1);r.writeByte(0);r.seek(0x54);r.writeByte(0xf0);r.writeByte(0);r.seek(0x58);r.writeByte(0x0b);r.writeByte(0x02)}}
 @Test fun validatesPe64MonoLayoutAndHashesAssembly(){val root=File(System.getProperty("java.io.tmpdir"),"validator-test-"+System.nanoTime());val data=File(root,"Game_Data");data.mkdirs();writePe(File(root,"Game.exe"));File(data,"globalgamemanagers").writeText("unity");File(data,"Managed").mkdirs();File(data,"Managed/Assembly-CSharp.dll").writeText("managed");val g=GameInstallation("g","Game",root.path,File(root,"Game.exe").path,data.path,"x86_64");val report=GameValidationPipeline().validate(g);assertTrue(report.message,report.ready);assertEquals(64,report.fingerprint.length);root.deleteRecursively()}
 @Test fun rejectsExecutableOutsideInstallRoot(){val root=File(System.getProperty("java.io.tmpdir"),"validator-root-"+System.nanoTime());val outside=File(root.parentFile,"external-"+System.nanoTime()+".exe");root.mkdirs();writePe(outside);val report=GameValidationPipeline().validate(GameInstallation("g","G",root.path,outside.path,File(root,"Data").path,"x86_64"));assertEquals(ValidationCode.EXE_OUTSIDE_ROOT,report.code);outside.delete();root.deleteRecursively()}
 @Test fun rejectsMalformedPe(){val root=File(System.getProperty("java.io.tmpdir"),"validator-bad-"+System.nanoTime());val data=File(root,"Data");data.mkdirs();File(root,"bad.exe").writeText("not a PE");val report=GameValidationPipeline().validate(GameInstallation("bad","Bad",root.path,File(root,"bad.exe").path,data.path,"x86_64"));assertEquals(ValidationCode.EXE_INVALID,report.code);root.deleteRecursively()}
}
