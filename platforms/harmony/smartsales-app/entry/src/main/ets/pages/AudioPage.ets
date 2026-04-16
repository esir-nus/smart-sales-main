// File: platforms/harmony/smartsales-app/entry/src/main/ets/pages/AudioPage.ets
// Module: smartsales-app/entry
// Summary: 音频/听悟页面 — 迁移自 tingwu-container Index.ets
// Author: created on 2026-04-16

import { common } from '@kit.AbilityKit';
import {
  AudioSnapshot,
  AudioJobStatus,
  AudioArtifacts,
  AudioItem,
  SpeakerSummary,
  QuestionAnswer,
  createEmptyAudioSnapshot
} from '../domain/Audio';
import { audioRepository } from '../services/AudioRepository';
import {
  formatBytes,
  formatDateTime,
  statusLabel
} from '../utils/FormatUtils';

@Component
export struct AudioPage {
  @State snapshot: AudioSnapshot = createEmptyAudioSnapshot();
  @State selectedAudioId: string = '';
  @State selectedArtifacts: AudioArtifacts | undefined = undefined;
  @State detailErrorMessage: string = '';
  @State expandedSection: string = '';
  private unsubscribe?: () => void;

  aboutToAppear(): void {
    const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
    audioRepository.initialize(context)
      .catch((error: Error) => {
        // 逐字段构造新快照，避免 ArkTS 禁止的 spread 语法
        this.snapshot = {
          items: this.snapshot.items,
          configMissingKeys: this.snapshot.configMissingKeys,
          isLoading: false,
          isBusy: this.snapshot.isBusy,
          busyMessage: this.snapshot.busyMessage,
          lastErrorMessage: error.message
        };
      });
    this.unsubscribe = audioRepository.subscribe((next: AudioSnapshot) => {
      this.snapshot = next;
      if (!this.selectedAudioId && next.items.length > 0) {
        this.selectItem(next.items[0].id);
      }
      const selected = this.currentItem();
      if (selected && selected.status === AudioJobStatus.COMPLETED) {
        this.loadArtifacts(selected.id);
      }
    });
  }

  aboutToDisappear(): void {
    this.unsubscribe?.();
    this.unsubscribe = undefined;
  }

  private currentItem(): AudioItem | undefined {
    return this.snapshot.items.find((item: AudioItem) => item.id === this.selectedAudioId);
  }

  private async handleImport(): Promise<void> {
    const item = await audioRepository.importAudio();
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
    const selected = this.snapshot.items.find((item: AudioItem) => item.id === audioId);
    if (!selected || selected.status !== AudioJobStatus.COMPLETED) {
      this.selectedArtifacts = undefined;
      return;
    }
    this.selectedArtifacts = await audioRepository.loadArtifacts(audioId);
  }

  private async handleProcess(audioId: string): Promise<void> {
    this.detailErrorMessage = '';
    try {
      await audioRepository.startTingwu(audioId);
      await this.loadArtifacts(audioId);
    } catch (error) {
      this.detailErrorMessage = error instanceof Error ? error.message : `${error}`;
    }
  }

  // 辅助：计算项目选中状态颜色，避免在 @Builder 内使用 const
  private primaryColor(itemId: string): string {
    return this.selectedAudioId === itemId ? '#FFF8ED' : '#2B241C';
  }

  private secondaryColor(itemId: string): string {
    return this.selectedAudioId === itemId ? '#F3E7D5' : '#5A5148';
  }

  private rowBackground(itemId: string): string {
    return this.selectedAudioId === itemId ? '#2F5D50' : '#FFF8ED';
  }

  private progressFontColor(itemId: string): string {
    return this.selectedAudioId === itemId ? '#F8F2E9' : '#2F5D50';
  }

  private toggleSection(key: string): void {
    this.expandedSection = this.expandedSection === key ? '' : key;
  }

