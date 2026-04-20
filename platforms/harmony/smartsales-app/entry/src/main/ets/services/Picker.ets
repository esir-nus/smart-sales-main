// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/Picker.ets
// Module: smartsales-app/entry
// Summary: 文件选择器服务 — 迁移自 tingwu-container HarmonyPickerService.ets
// Author: created on 2026-04-16

import { picker } from '@kit.CoreFileKit';

class PickerService {
  async selectSingleAudioUri(): Promise<string | undefined> {
    const options = new picker.DocumentSelectOptions();
    const documentPicker = new picker.DocumentViewPicker();
    const result = await documentPicker.select(options);
    if (!result || result.length === 0) {
      return undefined;
    }
    return result[0];
  }
}

export const pickerService = new PickerService();
