import { common } from '@kit.AbilityKit';
import {
  HarmonyContainerSnapshot,
  HarmonyJobStatus,
  HarmonyTingwuArtifacts,
  HarmonyTingwuAudioItem,
  createEmptySnapshot
} from '../model/HarmonyTingwuModels';
import { harmonyContainerRepository } from '../services/HarmonyContainerRepository';
import {
  formatBytes,
  formatDateTime,
  formatDuration,
  statusLabel
} from '../utils/HarmonyFormatUtils';

@Entry
@Component
struct Index {
  @State snapshot: HarmonyContainerSnapshot = createEmptySnapshot();
  @State selectedAudioId: string = '';
  @State selectedArtifacts?: HarmonyTingwuArtifacts = undefined;
  @State detailErrorMessage: string = '';
  private unsubscribe?: () => void;

  private readonly supported: string[] = [
    'Local file import only',
    'Harmony-owned OSS upload and Tingwu polling',
    'Transcript, chapters, keywords, and summary reopen',
    'Harmony namespaced metadata and artifact files'
  ];

  private readonly hidden: string[] = [
    'Scheduler create, reschedule, reminder, follow-up',
    'Ask-AI, audio-grounded chat, session binding',
    'Onboarding scheduler handoff',
    'Badge pairing, sync, download, and hardware flows'
  ];

  aboutToAppear(): void {
    const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
    harmonyContainerRepository.initialize(context)
      .catch((error: Error) => {
        this.snapshot = {
          ...this.snapshot,
          isLoading: false,
          lastErrorMessage: error.message
        };
      });
    this.unsubscribe = harmonyContainerRepository.subscribe((next: HarmonyContainerSnapshot) => {
      this.snapshot = next;
      if (!this.selectedAudioId && next.items.length > 0) {
        this.selectItem(next.items[0].id);
      }
      const selected = this.currentItem();
      if (selected && selected.status === HarmonyJobStatus.COMPLETED) {
        this.loadArtifacts(selected.id);
      }
    });
  }

  aboutToDisappear(): void {
    this.unsubscribe?.();
    this.unsubscribe = undefined;
  }

  private currentItem(): HarmonyTingwuAudioItem | undefined {
    return this.snapshot.items.find((item: HarmonyTingwuAudioItem) => item.id === this.selectedAudioId);
  }

  private async handleImport(): Promise<void> {
    const item = await harmonyContainerRepository.importAudio();
    if (item) {
      await this.selectItem(item.id);
    }
  }

  private async selectItem(audioId: string): Promise<void> {
    this.selectedAudioId = audioId;
    this.detailErrorMessage = '';
    await this.loadArtifacts(audioId);
  }

  private async loadArtifacts(audioId: string): Promise<void> {
    const selected = this.snapshot.items.find((item: HarmonyTingwuAudioItem) => item.id === audioId);
    if (!selected || selected.status !== HarmonyJobStatus.COMPLETED) {
      this.selectedArtifacts = undefined;
      return;
    }
    this.selectedArtifacts = await harmonyContainerRepository.loadArtifacts(audioId);
  }

  private async handleProcess(audioId: string): Promise<void> {
    this.detailErrorMessage = '';
    try {
      await harmonyContainerRepository.startTingwu(audioId);
      await this.loadArtifacts(audioId);
    } catch (error) {
      this.detailErrorMessage = error instanceof Error ? error.message : `${error}`;
    }
  }

