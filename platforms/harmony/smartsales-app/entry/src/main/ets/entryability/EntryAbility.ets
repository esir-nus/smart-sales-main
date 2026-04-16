// File: platforms/harmony/smartsales-app/entry/src/main/ets/entryability/EntryAbility.ets
// Module: smartsales-app/entry
// Summary: 应用入口 Ability，加载主导航页面
// Author: created on 2026-04-16

import { UIAbility } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';

export default class EntryAbility extends UIAbility {
  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index');
  }
}
