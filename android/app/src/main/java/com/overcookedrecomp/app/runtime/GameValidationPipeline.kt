package com.overcookedrecomp.app.runtime
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
enum class ValidationCode{READY,ROOT_MISSING,EXE_MISSING,EXE_OUTSIDE_ROOT,EXE_INVALID,DATA_MISSING,UNITY_CORE_MISSING,UNSUPPORTED_ARCH,IO_ERROR}
data class ValidationReport(val installationId:String,val code:ValidationCode,val message:String,val fingerprint:String="",val checkedFiles:List<String> = emptyList()){val ready:Boolean get()=code==ValidationCode.READY}
/** File-only validation. Parses PE headers, never executes the game. */
class GameValidationPipeline{
 fun validate(g:GameInstallation):ValidationReport{
  val root=File(g.installRoot);if(!root.isDirectory)return report(g,ValidationCode.ROOT_MISSING,"Folder instalasi tidak ditemukan.")
  val exe=File(g.executablePath);val data=File(g.dataDirectory)
  val rootPath=runCatching{root.canonicalFile.toPath()}.getOrElse{return report(g,ValidationCode.IO_ERROR,"Path instalasi tidak dapat dibaca.")}
  val exePath=runCatching{exe.canonicalFile.toPath()}.getOrElse{return report(g,ValidationCode.IO_ERROR,"Path executable tidak valid.")}
  if(!exePath.startsWith(rootPath))return report(g,ValidationCode.EXE_OUTSIDE_ROOT,"Executable berada di luar folder instalasi.")
  if(!exe.isFile||exe.length()==0L)return report(g,ValidationCode.EXE_MISSING,"Executable tidak ditemukan atau kosong.")
  val pe=runCatching{readPeArchitecture(exe)}.getOrElse{return report(g,ValidationCode.EXE_INVALID,"Header executable PE tidak valid atau terpotong.")}
  if(pe!="x86_64")return report(g,ValidationCode.UNSUPPORTED_ARCH,"PE architecture belum didukung: "+pe)
  if(g.architecture !in setOf("x86_64","amd64"))return report(g,ValidationCode.UNSUPPORTED_ARCH,"Metadata architecture instalasi tidak cocok: "+g.architecture)
  if(!data.isDirectory)return report(g,ValidationCode.DATA_MISSING,"Folder data game tidak ditemukan.")
  val core=listOf("globalgamemanagers","data.unity3d","level0").map{File(data,it)}.filter{it.isFile&&it.length()>0}
  if(core.isEmpty())return report(g,ValidationCode.UNITY_CORE_MISSING,"File inti Unity tidak ditemukan.")
  val managed=File(data,"Managed/Assembly-CSharp.dll");val il2cpp=File(data,"il2cpp_data/Metadata/global-metadata.dat")
  if(!managed.isFile&&!il2cpp.isFile)return report(g,ValidationCode.UNITY_CORE_MISSING,"Assembly-CSharp.dll atau global-metadata.dat tidak ditemukan.")
  val target=if(managed.isFile)managed else il2cpp
  val hash=runCatching{sha(target)}.getOrElse{return report(g,ValidationCode.IO_ERROR,"Fingerprint file gagal dihitung.")}
  return ValidationReport(g.id,ValidationCode.READY,"Struktur instalasi dan PE64 AMD64 terdeteksi; game belum dijalankan.",hash,core.map{it.name}+target.name)
 }
 private fun readPeArchitecture(file:File):String=RandomAccessFile(file,"r").use{r->if(r.length()<128||u16(r,0)!=0x5A4D)throw IllegalArgumentException();val offset=u32(r,0x3c);if(offset<64||offset+26>r.length())throw IllegalArgumentException();r.seek(offset);if(r.readUnsignedByte()!=0x50||r.readUnsignedByte()!=0x45||r.readUnsignedByte()!=0||r.readUnsignedByte()!=0)throw IllegalArgumentException();val machine=u16(r,offset+4);val opt=u16(r,offset+24);if(opt!=0x20b)throw IllegalArgumentException();when(machine){0x8664->"x86_64";0xaa64->"arm64";0x14c->"x86";else->"unknown"}}
 private fun u16(r:RandomAccessFile,p:Long):Int{r.seek(p);return r.readUnsignedByte() or (r.readUnsignedByte() shl 8)}
 private fun u32(r:RandomAccessFile,p:Long):Long{r.seek(p);return r.readUnsignedByte().toLong() or (r.readUnsignedByte().toLong() shl 8) or (r.readUnsignedByte().toLong() shl 16) or (r.readUnsignedByte().toLong() shl 24)}
 private fun report(g:GameInstallation,c:ValidationCode,m:String)=ValidationReport(g.id,c,m)
 private fun sha(f:File):String{val d=MessageDigest.getInstance("SHA-256");f.inputStream().buffered().use{s->val b=ByteArray(65536);while(true){val n=s.read(b);if(n<0)break;d.update(b,0,n)}};return d.digest().joinToString(""){"%02x".format(it)}}
}
