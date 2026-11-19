package com.overcookedrecomp.app.runtime
/** Android Activity/Surface adapter contract. Surface attachment alone is not proof of first rendered frame. */
interface AndroidGameSessionHost{fun enterImmersive();fun attachGameSurface(surfaceToken:Any):Boolean;fun reportFirstFramePresented();fun forwardInput(event:Any):Boolean;fun suspendSession();fun resumeSession();fun exitImmersive()}
