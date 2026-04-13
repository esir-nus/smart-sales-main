import { common } from '@kit.AbilityKit';
import {
  createEmptyHarmonySchedulerSnapshot,
  HarmonySchedulerPathACommitRecord,
  HarmonySchedulerScenarioKind,
  HarmonySchedulerSnapshot,
  HarmonySchedulerTaskRecord,
  HarmonySchedulerTraceEvent
} from '../model/HarmonySchedulerModels';
import {
  HarmonyChapter,
  HarmonyContainerSnapshot,
  HarmonyJobStatus,
  HarmonyResultLink,
  HarmonyTingwuArtifacts,
  HarmonyTingwuAudioItem,
  createEmptySnapshot
} from '../model/HarmonyTingwuModels';
import { harmonyContainerRepository } from '../services/HarmonyContainerRepository';
import { harmonySchedulerIngressCoordinator } from '../services/HarmonySchedulerIngressCoordinator';
import { harmonySchedulerRepository } from '../services/HarmonySchedulerRepository';
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
  @State schedulerSnapshot: HarmonySchedulerSnapshot = createEmptyHarmonySchedulerSnapshot();
  @State selectedAudioId: string = '';
  @State selectedArtifacts?: HarmonyTingwuArtifacts = undefined;
  @State detailErrorMessage: string = '';
  @State schedulerAdvancedVisible: boolean = false;
  private unsubscribe?: () => void;
  private schedulerUnsubscribe?: () => void;

  private readonly supported: string[] = [
    'Local file import only',
    'Harmony-owned OSS upload and Tingwu polling',
    'Transcript, chapters, keywords, and summary reopen',
    'Harmony namespaced metadata and artifact files',
    'Operator-only scheduler Path A mini lab for Uni-A exact create and Uni-B vague create',
    'Collapsed internal scaffold verification for conflict, reschedule, and null safe-fail backend proof'
  ];

  private readonly hidden: string[] = [
    'User-facing scheduler create, reschedule, reminder, follow-up',
    'Scheduler onboarding handoff and discoverability teaching',
    'Conflict, reschedule, and null-fail verification cases stay internal scaffold only',
    'Ask-AI, audio-grounded chat, session binding',
    'Badge pairing, sync, download, and hardware flows'
  ];

  aboutToAppear(): void {
    const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
    harmonyContainerRepository.initialize(context)
      .catch((error: Error) => {
        this.setContainerLoadError(error.message);
      });
    harmonySchedulerRepository.initialize(context)
      .catch((error: Error) => {
        this.setSchedulerError(error.message, true);
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
    this.schedulerUnsubscribe = harmonySchedulerRepository.subscribe((next: HarmonySchedulerSnapshot) => {
      this.schedulerSnapshot = next;
    });
  }

  aboutToDisappear(): void {
    this.unsubscribe?.();
    this.unsubscribe = undefined;
    this.schedulerUnsubscribe?.();
    this.schedulerUnsubscribe = undefined;
  }

  private currentItem(): HarmonyTingwuAudioItem | undefined {
    return this.snapshot.items.find((item: HarmonyTingwuAudioItem) => item.id === this.selectedAudioId);
  }

  private schedulerTasks(): HarmonySchedulerTaskRecord[] {
    return this.schedulerSnapshot.tasks.slice(0, 6);
  }

  private schedulerEvents(): HarmonySchedulerTraceEvent[] {
    return this.schedulerSnapshot.events.slice(0, 12);
  }

  private schedulerScenarioLabel(scenario?: HarmonySchedulerScenarioKind): string {
    switch (scenario) {
      case HarmonySchedulerScenarioKind.EXACT_CREATE:
        return 'Uni-A exact create';
      case HarmonySchedulerScenarioKind.VAGUE_CREATE:
        return 'Uni-B vague create';
      case HarmonySchedulerScenarioKind.CONFLICT_CREATE:
        return 'Conflict scaffold only';
      case HarmonySchedulerScenarioKind.RESCHEDULE:
        return 'Reschedule scaffold only';
      case HarmonySchedulerScenarioKind.NULL_FAIL:
        return 'Null safe-fail scaffold only';
      default:
        return 'none';
    }
  }

  private schedulerDueLabel(task: HarmonySchedulerTaskRecord): string {
    return task.dueAtIso || 'needs time';
  }

  private schedulerOwnerChainLabel(): string {
    if (this.schedulerSnapshot.ownerChain.length === 0) {
      return 'none';
    }
    return this.schedulerSnapshot.ownerChain.join(' -> ');
  }

  private hasActiveExactTask(): boolean {
    return this.schedulerSnapshot.tasks.some((task: HarmonySchedulerTaskRecord) =>
      task.state === 'ACTIVE' && task.commandKind === 'EXACT_CREATE'
    );
  }

  private schedulerMetadataSummary(event: HarmonySchedulerTraceEvent): string {
    const payload = JSON.stringify(event.metadata);
    return payload && payload !== '{}' ? payload : 'no metadata';
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

  private async runSchedulerScenario(scenario: HarmonySchedulerScenarioKind): Promise<void> {
    try {
      await harmonySchedulerIngressCoordinator.runOperatorScenario(scenario);
    } catch (error) {
      this.setSchedulerError(this.errorMessage(error), false);
    }
  }

  private async resetSchedulerSandbox(): Promise<void> {
    try {
      await harmonySchedulerRepository.resetSandbox();
    } catch (error) {
      this.setSchedulerError(this.errorMessage(error), false);
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
        this.renderSchedulerBackendPanel()
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

      Text('Transient Harmony-native lane for file import, OSS upload, Tingwu processing, persisted artifact reopen, and one operator-only scheduler Path A mini lab.')
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
          Column({ space: 10 }) {
            Row() {
              Column({ space: 4 }) {
                Text(item.displayName)
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(this.inventoryPrimaryTextColor(item.id))
                Text(`${statusLabel(item.status)} · ${formatBytes(item.sizeBytes)} · ${formatDateTime(item.importedAt)}`)
                  .fontSize(12)
                  .opacity(0.66)
                  .fontColor(this.inventorySecondaryTextColor(item.id))
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)

              Text(`${item.progressPercent}%`)
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor(this.inventoryProgressTextColor(item.id))
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
                .fontColor(this.inventoryPrimaryTextColor(item.id))
            }

            if (item.lastErrorMessage) {
              Text(item.lastErrorMessage)
                .fontSize(12)
                .fontColor('#8D2F2F')
            }
          }
          .padding(16)
          .borderRadius(20)
          .backgroundColor(this.inventoryCardBackground(item.id))
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
    Column({ space: 12 }) {
      Text('Artifact detail')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      if (!this.currentItem()) {
        Text('Select an imported Harmony audio item to review its Tingwu-only state, then process or reopen artifacts.')
          .fontSize(14)
          .lineHeight(21)
          .opacity(0.72)
      } else {
        Text(this.currentItem()?.displayName || '')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)

        Text(`Local file: ${this.currentItem()?.localFileName || ''}`)
          .fontSize(13)
          .opacity(0.7)

        Row({ space: 12 }) {
          Button(this.currentItem()?.status === HarmonyJobStatus.COMPLETED ? 'Re-run Tingwu' : 'Process with Tingwu')
            .enabled(this.currentItem()?.status !== HarmonyJobStatus.UPLOADING && this.currentItem()?.status !== HarmonyJobStatus.PROCESSING)
            .backgroundColor('#8C5A34')
            .fontColor('#FFF8ED')
            .borderRadius(16)
            .onClick(async () => {
              await this.handleProcess(this.currentItem()?.id || '');
            })

          Text(statusLabel(this.currentItem()?.status || HarmonyJobStatus.IMPORTED))
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
        }

        if (this.detailErrorMessage) {
          Text(this.detailErrorMessage)
            .fontSize(12)
            .fontColor('#8D2F2F')
        }

        if ((this.currentItem()?.keywords.length || 0) > 0) {
          Text(`Keywords: ${this.currentItem()?.keywords.join(' · ') || ''}`)
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
            ForEach(this.selectedArtifacts?.chapters || [], (chapter: HarmonyChapter) => {
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
            ForEach(this.selectedArtifacts?.resultLinks || [], (link: HarmonyResultLink) => {
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
  private renderSchedulerBackendPanel() {
    Column({ space: 12 }) {
      Text('Scheduler Path A mini lab')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      Text('Operator-only Harmony experiment for Uni-A exact create and Uni-B vague create. It proves classification, owner chain, Path A commit visibility, and local snapshot persistence without pretending scheduler parity.')
        .fontSize(14)
        .lineHeight(21)
        .opacity(0.78)

      Row({ space: 8 }) {
        this.renderSchedulerActionButton('Run Uni-A exact', HarmonySchedulerScenarioKind.EXACT_CREATE, '#355C7D')
        this.renderSchedulerActionButton('Run Uni-B vague', HarmonySchedulerScenarioKind.VAGUE_CREATE, '#355C7D')
      }
      .width('100%')

      Row({ space: 8 }) {
        Button('Reset sandbox')
          .enabled(!this.schedulerSnapshot.isBusy)
          .layoutWeight(1)
          .backgroundColor('#E8DCCA')
          .fontColor('#2B241C')
          .borderRadius(16)
          .onClick(async () => {
            await this.resetSchedulerSandbox();
          })
      }
      .width('100%')

      Button(this.schedulerAdvancedVisible ? 'Hide internal scaffold verification' : 'Show internal scaffold verification')
        .enabled(!this.schedulerSnapshot.isBusy)
        .width('100%')
        .backgroundColor('#D5DDE8')
        .fontColor('#243447')
        .borderRadius(16)
        .onClick(() => {
          this.schedulerAdvancedVisible = !this.schedulerAdvancedVisible;
        })

      if (this.schedulerAdvancedVisible) {
        this.renderSchedulerAdvancedPanel()
      }

      if (this.schedulerSnapshot.isBusy) {
        Text(this.schedulerSnapshot.busyMessage)
          .fontSize(12)
          .fontColor('#355C7D')
      }

      if (this.schedulerSnapshot.lastErrorMessage) {
        Text(this.schedulerSnapshot.lastErrorMessage)
          .fontSize(12)
          .fontColor('#8D2F2F')
      }

      Column({ space: 6 }) {
        Text('Mini-lab summary')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
        Text(`Last scenario: ${this.schedulerScenarioLabel(this.schedulerSnapshot.lastScenario)}`)
          .fontSize(13)
        Text(`Last command: ${this.schedulerSnapshot.lastCommandKind}`)
          .fontSize(13)
        Text(`Ingress owner: ${this.schedulerSnapshot.lastIngressOwner || 'none'}`)
          .fontSize(13)
        Text(`Owner chain: ${this.schedulerOwnerChainLabel()}`)
          .fontSize(13)
          .lineHeight(19)
        Text(`Last input: ${this.schedulerSnapshot.lastInput || '(empty input)'}`)
          .fontSize(13)
          .lineHeight(19)
        if (this.schedulerSnapshot.lastSafeFailReason) {
          Text(`Safe fail: ${this.schedulerSnapshot.lastSafeFailReason}`)
            .fontSize(13)
            .fontColor('#8D2F2F')
        }
      }
      .alignItems(HorizontalAlign.Start)
      .width('100%')
      .padding(14)
      .borderRadius(18)
      .backgroundColor('#FFF8ED')

      this.renderSchedulerCommitPanel(this.schedulerSnapshot.lastPathACommit)

      Column({ space: 8 }) {
        Text('Persisted task snapshot')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
        if (this.schedulerTasks().length === 0) {
          Text('No local Harmony scheduler tasks yet. Run Uni-A or Uni-B to inspect the stored snapshot.')
            .fontSize(13)
            .opacity(0.7)
        } else {
          ForEach(this.schedulerTasks(), (task: HarmonySchedulerTaskRecord) => {
            Column({ space: 4 }) {
              Text(task.title)
                .fontSize(14)
                .fontWeight(FontWeight.Medium)
              Text(`lineage=${task.lineageGuid}`)
                .fontSize(12)
                .opacity(0.72)
              Text(`state=${task.state} · timing=${task.timingKind} · due=${this.schedulerDueLabel(task)}`)
                .fontSize(12)
                .lineHeight(18)
              Text(`reminder=${task.reminderState} · pathB=${task.pathBState} · revision=${task.revisionNo}`)
                .fontSize(12)
                .lineHeight(18)
              Text(`source=${task.sourceInput}`)
                .fontSize(12)
                .lineHeight(18)
              if (task.conflictDetected) {
                Text('conflict-visible marker present')
                  .fontSize(12)
                  .fontColor('#8D2F2F')
              }
            }
            .padding(12)
            .borderRadius(16)
            .backgroundColor('#F6F0E4')
            .width('100%')
            .alignItems(HorizontalAlign.Start)
          })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .width('100%')
      .padding(14)
      .borderRadius(18)
      .backgroundColor('#FFF8ED')

      Column({ space: 8 }) {
        Text('Recent telemetry')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
        if (this.schedulerEvents().length === 0) {
          Text('Run a scheduler mini-lab action to inspect trace joints.')
            .fontSize(13)
            .opacity(0.7)
        } else {
          ForEach(this.schedulerEvents(), (event: HarmonySchedulerTraceEvent) => {
            Column({ space: 2 }) {
              Text(`${event.stage} · ${event.message}`)
                .fontSize(13)
                .lineHeight(20)
              Text(`${formatDateTime(event.occurredAt)} · ${this.schedulerMetadataSummary(event)}`)
                .fontSize(11)
                .lineHeight(16)
                .opacity(0.7)
            }
            .width('100%')
            .alignItems(HorizontalAlign.Start)
          })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .width('100%')
      .padding(14)
      .borderRadius(18)
      .backgroundColor('#FFF8ED')
    }
    .padding(18)
    .borderRadius(24)
    .backgroundColor('#E5E9F0')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  private renderSchedulerCommitPanel(commit?: HarmonySchedulerPathACommitRecord) {
    Column({ space: 8 }) {
      Text('Latest Path A commit')
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
      if (!commit) {
        Text('No Path A commit recorded yet.')
          .fontSize(13)
          .opacity(0.7)
      } else {
        Text(`rootGuid: ${commit.rootGuid}`)
          .fontSize(13)
          .lineHeight(19)
        Text(`lineageGuid: ${commit.lineageGuid}`)
          .fontSize(13)
          .lineHeight(19)
        Text(`command: ${commit.commandKind} · state: ${commit.taskState}`)
          .fontSize(13)
          .lineHeight(19)
        Text(`committed: ${formatDateTime(commit.committedAt)}`)
          .fontSize(13)
        Text(`owner chain: ${commit.ownerChain.join(' -> ')}`)
          .fontSize(13)
          .lineHeight(19)
      }
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
    .padding(14)
    .borderRadius(18)
    .backgroundColor('#FFF8ED')
  }

  @Builder
  private renderSchedulerAdvancedPanel() {
    Column({ space: 10 }) {
      Text('Internal scaffold verification only')
        .fontSize(16)
        .fontWeight(FontWeight.Medium)

      Text('Use this collapsed section to complete backend-first Harmony Path A proof. These actions are intentionally hidden from the default visible mini-lab and must not be presented as scheduler parity.')
        .fontSize(13)
        .lineHeight(19)
        .opacity(0.78)

      if (!this.hasActiveExactTask()) {
        Text('Reschedule requires at least one seeded active exact task. The operator scenario itself now carries an explicit target plus a new exact time.')
          .fontSize(12)
          .fontColor('#8D2F2F')
          .lineHeight(18)
      }

      Row({ space: 8 }) {
        this.renderSchedulerActionButton('Run conflict create', HarmonySchedulerScenarioKind.CONFLICT_CREATE, '#7D4E57')
        this.renderSchedulerActionButton('Run reschedule', HarmonySchedulerScenarioKind.RESCHEDULE, '#7D4E57')
      }
      .width('100%')

      Row({ space: 8 }) {
        this.renderSchedulerActionButton('Run null safe-fail', HarmonySchedulerScenarioKind.NULL_FAIL, '#7D4E57')
      }
      .width('100%')
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
    .padding(14)
    .borderRadius(18)
    .backgroundColor('#F0E6EA')
  }

  @Builder
  private renderSchedulerActionButton(label: string, scenario: HarmonySchedulerScenarioKind, color: string) {
    Button(label)
      .enabled(!this.schedulerSnapshot.isBusy)
      .layoutWeight(1)
      .backgroundColor(color)
      .fontColor('#FFF8ED')
      .borderRadius(16)
      .onClick(async () => {
        await this.runSchedulerScenario(scenario);
      })
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

  private setContainerLoadError(message: string): void {
    const nextSnapshot = this.copyContainerSnapshot(this.snapshot);
    nextSnapshot.isLoading = false;
    nextSnapshot.lastErrorMessage = message;
    this.snapshot = nextSnapshot;
  }

  private setSchedulerError(message: string, clearLoading: boolean): void {
    const nextSnapshot = this.copySchedulerSnapshot(this.schedulerSnapshot);
    if (clearLoading) {
      nextSnapshot.isLoading = false;
    }
    nextSnapshot.lastErrorMessage = message;
    this.schedulerSnapshot = nextSnapshot;
  }

  private errorMessage(error: Error | Object | string | number | boolean | undefined | null): string {
    if (error instanceof Error) {
      return error.message;
    }
    return `${error}`;
  }

  private inventoryCardBackground(audioId: string): string {
    return this.selectedAudioId === audioId ? '#2F5D50' : '#FFF8ED';
  }

  private inventoryPrimaryTextColor(audioId: string): string {
    return this.selectedAudioId === audioId ? '#FFF8ED' : '#2B241C';
  }

  private inventorySecondaryTextColor(audioId: string): string {
    return this.selectedAudioId === audioId ? '#F3E7D5' : '#5A5148';
  }

  private inventoryProgressTextColor(audioId: string): string {
    return this.selectedAudioId === audioId ? '#F8F2E9' : '#2F5D50';
  }

  private copyContainerSnapshot(snapshot: HarmonyContainerSnapshot): HarmonyContainerSnapshot {
    return {
      items: this.copyAudioItems(snapshot.items),
      configMissingKeys: this.copyStringArray(snapshot.configMissingKeys),
      isLoading: snapshot.isLoading,
      isBusy: snapshot.isBusy,
      busyMessage: snapshot.busyMessage,
      lastErrorMessage: snapshot.lastErrorMessage
    };
  }

  private copySchedulerSnapshot(snapshot: HarmonySchedulerSnapshot): HarmonySchedulerSnapshot {
    return {
      tasks: this.copySchedulerTasks(snapshot.tasks),
      events: this.copySchedulerEvents(snapshot.events),
      isLoading: snapshot.isLoading,
      isBusy: snapshot.isBusy,
      busyMessage: snapshot.busyMessage,
      lastInput: snapshot.lastInput,
      lastScenario: snapshot.lastScenario,
      lastIngressKind: snapshot.lastIngressKind,
      lastIngressOwner: snapshot.lastIngressOwner,
      ownerChain: this.copyStringArray(snapshot.ownerChain),
      lastCommandKind: snapshot.lastCommandKind,
      lastSafeFailReason: snapshot.lastSafeFailReason,
      lastPathACommit: snapshot.lastPathACommit ? this.copyPathACommit(snapshot.lastPathACommit) : undefined,
      lastErrorMessage: snapshot.lastErrorMessage
    };
  }

  private copyAudioItems(items: HarmonyTingwuAudioItem[]): HarmonyTingwuAudioItem[] {
    const nextItems: HarmonyTingwuAudioItem[] = [];
    items.forEach((item: HarmonyTingwuAudioItem) => {
      nextItems.push({
        id: item.id,
        displayName: item.displayName,
        sourceUri: item.sourceUri,
        localFileName: item.localFileName,
        localPath: item.localPath,
        extension: item.extension,
        sizeBytes: item.sizeBytes,
        importedAt: item.importedAt,
        updatedAt: item.updatedAt,
        progressPercent: item.progressPercent,
        status: item.status,
        objectKey: item.objectKey,
        publicUrl: item.publicUrl,
        jobId: item.jobId,
        summary: item.summary,
        transcriptPreview: item.transcriptPreview,
        lastErrorMessage: item.lastErrorMessage,
        completedAt: item.completedAt,
        keywords: this.copyStringArray(item.keywords)
      });
    });
    return nextItems;
  }

  private copySchedulerTasks(tasks: HarmonySchedulerTaskRecord[]): HarmonySchedulerTaskRecord[] {
    const nextTasks: HarmonySchedulerTaskRecord[] = [];
    tasks.forEach((task: HarmonySchedulerTaskRecord) => {
      nextTasks.push({
        rootGuid: task.rootGuid,
        revisionNo: task.revisionNo,
        lineageGuid: task.lineageGuid,
        title: task.title,
        sourceInput: task.sourceInput,
        commandKind: task.commandKind,
        timingKind: task.timingKind,
        urgencyLabel: task.urgencyLabel,
        state: task.state,
        reminderState: task.reminderState,
        pathBState: task.pathBState,
        dueAtIso: task.dueAtIso,
        conflictDetected: task.conflictDetected,
        createdAt: task.createdAt,
        updatedAt: task.updatedAt,
        supersededAt: task.supersededAt
      });
    });
    return nextTasks;
  }

  private copySchedulerEvents(events: HarmonySchedulerTraceEvent[]): HarmonySchedulerTraceEvent[] {
    const nextEvents: HarmonySchedulerTraceEvent[] = [];
    events.forEach((event: HarmonySchedulerTraceEvent) => {
      nextEvents.push({
        traceId: event.traceId,
        stage: event.stage,
        message: event.message,
        metadata: event.metadata,
        occurredAt: event.occurredAt
      });
    });
    return nextEvents;
  }

  private copyPathACommit(commit: HarmonySchedulerPathACommitRecord): HarmonySchedulerPathACommitRecord {
    return {
      traceId: commit.traceId,
      rootGuid: commit.rootGuid,
      lineageGuid: commit.lineageGuid,
      commandKind: commit.commandKind,
      taskState: commit.taskState,
      committedAt: commit.committedAt,
      ownerChain: this.copyStringArray(commit.ownerChain)
    };
  }

  private copyStringArray(values: string[]): string[] {
    const nextValues: string[] = [];
    values.forEach((value: string) => {
      nextValues.push(value);
    });
    return nextValues;
  }
}