  build() {
    Scroll() {
      Column({ space: 18 }) {
        this.renderHero()
        this.renderConfigCard()
        this.renderCapabilities('Supported now', this.supported, '#F6F0E4')
        this.renderInventoryPanel()
        this.renderDetailPanel()
        this.renderCapabilities('Hidden and unsupported', this.hidden, '#EEDFD1')
      }
      .padding({ left: 18, right: 18, top: 24, bottom: 36 })
      .width('100%')
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#E8DCCA')
  }

  @Builder
  private renderHero() {
    Column({ space: 12 }) {
      Text('Harmony Tingwu Container')
        .fontSize(30)
        .fontWeight(FontWeight.Bold)

      Text('Transient Harmony-native lane for file import, OSS upload, Tingwu processing, and persisted artifact reopen.')
        .fontSize(15)
        .lineHeight(22)
        .opacity(0.72)

      Row({ space: 12 }) {
        Button(this.snapshot.isBusy ? 'Working...' : 'Import Local Audio')
          .enabled(!this.snapshot.isBusy)
          .backgroundColor('#2F5D50')
          .fontColor('#FFF8ED')
          .borderRadius(18)
          .padding({ left: 18, right: 18, top: 12, bottom: 12 })
          .onClick(async () => {
            await this.handleImport();
          })

        Column({ space: 2 }) {
          Text(`${this.snapshot.items.length} item${this.snapshot.items.length === 1 ? '' : 's'}`)
            .fontSize(14)
            .fontWeight(FontWeight.Medium)
          if (this.snapshot.isBusy) {
            Text(this.snapshot.busyMessage)
              .fontSize(12)
              .opacity(0.7)
          }
        }
        .alignItems(HorizontalAlign.Start)
      }

      if (this.snapshot.lastErrorMessage) {
        Text(this.snapshot.lastErrorMessage)
          .fontSize(13)
          .fontColor('#8D2F2F')
      }
    }
    .padding(20)
    .borderRadius(26)
    .backgroundColor('#FFF8ED')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderConfigCard() {
    Column({ space: 10 }) {
      Text('Config boundary')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      Text('Live processing uses repo-root `local.properties` and the Harmony build-generated local config artifact. Fresh clones fall back to empty config until that artifact exists.')
        .fontSize(14)
        .lineHeight(20)

      if (this.snapshot.configMissingKeys.length > 0) {
        ForEach(this.snapshot.configMissingKeys, (key: string) => {
          Text(`- missing ${key}`)
            .fontSize(13)
            .fontColor('#8D2F2F')
        })
      } else {
        Text('All required Harmony-local keys are present for this container build.')
          .fontSize(13)
          .fontColor('#2E5F4D')
      }
    }
    .padding(18)
    .borderRadius(22)
    .backgroundColor('#F4E4D6')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderInventoryPanel() {
    Column({ space: 12 }) {
      Text('Container inventory')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      if (this.snapshot.items.length === 0) {
        Text('Import a local recording to create a Harmony-owned `harmony_<audioId>.*` item and start the Tingwu-only flow.')
          .fontSize(14)
          .lineHeight(21)
          .opacity(0.72)
      } else {
        ForEach(this.snapshot.items, (item: HarmonyTingwuAudioItem) => {
          const isSelected = this.selectedAudioId === item.id;
          const primaryTextColor = isSelected ? '#FFF8ED' : '#2B241C';
          const secondaryTextColor = isSelected ? '#F3E7D5' : '#5A5148';
          Column({ space: 10 }) {
            Row() {
              Column({ space: 4 }) {
                Text(item.displayName)
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(primaryTextColor)
                Text(`${statusLabel(item.status)} · ${formatBytes(item.sizeBytes)} · ${formatDateTime(item.importedAt)}`)
                  .fontSize(12)
                  .opacity(0.66)
                  .fontColor(secondaryTextColor)
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)

              Text(`${item.progressPercent}%`)
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor(isSelected ? '#F8F2E9' : '#2F5D50')
            }

            Progress({ value: item.progressPercent, total: 100, type: ProgressType.Linear })
              .color('#2F5D50')
              .backgroundColor('#D7D0C6')
              .height(8)
              .borderRadius(8)
              .width('100%')

            if (item.summary) {
              Text(item.summary)
                .fontSize(13)
                .lineHeight(19)
                .maxLines(3)
                .fontColor(primaryTextColor)
            }

            if (item.lastErrorMessage) {
              Text(item.lastErrorMessage)
                .fontSize(12)
                .fontColor('#8D2F2F')
            }
          }
          .padding(16)
          .borderRadius(20)
          .backgroundColor(isSelected ? '#2F5D50' : '#FFF8ED')
          .width('100%')
          .alignItems(HorizontalAlign.Start)
          .onClick(() => {
            this.selectItem(item.id);
          })
        })
      }
    }
    .padding(18)
    .borderRadius(24)
    .backgroundColor('#F2EBDD')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderDetailPanel() {
    const item = this.currentItem();
    Column({ space: 12 }) {
      Text('Artifact detail')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      if (!item) {
        Text('Select an imported Harmony audio item to review its Tingwu-only state, then process or reopen artifacts.')
          .fontSize(14)
          .lineHeight(21)
          .opacity(0.72)
      } else {
        Text(item.displayName)
          .fontSize(18)
          .fontWeight(FontWeight.Medium)

        Text(`Local file: ${item.localFileName}`)
          .fontSize(13)
          .opacity(0.7)

        Row({ space: 12 }) {
          Button(item.status === HarmonyJobStatus.COMPLETED ? 'Re-run Tingwu' : 'Process with Tingwu')
            .enabled(item.status !== HarmonyJobStatus.UPLOADING && item.status !== HarmonyJobStatus.PROCESSING)
            .backgroundColor('#8C5A34')
            .fontColor('#FFF8ED')
            .borderRadius(16)
            .onClick(async () => {
              await this.handleProcess(item.id);
            })

          Text(statusLabel(item.status))
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
        }

        if (this.detailErrorMessage) {
          Text(this.detailErrorMessage)
            .fontSize(12)
            .fontColor('#8D2F2F')
        }

        if (item.keywords.length > 0) {
          Text(`Keywords: ${item.keywords.join(' · ')}`)
            .fontSize(13)
            .lineHeight(19)
        }

        if (this.selectedArtifacts?.smartSummary?.summary) {
          this.renderSubsection('Summary', this.selectedArtifacts.smartSummary.summary)
        }

        if ((this.selectedArtifacts?.smartSummary?.keyPoints || []).length > 0) {
          this.renderListSubsection('Key points', this.selectedArtifacts?.smartSummary?.keyPoints || [])
        }

        if ((this.selectedArtifacts?.chapters || []).length > 0) {
          Column({ space: 8 }) {
            Text('Chapters')
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
            ForEach(this.selectedArtifacts?.chapters || [], (chapter) => {
              Text(`- ${chapter.title} (${formatDuration(chapter.startMs)}-${formatDuration(chapter.endMs)})${chapter.summary ? `: ${chapter.summary}` : ''}`)
                .fontSize(13)
                .lineHeight(20)
            })
          }
          .alignItems(HorizontalAlign.Start)
          .width('100%')
        }

        if (this.selectedArtifacts?.transcriptMarkdown) {
          this.renderSubsection('Transcript', this.selectedArtifacts.transcriptMarkdown)
        }

        if ((this.selectedArtifacts?.resultLinks || []).length > 0) {
          Column({ space: 8 }) {
            Text('Provider links')
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
            ForEach(this.selectedArtifacts?.resultLinks || [], (link) => {
              Text(`- ${link.label}: ${link.url}`)
                .fontSize(12)
                .lineHeight(18)
            })
          }
          .alignItems(HorizontalAlign.Start)
          .width('100%')
        }
      }
    }
    .padding(18)
    .borderRadius(24)
    .backgroundColor('#FFF8ED')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderSubsection(title: string, body: string) {
    Column({ space: 8 }) {
      Text(title)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
      Text(body)
        .fontSize(13)
        .lineHeight(20)
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
  }

  @Builder
  private renderListSubsection(title: string, rows: string[]) {
    Column({ space: 8 }) {
      Text(title)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
      ForEach(rows, (row: string) => {
        Text(`- ${row}`)
          .fontSize(13)
          .lineHeight(20)
      })
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
  }

  @Builder
  private renderCapabilities(title: string, rows: string[], background: string) {
    Column({ space: 10 }) {
      Text(title)
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      ForEach(rows, (row: string) => {
        Text(`- ${row}`)
          .fontSize(14)
          .lineHeight(21)
      })
    }
    .padding(18)
    .borderRadius(22)
    .backgroundColor(background)
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }
}
