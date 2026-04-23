// File: platforms/harmony/smartsales-app/entry/src/main/ets/pages/SchedulerPage.ets
// Module: smartsales-app/entry
// Summary: 日程管理页面 — Phase 2B 手动创建/列表/完成/删除
// Author: created on 2026-04-20

import { common } from '@kit.AbilityKit';
import {
  ScheduledTask,
  SchedulerSnapshot,
  UrgencyLevel,
  createEmptySchedulerSnapshot
} from '../domain/Scheduler';
import { schedulerRepository } from '../services/SchedulerRepository';

@Component
export struct SchedulerPage {
  @State snapshot: SchedulerSnapshot = createEmptySchedulerSnapshot();
  @State titleInput: string = '';
  @State datetimeInput: string = '';
  @State durationInput: string = '60';
  @State notesInput: string = '';
  @State selectedUrgency: UrgencyLevel = UrgencyLevel.L2_IMPORTANT;
  @State rescheduleTaskId: string = '';
  @State rescheduleDatetimeInput: string = '';
  @State rescheduleDurationInput: string = '60';
  @State rescheduleUrgency: UrgencyLevel = UrgencyLevel.L2_IMPORTANT;
  @State formErrorMessage: string = '';
  @State formSuccessMessage: string = '';
  private unsubscribe?: () => void;

  aboutToAppear(): void {
    const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
    schedulerRepository.initialize(context)
      .catch((error: Error) => {
        this.formErrorMessage = error.message;
      });
    this.unsubscribe = schedulerRepository.subscribe((next: SchedulerSnapshot) => {
      this.snapshot = next;
    });
  }

  aboutToDisappear(): void {
    this.unsubscribe?.();
    this.unsubscribe = undefined;
  }

  private async handleCreate(): Promise<void> {
    this.formErrorMessage = '';
    this.formSuccessMessage = '';
    const title = this.titleInput.trim();
    if (title.length === 0) {
      this.formErrorMessage = 'Title is required.';
      return;
    }
    const parsedStartTime = this.parseDateTime(this.datetimeInput);
    if (!parsedStartTime) {
      this.formErrorMessage = 'Start time must use YYYY-MM-DD HH:mm.';
      return;
    }
    const durationMinutes = this.parseDurationMinutes(this.durationInput);
    if (durationMinutes === undefined) {
      this.formErrorMessage = 'Duration must be 0 or a positive integer.';
      return;
    }
    try {
      const task = await schedulerRepository.addTask({
        title,
        startTime: parsedStartTime,
        durationMinutes,
        urgencyLevel: this.selectedUrgency,
        notes: this.notesInput
      });
      this.titleInput = '';
      this.datetimeInput = '';
      this.durationInput = '60';
      this.notesInput = '';
      this.selectedUrgency = UrgencyLevel.L2_IMPORTANT;
      this.formSuccessMessage = `Task created: ${task.title}`;
    } catch (error) {
      this.formErrorMessage = error instanceof Error ? error.message : `${error}`;
    }
  }

  private applyHarnessPreset(): void {
    this.titleInput = 'HS-006 demo follow-up';
    this.datetimeInput = '2030-01-15 09:30';
    this.durationInput = '60';
    this.notesInput = 'Device harness preset to prove create, restore, complete, and delete.';
    this.selectedUrgency = UrgencyLevel.L2_IMPORTANT;
    this.formErrorMessage = '';
    this.formSuccessMessage = 'HS-006 preset loaded.';
    console.info('[HS-006][SchedulerPage] preset loaded future demo task');
  }

  private async applyConflictScenarioPreset(): Promise<void> {
    this.formErrorMessage = '';
    this.formSuccessMessage = '';
    try {
      await schedulerRepository.loadConflictScenarioPreset();
      this.formSuccessMessage = 'HS-006 Phase 2C conflict scenario loaded.';
    } catch (error) {
      this.formErrorMessage = error instanceof Error ? error.message : `${error}`;
    }
  }

  private async handleComplete(taskId: string): Promise<void> {
    this.formErrorMessage = '';
    this.formSuccessMessage = '';
    try {
      await schedulerRepository.completeTask(taskId);
      this.formSuccessMessage = 'Task marked complete.';
    } catch (error) {
      this.formErrorMessage = error instanceof Error ? error.message : `${error}`;
    }
  }

