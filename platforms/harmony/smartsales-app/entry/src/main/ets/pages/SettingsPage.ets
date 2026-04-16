// File: platforms/harmony/smartsales-app/entry/src/main/ets/pages/SettingsPage.ets
// Module: smartsales-app/entry
// Summary: 设置页面 — Phase 2E 设备集成与打磨
// Author: created on 2026-04-16

@Entry
@Component
struct SettingsPage {
  build() {
    Column() {
      Text('设置')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
      Text('等待 Phase 2E 实现')
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 8 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
