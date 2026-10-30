package com.websnake.recentstv.ui.settings
import com.websnake.recentstv.data.*
import org.junit.Assert.*
import org.junit.Test
class SettingsStructureTest {
 @Test fun recoveryMenuConfigurationRemainsReachableWithNormalShortcutOff() {
  val s=RecentsSettings(edgeShortcut=EdgeShortcut.OFF,edgeAction=EdgeAction.SWITCH_VIEW,edgeWithoutAccess=EdgeAction.OFF,emptyListRecovery=EmptyListRecovery.MENU)
  assertTrue(SetRow.EDGE_MENU_OPTIONS in SettingsSubmenu.SHORTCUT.rowsFor(s))
  val rows=SettingsSubmenu.EDGE.rowsFor(s)
  assertEquals(listOf(SetRow.EDGE_MENU_SWITCH,SetRow.EDGE_MENU_SETTINGS),rows)
 }
 @Test fun rootOrderStartsWithPresetsAndKeepsToolsLast() {
  assertEquals(0,SettingsMenu.presets)
  assertEquals(SettingsSection.APPEARANCE,SettingsMenu.sectionAt(1))
  assertEquals(SettingsSection.CONTROLS,SettingsMenu.sectionAt(2))
  assertEquals(SettingsSection.APPS,SettingsMenu.sectionAt(3))
  assertEquals(SettingsSection.ADVANCED,SettingsMenu.sectionAt(4))
  assertNull(SettingsMenu.sectionAt(SettingsMenu.presets))
  assertNull(SettingsMenu.sectionAt(SettingsMenu.about))
 }
 @Test fun busyPagesLinkToFocusedSubmenus() {
  val s=RecentsSettings()
  assertTrue(SetRow.DOWN_OPTIONS in SettingsSection.CONTROLS.rowsFor(s))
  assertTrue(SetRow.DOWN_ACTION in SettingsSubmenu.DOWN.rowsFor(s))
  assertFalse(SetRow.DOWN_ACTION in SettingsSection.CONTROLS.rowsFor(s))
  assertTrue(SetRow.VIEW_OPTIONS in SettingsSection.CONTROLS.rowsFor(s))
  assertFalse(SetRow.VIEW_SWITCH in SettingsSection.CONTROLS.rowsFor(s))
  assertEquals(SetRow.EXCEPTIONS, SettingsSection.APPS.rowsFor(s).first())
  assertTrue(SetRow.DISMISS_OPTIONS in SettingsSection.APPS.rowsFor(s))
  assertTrue(SetRow.BULK_OPTIONS in SettingsSection.APPS.rowsFor(s))
  assertTrue(SetRow.BULK_WITHOUT_ACCESS in SettingsSubmenu.BULK.rowsFor(s))
 }
 @Test fun dependentRowsFollowConfigurationWithoutClearingPreferences() {
  val s=RecentsSettings(emptyListRecovery=EmptyListRecovery.OFF, enableDismiss=false, dismissScope=DismissScope.BOTH_VIEWS, edgeShortcut=EdgeShortcut.OFF, edgeAction=EdgeAction.MENU)
  assertFalse(SetRow.DISMISS_SCOPE in SettingsSubmenu.DISMISS.rowsFor(s))
  assertTrue(SetRow.RESTORE_DISMISSED in SettingsSubmenu.DISMISS.rowsFor(s))
  assertFalse(SetRow.EDGE_ACTION in SettingsSubmenu.SHORTCUT.rowsFor(s))
  assertFalse(SetRow.EDGE_MENU_OPTIONS in SettingsSubmenu.SHORTCUT.rowsFor(s))
  assertTrue(SetRow.DISMISS_ENABLE in SettingsSubmenu.DISMISS.rowsFor(s.copy(enableDismiss=true)))
  assertEquals(DismissScope.BOTH_VIEWS,s.dismissScope)
  assertTrue(SetRow.EDGE_MENU_OPTIONS in SettingsSubmenu.SHORTCUT.rowsFor(s.copy(edgeShortcut=EdgeShortcut.ONE_PRESS)))
 }
 @Test fun visibilityDependsOnPlacementRatherThanServiceAvailability() {
  val s=RecentsSettings(viewButtons=ViewButtons.APP_ROW, killAllLocation=KillAllLocation.APP_ROW, smoothMotion=false)
  assertFalse(SetRow.HIDE_TOP_SWITCH in SettingsSubmenu.SHORTCUT.rowsFor(s))
  assertFalse(SetRow.HIDE_TOP_BULK in SettingsSubmenu.BULK.rowsFor(s))
  assertFalse(SetRow.CLOSE_SPEED in SettingsSection.CONTROLS.rowsFor(s))
  assertTrue(SetRow.BULK_ACTION in SettingsSubmenu.BULK.rowsFor(s.copy(usePrivileged=false)))
 }
}
