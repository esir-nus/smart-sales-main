// File: platforms/harmony/smartsales-app/entry/src/main/ets/pages/ChatPage.ets
// Module: smartsales-app/entry
// Summary: AI 对话页面 — Phase 2C 新领域实现
// Author: created on 2026-04-16

@Entry
@Component
struct ChatPage {
  build() {
    Column() {
      Text('AI 对话')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
      Text('等待 Phase 2C 实现')
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 8 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
