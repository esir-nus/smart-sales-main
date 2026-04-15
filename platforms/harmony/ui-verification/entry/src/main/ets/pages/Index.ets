import {
  HarmonyUiArtifactCard,
  HarmonyUiPageCard,
  HarmonyUiPageId,
  HarmonyUiSchedulerPreviewCard,
  HarmonyUiSchedulerPreviewState,
  HarmonyUiShellState,
  HarmonyUiTingwuState
} from '../model/HarmonyUiModels';
import { harmonyUiDriver } from '../services/HarmonyUiDriver';

@Entry
@Component
struct Index {
  @State private shellState: HarmonyUiShellState = harmonyUiDriver.loadShellState();
  @State private tingwuState: HarmonyUiTingwuState = harmonyUiDriver.loadTingwuState();
  @State private schedulerState: HarmonyUiSchedulerPreviewState = harmonyUiDriver.loadSchedulerPreviewState();
  @State private selectedPageId: HarmonyUiPageId = HarmonyUiPageId.SHELL;
  @State private schedulerUnlocked: boolean = false;

  private visibleCards(): HarmonyUiPageCard[] {
    if (this.schedulerUnlocked) {
      return this.shellState.routeCards;
    }
    return this.shellState.routeCards.filter((card: HarmonyUiPageCard) => card.id !== HarmonyUiPageId.SCHEDULER);
  }

  private selectPage(pageId: HarmonyUiPageId): void {
    if (pageId === HarmonyUiPageId.SCHEDULER && !this.schedulerUnlocked) {
      return;
    }
    this.selectedPageId = pageId;
  }