  private async handleDelete(taskId: string): Promise<void> {
    this.formErrorMessage = '';
    this.formSuccessMessage = '';
    try {
      await schedulerRepository.deleteTask(taskId);
      this.formSuccessMessage = 'Task deleted.';
    } catch (error) {
      this.formErrorMessage = error instanceof Error ? error.message : `${error}`;
    }
  }

  private openRescheduleEditor(task: ScheduledTask): void {
    this.rescheduleTaskId = task.id;
    this.rescheduleDatetimeInput = this.formatDateTimeInput(task.startTime);
    this.rescheduleDurationInput = `${task.durationMinutes}`;
    this.rescheduleUrgency = task.urgencyLevel;
    this.formErrorMessage = '';
    this.formSuccessMessage = '';
  }

  private closeRescheduleEditor(): void {
    this.rescheduleTaskId = '';
    this.rescheduleDatetimeInput = '';
    this.rescheduleDurationInput = '60';
    this.rescheduleUrgency = UrgencyLevel.L2_IMPORTANT;
  }

  private async handleReschedule(task: ScheduledTask): Promise<void> {
    this.formErrorMessage = '';
    this.formSuccessMessage = '';
    const parsedStartTime = this.parseDateTime(this.rescheduleDatetimeInput);
    if (!parsedStartTime) {
      this.formErrorMessage = 'Reschedule time must use YYYY-MM-DD HH:mm.';
      return;
    }
    const durationMinutes = this.parseDurationMinutes(this.rescheduleDurationInput);
    if (durationMinutes === undefined) {
      this.formErrorMessage = 'Reschedule duration must be 0 or a positive integer.';
      return;
    }
    try {
      const updated = await schedulerRepository.rescheduleTask(
        task.id,
        parsedStartTime,
        durationMinutes,
        this.rescheduleUrgency
      );
      this.formSuccessMessage = `Task rescheduled: ${updated.title}`;
      this.closeRescheduleEditor();
    } catch (error) {
      this.formErrorMessage = error instanceof Error ? error.message : `${error}`;
    }
  }

