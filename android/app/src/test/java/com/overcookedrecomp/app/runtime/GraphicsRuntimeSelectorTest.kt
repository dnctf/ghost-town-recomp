package com.overcookedrecomp.app.runtime
import org.junit.Assert.*
import org.junit.Test
class GraphicsRuntimeSelectorTest{
 @Test fun prefersTurnipWhenAllVulkanCapabilitiesPresent(){val s=GraphicsRuntimeSelector.select(GraphicsCapabilities(true,true,true,true,true));assertEquals(GraphicsBackend.DXVK_TURNIP,s.backend);assertTrue(s.experimental)}
 @Test fun fallsBackToWined3dWhenVulkanPathIncomplete(){val s=GraphicsRuntimeSelector.select(GraphicsCapabilities(false,false,true,false,true));assertEquals(GraphicsBackend.WINED3D,s.backend)}
 @Test fun blocksWhenNoGraphicsBackend(){assertEquals(GraphicsBackend.BLOCKED,GraphicsRuntimeSelector.select(GraphicsCapabilities(false,false,false,false,false)).backend)}
}