package com.overcookedrecomp.app.runtime
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
data class RuntimeDiagnosticEvent(val timestamp:Long=System.currentTimeMillis(),val level:String,val stage:String,val code:String,val message:String)
/** Session log with one-file rotation. */
class RuntimeDiagnostics(private val directory:File,private val maxBytes:Long=1024L*1024L){private val lock=Any();fun record(e:RuntimeDiagnosticEvent):Boolean=synchronized(lock){runCatching{if(!directory.exists()&&!directory.mkdirs())return false;val f=File(directory,"runtime-session.log");val line=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",Locale.US).format(Date(e.timestamp))+" "+e.level+"/"+e.stage+"/"+e.code+": "+e.message.replace('\n',' ')+"\n";if(f.exists()&&f.length()+line.toByteArray(Charsets.UTF_8).size>maxBytes){val b=File(directory,"runtime-session.log.1");if(b.exists())b.delete();if(!f.renameTo(b))return false};f.appendText(line,Charsets.UTF_8);true}.getOrDefault(false)};fun readTail(maxChars:Int=20000):String=runCatching{File(directory,"runtime-session.log").takeIf{it.isFile}?.readText(Charsets.UTF_8)?.takeLast(maxChars)?:""}.getOrDefault("")}
