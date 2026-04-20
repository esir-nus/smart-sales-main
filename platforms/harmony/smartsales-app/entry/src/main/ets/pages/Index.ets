// File: platforms/harmony/smartsales-app/entry/src/main/ets/pages/Index.ets
// Module: smartsales-app/entry
// Summary: 主导航壳页面，承载底部标签栏和页面路由
// Author: created on 2026-04-16

import { AudioPage } from './AudioPage';

@Entry
@Component
struct Index {
  @State currentTab: number = 0;

  // 标签页配置：标签名和对应路由
  private readonly tabs: TabItem[] = [
    { label: '录音', icon: $r('sys.symbol.mic'), route: 'pages/AudioPage' },
    { label: '日程', icon: $r('sys.symbol.calendar'), route: 'pages/SchedulerPage' },
    { label: '对话', icon: $r('sys.symbol.message'), route: 'pages/ChatPage' },
    { label: '联系人', icon: $r('sys.symbol.person_2'), route: 'pages/ContactsPage' },
    { label: '设置', icon: $r('sys.symbol.gearshape'), route: 'pages/SettingsPage' }
  ];

  build() {
    Column() {
      // 主内容区域
      Stack() {
        if (this.currentTab === 0) {
          AudioPage()
        } else if (this.currentTab === 1) {
          SchedulerPagePlaceholder()
        } else if (this.currentTab === 2) {
          ChatPagePlaceholder()
        } else if (this.currentTab === 3) {
          ContactsPagePlaceholder()
        } else {
          SettingsPagePlaceholder()
        }
      }
      .width('100%')
      .layoutWeight(1)

      // 底部标签栏
      Row() {
        ForEach(this.tabs, (tab: TabItem, index: number) => {
          Column() {
            SymbolGlyph(tab.icon)
              .fontSize(24)
              .fontColor([this.currentTab === index ? '#1A73E8' : '#999999'])
            Text(tab.label)
              .fontSize(10)
              .fontColor(this.currentTab === index ? '#1A73E8' : '#999999')
              .margin({ top: 2 })
          }
          .layoutWeight(1)
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)
          .height(56)
          .onClick(() => {
            this.currentTab = index as number;
          })
        }, (tab: TabItem, index: number) => index.toString())
      }
      .width('100%')
      .backgroundColor('#FFFFFF')
      .border({ width: { top: 0.5 }, color: '#E0E0E0' })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}

// 标签页数据模型
interface TabItem {
  label: string;
  icon: Resource;
  route: string;
}

@Component
struct SchedulerPagePlaceholder {
  build() {
    Column() {
      Text('日程')
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
      Text('任务创建、冲突检测、提醒管理')
        .fontSize(14)
        .fontColor('#666666')
        .margin({ top: 8 })
      Text('Phase 2B: 基于 H-02 后端验证构建')
        .fontSize(12)
        .fontColor('#999999')
        .margin({ top: 16 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }
}

@Component
struct ChatPagePlaceholder {
  build() {
    Column() {
      Text('AI 对话')
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
      Text('LLM 对话、会话绑定、上下文组装')
        .fontSize(14)
        .fontColor('#666666')
        .margin({ top: 8 })
      Text('Phase 2C: 新领域实现')
        .fontSize(12)
        .fontColor('#999999')
        .margin({ top: 16 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }
}

@Component
struct ContactsPagePlaceholder {
  build() {
    Column() {
      Text('联系人')
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
      Text('实体管理、客户资料、别名解析')
        .fontSize(14)
        .fontColor('#666666')
        .margin({ top: 8 })
      Text('Phase 2D: CRM/实体管理')
        .fontSize(12)
        .fontColor('#999999')
        .margin({ top: 16 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }
}

@Component
struct SettingsPagePlaceholder {
  build() {
    Column() {
      Text('设置')
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
      Text('配置管理、权限、通知')
        .fontSize(14)
        .fontColor('#666666')
        .margin({ top: 8 })
      Text('Phase 2E: 设备集成与打磨')
        .fontSize(12)
        .fontColor('#999999')
        .margin({ top: 16 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }
}
