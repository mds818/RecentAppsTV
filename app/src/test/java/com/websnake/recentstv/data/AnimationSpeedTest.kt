package com.websnake.recentstv.data
import org.junit.Assert.*
import org.junit.Test
class AnimationSpeedTest {
 @Test fun normalPreservesExistingActionDurationsAndOtherModesScaleConsistently() {
  for (base in listOf(20,170,200,240,260,300)) {
   assertEquals(base,AnimationSpeed.NORMAL.duration(base))
   assertTrue(AnimationSpeed.FAST.duration(base)<base)
   assertTrue(AnimationSpeed.SLOW.duration(base)>base)
  }
  assertEquals(0,AnimationSpeed.SLOW.duration(0))
 }
 @Test fun legacyChoiceMigratesWithoutDefaultingSlowToNormal() {
  assertEquals(AnimationSpeed.NORMAL,AnimationSpeed.legacy(CloseAnimationSpeed.NORMAL))
  assertEquals(AnimationSpeed.SLOW,AnimationSpeed.legacy(CloseAnimationSpeed.SLOW))
  assertEquals(AnimationSpeed.NORMAL,RecentsSettings().animationSpeed)
 }
}
