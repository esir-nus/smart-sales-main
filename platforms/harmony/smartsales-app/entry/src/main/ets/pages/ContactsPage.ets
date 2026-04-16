// File: platforms/harmony/smartsales-app/entry/src/main/ets/pages/ContactsPage.ets
// Module: smartsales-app/entry
// Summary: 联系人/实体管理页面 — Phase 2D CRM 实现
// Author: created on 2026-04-16

@Entry
@Component
struct ContactsPage {
  build() {
    Column() {
      Text('联系人')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
      Text('等待 Phase 2D 实现')
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 8 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
