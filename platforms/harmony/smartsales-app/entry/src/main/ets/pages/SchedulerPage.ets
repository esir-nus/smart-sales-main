// File: platforms/harmony/smartsales-app/entry/src/main/ets/pages/SchedulerPage.ets
// Module: smartsales-app/entry
// Summary: 日程管理页面 — Phase 2B 基于后端验证构建
// Author: created on 2026-04-16

@Entry
@Component
struct SchedulerPage {
  build() {
    Column() {
      Text('日程管理')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
      Text('等待 Phase 2B 实现')
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 8 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