  build() {
    Scroll() {
      Column({ space: 18 }) {
        this.renderHero();
        this.renderGateCard();
        this.renderRouteCards();
        this.renderActivePage();
      }
      .padding({ left: 18, right: 18, top: 24, bottom: 36 })
      .width('100%')
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F2EADF')
  }

  @Builder
  private renderHero() {
    Column({ space: 10 }) {
      Text('Harmony UI Verification')
        .fontSize(30)
        .fontWeight(FontWeight.Bold)

      Text('Internal ArkUI package for page-by-page native rewrite. This app is not the public Harmony product surface.')
        .fontSize(15)
        .lineHeight(22)
        .opacity(0.72)

      Row({ space: 10 }) {
        this.renderBadge('Bundle', 'smartsales.HOS.ui', '#E2D3BF')
        this.renderBadge('Lane', 'Internal only', '#E7D8C6')
        this.renderBadge('Pages', `${this.visibleCards().length}`, '#F5EDE2')
      }
      .alignItems(VerticalAlign.Center)
    }
    .padding(20)
    .borderRadius(26)
    .backgroundColor('#FFF9F1')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderGateCard() {
    Column({ space: 10 }) {
      Text('Internal gate')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      Text('Scheduler preview remains hidden by default even inside this package. Unlock it only for internal review.')
        .fontSize(14)
        .lineHeight(20)
        .opacity(0.78)

      Button(this.schedulerUnlocked ? 'Scheduler preview unlocked' : 'Unlock scheduler preview')
        .backgroundColor(this.schedulerUnlocked ? '#7D8A63' : '#2F5D50')
        .fontColor('#FFF8ED')
        .borderRadius(16)
        .padding({ left: 18, right: 18, top: 12, bottom: 12 })
        .onClick(() => {
          this.schedulerUnlocked = !this.schedulerUnlocked;
          if (!this.schedulerUnlocked && this.selectedPageId === HarmonyUiPageId.SCHEDULER) {
            this.selectedPageId = HarmonyUiPageId.SHELL;
          }
        })
    }
    .padding(18)
    .borderRadius(24)
    .backgroundColor('#FFF4E6')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderRouteCards() {
    Column({ space: 12 }) {
      Text('Page order')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      ForEach(this.visibleCards(), (card: HarmonyUiPageCard) => {
        Button() {
          Row({ space: 14 }) {
            Column({ space: 4 }) {
              Text(card.title)
                .fontSize(17)
                .fontWeight(FontWeight.Medium)
              Text(card.summary)
                .fontSize(13)
                .lineHeight(18)
                .opacity(0.72)
            }
            .layoutWeight(1)
            .alignItems(HorizontalAlign.Start)

            Text(card.statusLabel)
              .fontSize(12)
              .padding({ left: 10, right: 10, top: 6, bottom: 6 })
              .backgroundColor('#EADBCB')
              .borderRadius(999)
          }
          .width('100%')
          .alignItems(VerticalAlign.Center)
        }
        .backgroundColor(this.selectedPageId === card.id ? '#F0E0CD' : '#FFF9F1')
        .borderRadius(20)
        .padding({ left: 16, right: 16, top: 16, bottom: 16 })
        .onClick(() => {
          this.selectPage(card.id);
        })
      }, (card: HarmonyUiPageCard) => card.id)
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderActivePage() {
    if (this.selectedPageId === HarmonyUiPageId.SHELL) {
      this.renderShellPage();
    } else if (this.selectedPageId === HarmonyUiPageId.TINGWU) {
      this.renderTingwuPage();
    } else {
      this.renderSchedulerPage();
    }
  }

  @Builder
  private renderShellPage() {
    Column({ space: 12 }) {
      Text(this.shellState.title)
        .fontSize(22)
        .fontWeight(FontWeight.Medium)
      Text(this.shellState.summary)
        .fontSize(14)
        .lineHeight(20)
        .opacity(0.78)
      Text('Use this page to verify shell spacing, route chrome, and the internal honesty boundary before docking any real data.')
        .fontSize(14)
        .lineHeight(20)
        .opacity(0.72)
    }
    .padding(20)
    .borderRadius(24)
    .backgroundColor('#FFF9F1')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderTingwuPage() {
    Column({ space: 12 }) {
      Text(this.tingwuState.title)
        .fontSize(22)
        .fontWeight(FontWeight.Medium)
      Text(this.tingwuState.summary)
        .fontSize(14)
        .lineHeight(20)
        .opacity(0.78)
      Text(this.tingwuState.sourceLabel)
        .fontSize(12)
        .fontColor('#7D3B28')

      ForEach(this.tingwuState.notes, (note: string) => {
        Text(note)
          .fontSize(12)
          .lineHeight(18)
          .opacity(0.74)
      }, (note: string) => note)

      ForEach(this.tingwuState.items, (item: HarmonyUiArtifactCard) => {
        Column({ space: 8 }) {
          Row({ space: 12 }) {
            Column({ space: 4 }) {
              Text(item.title)
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
              Text(item.summary)
                .fontSize(13)
                .lineHeight(18)
                .opacity(0.72)
            }
            .layoutWeight(1)
            .alignItems(HorizontalAlign.Start)

            Text(item.statusLabel)
              .fontSize(12)
              .padding({ left: 10, right: 10, top: 6, bottom: 6 })
              .backgroundColor('#E4D5C2')
              .borderRadius(999)
          }
          .width('100%')

          Row({ space: 8 }) {
            ForEach(item.tags, (tag: string) => {
              Text(tag)
                .fontSize(11)
                .padding({ left: 8, right: 8, top: 5, bottom: 5 })
                .backgroundColor('#F1E5D7')
                .borderRadius(999)
            }, (tag: string) => tag)
          }
        }
        .padding(16)
        .borderRadius(18)
        .backgroundColor('#FFF3E5')
        .width('100%')
        .alignItems(HorizontalAlign.Start)
      }, (item: HarmonyUiArtifactCard) => item.title)
    }
    .padding(20)
    .borderRadius(24)
    .backgroundColor('#FFF9F1')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderSchedulerPage() {
    Column({ space: 12 }) {
      Text(this.schedulerState.title)
        .fontSize(22)
        .fontWeight(FontWeight.Medium)
      Text(this.schedulerState.summary)
        .fontSize(14)
        .lineHeight(20)
        .opacity(0.78)
      Text(this.schedulerState.gateLabel)
        .fontSize(12)
        .fontColor('#7D3B28')

      ForEach(this.schedulerState.cards, (card: HarmonyUiSchedulerPreviewCard) => {
        Column({ space: 8 }) {
          Row({ space: 12 }) {
            Column({ space: 4 }) {
              Text(card.title)
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
              Text(card.summary)
                .fontSize(13)
                .lineHeight(18)
                .opacity(0.72)
            }
            .layoutWeight(1)
            .alignItems(HorizontalAlign.Start)

            Text(card.stateLabel)
              .fontSize(12)
              .padding({ left: 10, right: 10, top: 6, bottom: 6 })
              .backgroundColor('#EBD4C2')
              .borderRadius(999)
          }
          .width('100%')

          Text(card.dueLabel)
            .fontSize(12)
            .opacity(0.72)
        }
        .padding(16)
        .borderRadius(18)
        .backgroundColor('#FFF0E1')
        .width('100%')
        .alignItems(HorizontalAlign.Start)
      }, (card: HarmonyUiSchedulerPreviewCard) => card.title)
    }
    .padding(20)
    .borderRadius(24)
    .backgroundColor('#FFF9F1')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderBadge(label: string, value: string, color: string) {
    Row({ space: 8 }) {
      Text(label)
        .fontSize(11)
        .opacity(0.72)
      Text(value)
        .fontSize(12)
        .fontWeight(FontWeight.Medium)
    }
    .padding({ left: 10, right: 10, top: 8, bottom: 8 })
    .backgroundColor(color)
    .borderRadius(999)
  }
}
