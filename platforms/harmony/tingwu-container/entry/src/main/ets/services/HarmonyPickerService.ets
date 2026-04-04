import { picker } from '@kit.CoreFileKit';

class HarmonyPickerService {
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

export const harmonyPickerService = new HarmonyPickerService();