  private parseDateTime(value: string): number | undefined {
    const normalized = value.trim();
    const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})$/);
    if (!match) {
      return undefined;
    }
    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const hour = Number(match[4]);
    const minute = Number(match[5]);
    if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) {
      return undefined;
    }
    if (!Number.isFinite(hour) || !Number.isFinite(minute)) {
      return undefined;
    }
    const date = new Date(year, month - 1, day, hour, minute, 0, 0);
    if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
      return undefined;
    }
    if (date.getHours() !== hour || date.getMinutes() !== minute) {
      return undefined;
    }
    return date.getTime();
  }

  private parseDurationMinutes(value: string): number | undefined {
    const normalized = value.trim();
    if (!normalized.match(/^\d+$/)) {
      return undefined;
    }
    const duration = Number(normalized);
    if (!Number.isFinite(duration) || duration < 0) {
      return undefined;
    }
    return Math.floor(duration);
  }

  private formatDateTimeInput(epochMillis: number): string {
    const date = new Date(epochMillis);
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    const hour = `${date.getHours()}`.padStart(2, '0');
    const minute = `${date.getMinutes()}`.padStart(2, '0');
    return `${year}-${month}-${day} ${hour}:${minute}`;
  }

  private urgencyChipBackground(level: UrgencyLevel): string {
    return this.urgencyChipBackgroundFor(level, this.selectedUrgency);
  }

  private urgencyChipBackgroundFor(level: UrgencyLevel, selected: UrgencyLevel): string {
    if (selected === level) {
      return this.urgencyColor(level);
    }
    return '#F4F0E8';
  }

  private urgencyChipTextColor(level: UrgencyLevel): string {
    return this.urgencyChipTextColorFor(level, this.selectedUrgency);
  }

  private urgencyChipTextColorFor(level: UrgencyLevel, selected: UrgencyLevel): string {
    if (selected === level) {
      return '#FFFFFF';
    }
    return this.urgencyColor(level);
  }

  private urgencyColor(level: UrgencyLevel): string {
    switch (level) {
      case UrgencyLevel.L1_CRITICAL:
        return '#B64040';
      case UrgencyLevel.L2_IMPORTANT:
        return '#D9862A';
      case UrgencyLevel.L3_NORMAL:
        return '#2F6BAA';
      case UrgencyLevel.FIRE_OFF:
        return '#7A7A7A';
      default:
        return '#2F6BAA';
    }
  }

  private urgencyLabel(level: UrgencyLevel): string {
    switch (level) {
      case UrgencyLevel.L1_CRITICAL:
        return 'Critical';
      case UrgencyLevel.L2_IMPORTANT:
        return 'Important';
      case UrgencyLevel.L3_NORMAL:
        return 'Normal';
      case UrgencyLevel.FIRE_OFF:
        return 'Fire-Off';
      default:
        return 'Normal';
    }
  }

  private taskCardBackground(task: ScheduledTask): string {
    return task.isDone ? '#EEE7DC' : '#FFF9F0';
  }

  private taskTitleColor(task: ScheduledTask): string {
    return task.isDone ? '#807466' : '#201B16';
  }

  private statusText(task: ScheduledTask): string {
    return task.isDone ? 'Completed' : 'Scheduled';
  }

  private durationText(task: ScheduledTask): string {
    if (task.durationMinutes <= 0) {
      return 'No duration';
    }
    return `${task.durationMinutes} min`;
  }

  build() {
    Scroll() {
      Column({ space: 18 }) {
        this.renderHero()
        this.renderCreateCard()
        this.renderTaskList()
      }
      .padding({ left: 18, right: 18, top: 24, bottom: 36 })
      .width('100%')
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#EFE7DA')
  }

  @Builder
  renderHero() {
    Column({ space: 10 }) {
      Text('Scheduler')
        .fontSize(30)
        .fontWeight(FontWeight.Bold)

      Text('HS-006 manual exact-task flow: create, restore, complete, and delete tasks with local persistence.')
        .fontSize(15)
        .lineHeight(22)
        .opacity(0.74)

      Text('Phase 2C adds reschedule, conflict caution, and a deterministic overlap harness flow while reminder delivery remains honestly deferred to HS-011.')
        .fontSize(15)
        .lineHeight(22)
        .opacity(0.74)

      Text(`${this.snapshot.tasks.length} task${this.snapshot.tasks.length === 1 ? '' : 's'} saved locally`)
        .fontSize(13)
        .fontColor('#5D554C')
    }
    .padding(20)
    .borderRadius(26)
    .backgroundColor('#FFF9F0')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  renderCreateCard() {
    Column({ space: 12 }) {
      Text('Create task')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      TextInput({ placeholder: 'Title', text: this.titleInput })
        .placeholderColor('#9A8E80')
        .backgroundColor('#FFFFFF')
        .borderRadius(16)
        .padding({ left: 14, right: 14, top: 12, bottom: 12 })
        .onChange((value: string) => {
          this.titleInput = value;
        })

      TextInput({ placeholder: 'Start time (YYYY-MM-DD HH:mm)', text: this.datetimeInput })
        .placeholderColor('#9A8E80')
        .backgroundColor('#FFFFFF')
        .borderRadius(16)
        .padding({ left: 14, right: 14, top: 12, bottom: 12 })
        .onChange((value: string) => {
          this.datetimeInput = value;
        })

      TextInput({ placeholder: 'Duration minutes', text: this.durationInput })
        .placeholderColor('#9A8E80')
        .backgroundColor('#FFFFFF')
        .borderRadius(16)
        .padding({ left: 14, right: 14, top: 12, bottom: 12 })
        .onChange((value: string) => {
          this.durationInput = value;
        })

      TextInput({ placeholder: 'Notes (optional)', text: this.notesInput })
        .placeholderColor('#9A8E80')
        .backgroundColor('#FFFFFF')
        .borderRadius(16)
        .padding({ left: 14, right: 14, top: 12, bottom: 12 })
        .onChange((value: string) => {
          this.notesInput = value;
        })

      Text('Urgency')
        .fontSize(13)
        .fontWeight(FontWeight.Medium)
        .fontColor('#5D554C')

      Row({ space: 8 }) {
        this.renderUrgencyChip(UrgencyLevel.L1_CRITICAL)
        this.renderUrgencyChip(UrgencyLevel.L2_IMPORTANT)
        this.renderUrgencyChip(UrgencyLevel.L3_NORMAL)
        this.renderUrgencyChip(UrgencyLevel.FIRE_OFF)
      }
      .width('100%')

      Button('Load HS-006 Demo Values')
        .backgroundColor('#E6D8C5')
        .fontColor('#3D3328')
        .borderRadius(18)
        .padding({ left: 18, right: 18, top: 12, bottom: 12 })
        .onClick(() => {
          this.applyHarnessPreset();
        })

      Button('Load HS-006 Phase 2C Conflict Scenario')
        .backgroundColor('#E6D8C5')
        .fontColor('#3D3328')
        .borderRadius(18)
        .padding({ left: 18, right: 18, top: 12, bottom: 12 })
        .onClick(async () => {
          await this.applyConflictScenarioPreset();
        })

      Button('Create Task')
        .backgroundColor('#2F5D50')
        .fontColor('#FFFFFF')
        .borderRadius(18)
        .padding({ left: 18, right: 18, top: 12, bottom: 12 })
        .onClick(async () => {
          await this.handleCreate();
        })

      if (this.formErrorMessage) {
        Text(this.formErrorMessage)
          .fontSize(13)
          .fontColor('#A63B3B')
      }

      if (this.formSuccessMessage) {
        Text(this.formSuccessMessage)
          .fontSize(13)
          .fontColor('#2F6A4A')
      }

      if (this.snapshot.lastErrorMessage) {
        Text(this.snapshot.lastErrorMessage || '')
          .fontSize(13)
          .fontColor('#A63B3B')
      }
    }
    .padding(18)
    .borderRadius(22)
    .backgroundColor('#F6EBDD')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  renderUrgencyChip(level: UrgencyLevel) {
    Button(this.urgencyLabel(level))
      .backgroundColor(this.urgencyChipBackground(level))
      .fontColor(this.urgencyChipTextColor(level))
      .borderRadius(14)
      .padding({ left: 12, right: 12, top: 10, bottom: 10 })
      .onClick(() => {
        this.selectedUrgency = level;
      })
  }

  @Builder
  renderTaskList() {
    Column({ space: 12 }) {
      Text('Task list')
        .fontSize(20)
        .fontWeight(FontWeight.Medium)

      if (this.snapshot.isLoading) {
        Text('Loading tasks...')
          .fontSize(14)
          .opacity(0.72)
      } else if (this.snapshot.tasks.length === 0) {
        Text('No tasks yet. Create an exact task above to populate the scheduler list.')
          .fontSize(14)
          .lineHeight(21)
          .opacity(0.72)
      } else {
        ForEach(this.snapshot.tasks, (task: ScheduledTask) => {
          Row({ space: 0 }) {
            Column() {}
              .width(6)
              .height('100%')
              .backgroundColor(this.urgencyColor(task.urgencyLevel))

            Column({ space: 10 }) {
              Row() {
                Column({ space: 4 }) {
                  Text(task.title)
                    .fontSize(18)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(this.taskTitleColor(task))
                    .decoration({ type: task.isDone ? TextDecorationType.LineThrough : TextDecorationType.None })

                  Text(`${this.statusText(task)} · ${task.dateRange} ${task.timeDisplay}`)
                    .fontSize(12)
                    .fontColor('#6D645A')

                  Text(this.durationText(task))
                    .fontSize(12)
                    .fontColor('#6D645A')
                }
                .layoutWeight(1)
                .alignItems(HorizontalAlign.Start)

                Text(this.urgencyLabel(task.urgencyLevel))
                  .fontSize(12)
                  .fontColor(this.urgencyColor(task.urgencyLevel))
              }

              if (task.notes) {
                Text(task.notes)
                  .fontSize(13)
                  .lineHeight(19)
                  .fontColor('#5E554C')
              }

              if (task.hasConflict) {
                Row({ space: 8 }) {
                  Text('Caution')
                    .fontSize(12)
                    .fontWeight(FontWeight.Medium)
                    .fontColor('#9E4F00')

                  Text(task.conflictSummary || 'Conflict detected')
                    .fontSize(12)
                    .fontColor('#9E4F00')
                    .layoutWeight(1)
                }
                .width('100%')
                .padding({ left: 12, right: 12, top: 10, bottom: 10 })
                .backgroundColor('#FFF1D8')
                .borderRadius(14)
              }

              Row({ space: 10 }) {
                if (!task.isDone) {
                  Button(this.rescheduleTaskId === task.id ? 'Editing' : 'Reschedule')
                    .backgroundColor('#D7E4F2')
                    .fontColor('#214A72')
                    .borderRadius(14)
                    .padding({ left: 14, right: 14, top: 10, bottom: 10 })
                    .onClick(() => {
                      this.openRescheduleEditor(task);
                    })

                  Button('Complete')
                    .backgroundColor('#2F6A4A')
                    .fontColor('#FFFFFF')
                    .borderRadius(14)
                    .padding({ left: 14, right: 14, top: 10, bottom: 10 })
                    .onClick(async () => {
                      await this.handleComplete(task.id);
                    })
                }

                Button('Delete')
                  .backgroundColor('#A63B3B')
                  .fontColor('#FFFFFF')
                  .borderRadius(14)
                  .padding({ left: 14, right: 14, top: 10, bottom: 10 })
                  .onClick(async () => {
                    await this.handleDelete(task.id);
                  })
              }

              if (this.rescheduleTaskId === task.id) {
                Column({ space: 10 }) {
                  Text('Reschedule task')
                    .fontSize(14)
                    .fontWeight(FontWeight.Medium)
                    .fontColor('#214A72')

                  TextInput({ placeholder: 'New time (YYYY-MM-DD HH:mm)', text: this.rescheduleDatetimeInput })
                    .placeholderColor('#8AA0B4')
                    .backgroundColor('#FFFFFF')
                    .borderRadius(14)
                    .padding({ left: 14, right: 14, top: 12, bottom: 12 })
                    .onChange((value: string) => {
                      this.rescheduleDatetimeInput = value;
                    })

                  TextInput({ placeholder: 'New duration minutes', text: this.rescheduleDurationInput })
                    .placeholderColor('#8AA0B4')
                    .backgroundColor('#FFFFFF')
                    .borderRadius(14)
                    .padding({ left: 14, right: 14, top: 12, bottom: 12 })
                    .onChange((value: string) => {
                      this.rescheduleDurationInput = value;
                    })

                  Row({ space: 8 }) {
                    this.renderInlineUrgencyChip(UrgencyLevel.L1_CRITICAL, this.rescheduleUrgency, (level: UrgencyLevel) => {
                      this.rescheduleUrgency = level;
                    })
                    this.renderInlineUrgencyChip(UrgencyLevel.L2_IMPORTANT, this.rescheduleUrgency, (level: UrgencyLevel) => {
                      this.rescheduleUrgency = level;
                    })
                    this.renderInlineUrgencyChip(UrgencyLevel.L3_NORMAL, this.rescheduleUrgency, (level: UrgencyLevel) => {
                      this.rescheduleUrgency = level;
                    })
                    this.renderInlineUrgencyChip(UrgencyLevel.FIRE_OFF, this.rescheduleUrgency, (level: UrgencyLevel) => {
                      this.rescheduleUrgency = level;
                    })
                  }
                  .width('100%')

                  Row({ space: 10 }) {
                    Button('Save Reschedule')
                      .backgroundColor('#214A72')
                      .fontColor('#FFFFFF')
                      .borderRadius(14)
                      .padding({ left: 14, right: 14, top: 10, bottom: 10 })
                      .onClick(async () => {
                        await this.handleReschedule(task);
                      })

                    Button('Cancel')
                      .backgroundColor('#EEE7DC')
                      .fontColor('#5E554C')
                      .borderRadius(14)
                      .padding({ left: 14, right: 14, top: 10, bottom: 10 })
                      .onClick(() => {
                        this.closeRescheduleEditor();
                      })
                  }
                }
                .width('100%')
                .padding(14)
                .backgroundColor('#EDF4FA')
                .borderRadius(16)
                .alignItems(HorizontalAlign.Start)
              }
            }
            .padding(16)
            .layoutWeight(1)
            .alignItems(HorizontalAlign.Start)
          }
          .width('100%')
          .backgroundColor(this.taskCardBackground(task))
          .borderRadius(20)
        }, (task: ScheduledTask) => `${task.id}_${task.updatedAt}_${task.isDone ? 'done' : 'todo'}`)
      }
    }
    .padding(18)
    .borderRadius(22)
    .backgroundColor('#FFF9F0')
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  renderInlineUrgencyChip(level: UrgencyLevel, selected: UrgencyLevel, onSelect: (level: UrgencyLevel) => void) {
    Button(this.urgencyLabel(level))
      .backgroundColor(this.urgencyChipBackgroundFor(level, selected))
      .fontColor(this.urgencyChipTextColorFor(level, selected))
      .borderRadius(14)
      .padding({ left: 12, right: 12, top: 10, bottom: 10 })
      .onClick(() => {
        onSelect(level);
      })
  }
}
