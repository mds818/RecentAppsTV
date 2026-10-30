package com.websnake.recentstv.data
import org.junit.Assert.*
import org.junit.Test
class ViewOptionsTest {
 @Test fun viewBulkNeverClosesHistory() {
  assertEquals(DownAction.CLOSE_TASK, ActionPolicy.bulkForAccess(BulkWithAccess.BY_VIEW, BulkWithoutAccess.HIDDEN, true, true, AppListSource.TASKS))
  assertEquals(DownAction.DISMISS, ActionPolicy.bulkForAccess(BulkWithAccess.BY_VIEW, BulkWithoutAccess.HIDDEN, true, true, AppListSource.HISTORY))
  assertEquals(DownAction.OFF, ActionPolicy.bulkForAccess(BulkWithAccess.BY_VIEW, BulkWithoutAccess.HIDDEN, true, false, AppListSource.HISTORY))
  assertEquals(DownAction.OFF, ActionPolicy.bulkForAccess(BulkWithAccess.BY_VIEW, BulkWithoutAccess.HIDDEN, false, true, AppListSource.TASKS))
 }
 @Test fun menuFiltersCapabilitiesWithoutAddingUnselectedSettings() {
  assertEquals(listOf(EdgeMenuItem.SETTINGS), EdgeMenuPolicy.available(EdgeMenuItem.entries.toSet(), false, false))
  assertEquals(listOf(EdgeMenuItem.DISMISS_ALL, EdgeMenuItem.SETTINGS), EdgeMenuPolicy.available(EdgeMenuItem.entries.toSet(), false, true))
  assertTrue(EdgeMenuPolicy.available(emptySet(), true, true).isEmpty())
  assertFalse(EdgeMenuItem.SETTINGS in EdgeMenuPolicy.defaults)
 }
 @Test fun dismissalScopeKeepsActualTasksVisible() {
  assertTrue(DismissPolicy.applies(DismissScope.RECENT_ONLY, AppListSource.HISTORY))
  assertFalse(DismissPolicy.applies(DismissScope.RECENT_ONLY, AppListSource.TASKS))
  assertTrue(DismissPolicy.applies(DismissScope.BOTH_VIEWS, AppListSource.TASKS))
  assertFalse(DismissPolicy.applies(DismissScope.BOTH_VIEWS, AppListSource.NEEDS_ACCESS))
 }
 @Test fun edgeModesMigrateIndependentlyAndRestoreRequiresDismissedEntries() {
  assertEquals(EdgeAction.DISMISS_ALL,EdgeActionPolicy.legacyFallback(EdgeAction.CLOSE_OR_DISMISS))
  assertEquals(EdgeAction.OFF,EdgeActionPolicy.legacyFallback(EdgeAction.SWITCH_VIEW))
  assertEquals(EdgeAction.MENU,EdgeActionPolicy.resolve(EdgeAction.SWITCH_VIEW,EdgeAction.MENU,false))
  assertTrue(EdgeMenuItem.RESTORE_DISMISSED in EdgeMenuPolicy.defaults)
  assertFalse(EdgeMenuItem.RESTORE_DISMISSED in EdgeMenuPolicy.available(EdgeMenuPolicy.defaults,false,true,false))
  assertTrue(EdgeMenuItem.RESTORE_DISMISSED in EdgeMenuPolicy.available(EdgeMenuPolicy.defaults,false,true,true))
  val defaults=AppearancePreset.DEFAULT.appearance()
  assertEquals(ViewButtons.HIDDEN,defaults.viewButtons)
  assertEquals(EdgeShortcut.ADAPTIVE,defaults.edgeShortcut)
  assertTrue(defaults.enableDismiss); assertTrue(defaults.showTaskIndicator)
  assertEquals(DownWithAccess.CLOSE_OR_DISMISS,defaults.downWithAccess)
  assertEquals(DownWithoutAccess.DISMISS,defaults.downWithoutAccess)
 }
 @Test fun restoreMenuDefaultsOnForUpgradesButCanBeDisabled() {
  val original=setOf(EdgeMenuItem.SETTINGS)
  assertEquals(original + EdgeMenuItem.RESTORE_DISMISSED, EdgeMenuPolicy.selection(original,false))
  assertEquals(original,EdgeMenuPolicy.selection(original,true))
  assertTrue(EdgeMenuPolicy.selection(emptySet(),true).isEmpty())
 }
 @Test fun adaptiveAndRecoveryActivationDoNotRewriteTheConfiguredMode() {
  assertEquals(EdgeShortcut.ONE_PRESS,EdgeActivationPolicy.resolve(EdgeShortcut.ADAPTIVE,true,false))
  assertEquals(EdgeShortcut.TWO_PRESSES,EdgeActivationPolicy.resolve(EdgeShortcut.ADAPTIVE,false,false))
  assertEquals(EdgeShortcut.TWO_PRESSES,EdgeActivationPolicy.resolve(EdgeShortcut.TWO_PRESSES,true,false))
  assertEquals(EdgeShortcut.ONE_PRESS,EdgeActivationPolicy.resolve(EdgeShortcut.OFF,false,true))
  assertEquals(EdgeShortcut.OFF,EdgeActivationPolicy.resolve(EdgeShortcut.OFF,false,false))
  assertEquals(EdgeAction.OFF,EdgeActionPolicy.privilegedChoices.first())
  assertEquals(EdgeAction.OFF,EdgeActionPolicy.fallbackChoices.first())
 }
 @Test fun recoveryMenuOmitsBulkActionsAndTopRestoreSupportsRecentOnly() {
  val all=EdgeMenuItem.entries.toSet()
  val recovery=EdgeMenuPolicy.recoverySelection(all)
  assertFalse(EdgeMenuItem.CLOSE_ALL in recovery); assertFalse(EdgeMenuItem.DISMISS_ALL in recovery)
  assertTrue(EdgeMenuItem.RESTORE_DISMISSED in recovery)
  assertFalse(EdgeMenuItem.SETTINGS in EdgeMenuPolicy.recoverySelection(EdgeMenuPolicy.defaults))
  val available=EdgeMenuPolicy.available(all,true,true,true,false,false)
  assertFalse(EdgeMenuItem.CLOSE_ALL in available); assertFalse(EdgeMenuItem.DISMISS_ALL in available)
  assertTrue(RestoreTopMode.RECENT_ONLY.visible(AppListSource.HISTORY))
  assertFalse(RestoreTopMode.RECENT_ONLY.visible(AppListSource.TASKS))
 }
 @Test fun profileRestoreVisibilityMatchesRequestedDefaults() {
  for (preset in AppearancePreset.entries) {
   val s=preset.appearance()
   assertEquals(EdgeShortcut.ADAPTIVE,s.edgeShortcut)
   assertEquals(RestoreTopMode.RECENT_ONLY,s.restoreTopMode)
   assertEquals(EmptyListRecovery.MENU,s.emptyListRecovery)
   assertEquals(preset==AppearancePreset.CLEAN || preset==AppearancePreset.CLEAN_PLUS,s.hideTopRestore)
  }
  val clean=AppearancePreset.CLEAN_PLUS.appearance()
  assertEquals(BulkWithAccess.CLOSE_ALL,clean.bulkWithAccess)
  assertEquals(KillAllLocation.TOP_CONTROLS,clean.killAllLocation);assertTrue(clean.hideTopBulk)
  assertEquals(ViewNoticeStyle.SUBTLE,clean.viewNoticeStyle)
 }
 @Test fun adaptiveContextRestoreUsesDirectReachabilityAndStaysAtBottom() {
  assertTrue(RestoreAccessPolicy.showInContext(ContextRestoreMode.ADAPTIVE,true,false,false))
  assertFalse(RestoreAccessPolicy.showInContext(ContextRestoreMode.ADAPTIVE,true,true,false))
  assertFalse(RestoreAccessPolicy.showInContext(ContextRestoreMode.ADAPTIVE,true,false,true))
  assertFalse(RestoreAccessPolicy.showInContext(ContextRestoreMode.OFF,true,false,false))
  assertTrue(RestoreAccessPolicy.showInContext(ContextRestoreMode.ON,true,true,true))
  assertFalse(RestoreAccessPolicy.showInContext(ContextRestoreMode.ON,false,false,false))
  assertEquals(5,ActionPolicy.context(true,true,true,true).last())
  assertEquals(ContextRestoreMode.ADAPTIVE,RecentsSettings().contextRestoreMode)
  assertTrue(RecentsSettings().rememberView)
  for (preset in AppearancePreset.entries) assertTrue(preset.appearance().rememberView)
 }
 @Test fun conditionalHeaderAndClean() {
  assertTrue(HeaderMode.WITH_ACCESS.visible(true)); assertFalse(HeaderMode.WITH_ACCESS.visible(false))
  assertTrue(HeaderMode.WITHOUT_ACCESS.visible(false)); assertFalse(HeaderMode.WITHOUT_ACCESS.visible(true))
  val clean=AppearancePreset.CLEAN.appearance()
  assertTrue(clean.showTaskIndicator)
  assertEquals(ViewButtons.HIDDEN, clean.viewButtons)
  assertEquals(EdgeShortcut.ADAPTIVE, clean.edgeShortcut)
  assertTrue(clean.headerMode.visible(true)); assertTrue(clean.headerMode.visible(false))
 }
}
