package com.overcookedrecomp.app.runtime
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Properties
enum class InstallationStatus { UNKNOWN, SCANNING, INVALID, MISSING_FILES, UNSUPPORTED, READY, RUNNING, CRASHED, STOPPED }
data class GameInstallation(val id:String,val title:String,val installRoot:String,val executablePath:String,val dataDirectory:String,val architecture:String="unknown",val fingerprint:String="",val status:InstallationStatus=InstallationStatus.UNKNOWN,val lastValidatedAt:Long=0L,val lastError:String="")
/** File-backed registry. Writes are staged and atomically promoted where supported. */
class GameInstallationRegistry(private val file:File) {
 private val lock=Any(); private var entries:LinkedHashMap<String,GameInstallation> = load()
 fun snapshot():List<GameInstallation> = synchronized(lock){entries.values.toList()}
 fun get(id:String):GameInstallation? = synchronized(lock){entries[id]}
 fun upsert(g:GameInstallation):Boolean=synchronized(lock){if(g.id.isBlank())return@synchronized false;val n=LinkedHashMap(entries);n[g.id]=g;if(!persist(n))return@synchronized false;entries=n;true}
 fun remove(id:String):Boolean=synchronized(lock){if(!entries.containsKey(id))return@synchronized false;val n=LinkedHashMap(entries);n.remove(id);if(!persist(n))return@synchronized false;entries=n;true}
 private fun load():LinkedHashMap<String,GameInstallation>{val out=linkedMapOf<String,GameInstallation>();if(!file.isFile)return out;val p=Properties();runCatching{file.inputStream().use(p::load)}.getOrElse{return out};for(id in p.getProperty("ids","").split(',').filter{it.isNotBlank()}){val k="game."+id+".";out[id]=GameInstallation(id,p.getProperty(k+"title",""),p.getProperty(k+"root",""),p.getProperty(k+"exe",""),p.getProperty(k+"data",""),p.getProperty(k+"arch","unknown"),p.getProperty(k+"fingerprint",""),runCatching{InstallationStatus.valueOf(p.getProperty(k+"status","UNKNOWN"))}.getOrDefault(InstallationStatus.UNKNOWN),p.getProperty(k+"validated","0").toLongOrNull()?:0L,p.getProperty(k+"error",""))};return LinkedHashMap(out)}
 private fun persist(n:Map<String,GameInstallation>):Boolean=runCatching{val parent=file.absoluteFile.parentFile?:return false;if(!parent.exists()&&!parent.mkdirs())return false;val p=Properties();p.setProperty("ids",n.keys.joinToString(","));n.forEach{(id,g)->val k="game."+id+".";p.setProperty(k+"title",g.title);p.setProperty(k+"root",g.installRoot);p.setProperty(k+"exe",g.executablePath);p.setProperty(k+"data",g.dataDirectory);p.setProperty(k+"arch",g.architecture);p.setProperty(k+"fingerprint",g.fingerprint);p.setProperty(k+"status",g.status.name);p.setProperty(k+"validated",g.lastValidatedAt.toString());p.setProperty(k+"error",g.lastError)};val tmp=File(parent,file.name+".tmp");FileOutputStream(tmp).use{p.store(it,"Ghost Town game catalog");it.fd.sync()};if(file.exists()&&!file.delete())return false;if(!tmp.renameTo(file))return false;true}.getOrDefault(false)
 companion object{fun stableId(root:File):String=MessageDigest.getInstance("SHA-256").digest(root.canonicalPath.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}.take(24)}
}