  build() {
    Scroll() {
      Column({ space: 18 }) {
        this.renderHero()
        this.renderConfigCard()
        this.renderInventoryPanel()
        this.renderDetailPanel()
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
  renderHero() {
    Column({ space: 12 }) {
      Text('Audio Processing')
        .fontSize(30)
        .fontWeight(FontWeight.Bold)

      Text('Import local recordings, upload via OSS, process with Tingwu, and view transcripts and summaries.')
        .fontSize(15)
        .lineHeight(22)
        .opacity(0.72)

      Row({ space: 12 }) {
        Button(this.snapshot.isBusy ? 'Working...' : 'Import Audio')
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
  renderConfigCard() {
    Column({ space: 10 }) {
      Text('Configuration')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      Text('Audio processing requires Tingwu and OSS credentials configured in the build environment.')
        .fontSize(14)
        .lineHeight(20)

      if (this.snapshot.configMissingKeys.length > 0) {
        ForEach(this.snapshot.configMissingKeys, (key: string) => {
          Text(`- missing ${key}`)
            .fontSize(13)
            .fontColor('#8D2F2F')
        })
      } else {
        Text('All required configuration keys are present.')
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
  renderInventoryPanel() {
    Column({ space: 12 }) {
      Text('Audio items')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      if (this.snapshot.items.length === 0) {
        Text('Import a local recording to start.')
          .fontSize(14)
          .lineHeight(21)
          .opacity(0.72)
      } else {
        ForEach(this.snapshot.items, (item: AudioItem) => {
          Column({ space: 10 }) {
            Row() {
              Column({ space: 4 }) {
                Text(item.displayName)
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(this.primaryColor(item.id))
                Text(`${statusLabel(item.status)} · ${formatBytes(item.sizeBytes)} · ${formatDateTime(item.importedAt)}`)
                  .fontSize(12)
                  .opacity(0.66)
                  .fontColor(this.secondaryColor(item.id))
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)

              Text(`${item.progressPercent}%`)
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor(this.progressFontColor(item.id))
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
                .fontColor(this.primaryColor(item.id))
            }

            if (item.lastErrorMessage) {
              Text(item.lastErrorMessage)
                .fontSize(12)
                .fontColor('#8D2F2F')
            }
          }
          .padding(16)
          .borderRadius(20)
          .backgroundColor(this.rowBackground(item.id))
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
  renderDetailPanel() {
    Column({ space: 12 }) {
      Text('Details')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      if (this.currentItem() === undefined) {
        Text('Select an audio item to view details.')
          .fontSize(14)
          .lineHeight(21)
          .opacity(0.72)
      } else {
        this.renderSelectedDetails()
      }
    }
    .padding(18)
    .borderRadius(24)
    .backgroundColor('#FFF8ED')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  renderSelectedDetails() {
    Text(this.currentItem()!.displayName)
      .fontSize(18)
      .fontWeight(FontWeight.Medium)

    Text(`File: ${this.currentItem()!.localFileName}`)
      .fontSize(13)
      .opacity(0.7)

    Row({ space: 12 }) {
      Button(this.currentItem()!.status === AudioJobStatus.COMPLETED ? 'Re-process' : 'Process')
        .enabled(
          this.currentItem()!.status !== AudioJobStatus.UPLOADING
            && this.currentItem()!.status !== AudioJobStatus.PROCESSING
        )
        .backgroundColor('#8C5A34')
        .fontColor('#FFF8ED')
        .borderRadius(16)
        .onClick(async () => {
          await this.handleProcess(this.currentItem()!.id);
        })

      Text(statusLabel(this.currentItem()!.status))
        .fontSize(13)
        .fontWeight(FontWeight.Medium)
    }

    if (this.detailErrorMessage) {
      Text(this.detailErrorMessage)
        .fontSize(12)
        .fontColor('#8D2F2F')
    }

    // Overview: summary + keywords + chapters merged
    if (this.selectedArtifacts) {
      if (this.selectedArtifacts.smartSummary.length > 0) {
        Text(this.selectedArtifacts.smartSummary)
          .fontSize(13)
          .lineHeight(20)
      }

      if (this.currentItem()!.keywords.length > 0) {
        Text(`Keywords: ${this.currentItem()!.keywords.join(' · ')}`)
          .fontSize(13)
          .lineHeight(19)
      }

      if (this.selectedArtifacts.chapters.length > 0) {
        Column({ space: 4 }) {
          ForEach(this.selectedArtifacts.chapters, (chapter: string) => {
            Text(`· ${chapter}`)
              .fontSize(12)
              .lineHeight(18)
              .opacity(0.8)
          })
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')
      }
    }

    // Accordion sections
    if (this.selectedArtifacts && this.selectedArtifacts.transcriptMarkdown.length > 0) {
      this.renderAccordionHeader('转写', 'transcript')
      if (this.expandedSection === 'transcript') {
        Text(this.selectedArtifacts.transcriptMarkdown)
          .fontSize(13)
          .lineHeight(20)
      }
    }

    if (this.selectedArtifacts && this.selectedArtifacts.speakerSummaries.length > 0) {
      this.renderAccordionHeader('发言人总结', 'speakers')
      if (this.expandedSection === 'speakers') {
        Column({ space: 12 }) {
          ForEach(this.selectedArtifacts!.speakerSummaries, (speaker: SpeakerSummary) => {
            Column({ space: 4 }) {
              if (speaker.name) {
                Text(speaker.name)
                  .fontSize(14)
                  .fontWeight(FontWeight.Medium)
              }
              Text(speaker.summary)
                .fontSize(13)
                .lineHeight(20)
            }
            .alignItems(HorizontalAlign.Start)
            .width('100%')
          })
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')
      }
    }

    if (this.selectedArtifacts && this.selectedArtifacts.questionAnswers.length > 0) {
      this.renderAccordionHeader('问答回顾', 'qa')
      if (this.expandedSection === 'qa') {
        Column({ space: 12 }) {
          ForEach(this.selectedArtifacts!.questionAnswers, (qa: QuestionAnswer) => {
            Column({ space: 4 }) {
              Text(`Q: ${qa.question}`)
                .fontSize(13)
                .lineHeight(20)
                .fontWeight(FontWeight.Medium)
              Text(`A: ${qa.answer}`)
                .fontSize(13)
                .lineHeight(20)
            }
            .alignItems(HorizontalAlign.Start)
            .width('100%')
          })
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')
      }
    }
  }

  @Builder
  renderAccordionHeader(title: string, sectionKey: string) {
    Row() {
      Text(title)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .layoutWeight(1)
      Text(this.expandedSection === sectionKey ? '▲' : '▼')
        .fontSize(14)
        .opacity(0.6)
    }
    .width('100%')
    .padding({ top: 12, bottom: 12 })
    .border({ width: { bottom: 1 }, color: '#E0D5C5' })
    .onClick(() => {
      this.toggleSection(sectionKey);
    })
  }

}
