package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolverDirCacheTest {

    @Test
    fun defectoSiEscribible() {
        assertEquals(ModoCache.DEFECTO, resolverDirCache(true))
    }

    @Test
    fun internaSiFalla() {
        assertEquals(ModoCache.INTERNA, resolverDirCache(false))
    }
}
