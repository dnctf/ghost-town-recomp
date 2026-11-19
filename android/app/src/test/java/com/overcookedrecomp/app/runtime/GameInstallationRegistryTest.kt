package com.overcookedrecomp.app.runtime
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class GameInstallationRegistryTest{
 @Test fun catalogPersistsUpsertsAndDeletes(){val d=File(System.getProperty("java.io.tmpdir"),"catalog-test-"+System.nanoTime());val f=File(d,"catalog.properties");val r=GameInstallationRegistry(f);val g=GameInstallation("id1","Test","/games/test","/games/test/game.exe","/games/test/Game_Data","x86_64");assertTrue(r.upsert(g));assertEquals(g,r.get("id1"));assertEquals(g,GameInstallationRegistry(f).get("id1"));assertTrue(r.remove("id1"));assertNull(GameInstallationRegistry(f).get("id1"));d.deleteRecursively()}
 @Test fun stableIdIsDeterministic(){val d=File(System.getProperty("java.io.tmpdir"));assertEquals(GameInstallationRegistry.stableId(d),GameInstallationRegistry.stableId(d))}
}