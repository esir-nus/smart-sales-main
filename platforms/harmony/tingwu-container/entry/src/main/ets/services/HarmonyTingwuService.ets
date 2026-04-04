import { http } from '@kit.NetworkKit';
import {
  normalizeTingwuBaseUrl
} from '../config/HarmonyContainerConfig';
import { getHarmonyRuntimeConfig } from '../config/HarmonyContainerRuntimeConfig';
import {
  HarmonyChapter,
  HarmonyDiarizedSegment,
  HarmonyResultLink,
  HarmonySmartSummary,
  HarmonySpeakerSummary,
  HarmonyQuestionAnswer,
  HarmonyTingwuArtifacts
} from '../model/HarmonyTingwuModels';
import {
  arrayOfObjects,
  buildTranscriptMarkdown,
  cleanString,
  makeQuestionAnswers,
  makeSpeakerSummaries,
  safeObject,
  safeStringArray,
  summarizeArtifacts,
  transcriptPreview,
  uniqueStrings
} from '../utils/HarmonyFormatUtils';
import {
  buildNonce,
  buildRfc1123Date,
  hmacSha1Base64
} from '../utils/HarmonySignatureUtils';
import { harmonyHttpClient } from './HarmonyHttpClient';

export interface HarmonyTingwuSubmitResult {
  jobId: string;
  taskKey: string;
}

interface HarmonyTingwuStatusData {
  TaskId?: string;
  TaskKey?: string;
  TaskStatus?: string;
  TaskProgress?: number;
  ErrorCode?: string;
  ErrorMessage?: string;
  OutputMp3Path?: string;
  OutputMp4Path?: string;
  OutputThumbnailPath?: string;
  OutputSpectrumPath?: string;
  Result?: Record<string, string>;
}

interface HarmonyTingwuStatusEnvelope {
  Code?: string;
  Message?: string;
  Data?: HarmonyTingwuStatusData;
}

export interface HarmonyTingwuCompletedResult {
  artifacts: HarmonyTingwuArtifacts;
  summary: string;
  transcriptPreview: string;
}

class HarmonyTingwuService {
  async submitTask(fileUrl: string, audioAssetName: string): Promise<HarmonyTingwuSubmitResult> {
    const harmonyRuntimeConfig = getHarmonyRuntimeConfig();
    const taskKey = `${audioAssetName}_${Date.now()}`.replace(/[^a-zA-Z0-9]/g, '_');
    const body = JSON.stringify({
      AppKey: harmonyRuntimeConfig.TINGWU_APP_KEY.trim(),
      Input: {
        SourceLanguage: 'cn',
        TaskKey: taskKey,
        FileUrl: fileUrl
      },
      Parameters: {
        Transcription: {
          DiarizationEnabled: true,
          Diarization: {
            OutputLevel: 1
          }
        },
        AutoChaptersEnabled: true,
        SummarizationEnabled: true,
        Summarization: {
          Types: ['Paragraph', 'Conversational', 'QuestionsAnswering']
        },
        MeetingAssistanceEnabled: true,
        MeetingAssistance: {
          Types: ['Actions', 'KeyInformation']
        },
        Transcoding: {
          TargetAudioFormat: 'mp3'
        }
      }
    });
    const response = await this.signedRequest('PUT', 'tasks?type=offline', body);
    const payload = this.parseJson(response.textBody || '{}') as Record<string, Object>;
    if (cleanString(payload.Code) && payload.Code !== '0') {
      throw new Error(`Tingwu submit failed: ${cleanString(payload.Message) || payload.Code}`);
    }
    const data = safeObject(payload.Data) || {};
    const jobId = cleanString(data.TaskId);
    if (!jobId) {
      throw new Error('Tingwu submit returned no TaskId');
    }
    return {
      jobId,
      taskKey
    };
  }

  async observeUntilComplete(
    jobId: string,
    onProgress: (progress: number, statusLabel: string) => void
  ): Promise<HarmonyTingwuCompletedResult> {
    while (true) {
      const status = await this.getTaskStatus(jobId);
      const statusLabel = status.Data?.TaskStatus || 'UNKNOWN';
      const progress = status.Data?.TaskProgress || 0;
      onProgress(progress, statusLabel);
      if (statusLabel === 'COMPLETED') {
        return this.fetchArtifacts(status.Data || {});
      }
      if (statusLabel === 'FAILED') {
        throw new Error(status.Data?.ErrorMessage || status.Data?.ErrorCode || 'Tingwu task failed');
      }
      await this.sleep(5000);
    }
  }

  private async getTaskStatus(jobId: string): Promise<HarmonyTingwuStatusEnvelope> {
    const response = await this.signedRequest('GET', `tasks/${encodeURIComponent(jobId)}?type=offline`);
    const payload = this.parseJson(response.textBody || '{}') as HarmonyTingwuStatusEnvelope;
    if (payload.Code && payload.Code !== '0' && payload.Code !== 'TaskNotFound') {
      throw new Error(`Tingwu status failed: ${payload.Message || payload.Code}`);
    }
    return payload;
  }

  private async fetchArtifacts(data: HarmonyTingwuStatusData): Promise<HarmonyTingwuCompletedResult> {
    const resultLinks = data.Result || {};
    const transcriptionUrl = resultLinks.Transcription;
    const autoChaptersUrl = resultLinks.AutoChapters;
    const summarizationUrl = resultLinks.Summarization;
    const meetingAssistanceUrl = resultLinks.MeetingAssistance;
    const identityRecognitionUrl = resultLinks.IdentityRecognition;

    const [transcriptionRaw, autoChaptersRaw, summarizationRaw, meetingAssistanceRaw, identityRecognitionRaw] =
      await Promise.all([
        this.fetchPublicBody(transcriptionUrl),
        this.fetchPublicBody(autoChaptersUrl),
        this.fetchPublicBody(summarizationUrl),
        this.fetchPublicBody(meetingAssistanceUrl),
        this.fetchPublicBody(identityRecognitionUrl)
      ]);

    const transcriptionRoot = this.extractTranscriptionRoot(transcriptionRaw);
    const diarizedSegments = this.parseDiarizedSegments(transcriptionRoot);
    const transcriptionSpeakerLabels = this.parseTranscriptionSpeakerLabels(transcriptionRoot);
    const identitySpeakerLabels = this.parseIdentitySpeakerLabels(identityRecognitionRaw);
    const speakerLabels = this.mergeSpeakerLabels(
      transcriptionSpeakerLabels,
      diarizedSegments,
      identitySpeakerLabels
    );
    const transcriptMarkdown = buildTranscriptMarkdown(
      diarizedSegments,
      speakerLabels,
      cleanString(transcriptionRoot.Text)
    );
    const chapters = this.parseChapters(autoChaptersRaw);
    const smartSummary = this.parseSmartSummary(summarizationRaw);
    const keywords = this.parseKeywords(meetingAssistanceRaw);
    const links = this.makeResultLinks(resultLinks);

    const artifacts: HarmonyTingwuArtifacts = {
      outputMp3Path: cleanString(data.OutputMp3Path),
      outputMp4Path: cleanString(data.OutputMp4Path),
      outputThumbnailPath: cleanString(data.OutputThumbnailPath),
      outputSpectrumPath: cleanString(data.OutputSpectrumPath),
      resultLinks: links,
      transcriptionUrl,
      autoChaptersUrl,
      extraResultUrls: this.extractExtraResultUrls(resultLinks),
      meetingAssistanceRaw,
      transcriptMarkdown,
      chapters,
      smartSummary,
      diarizedSegments,
      speakerLabels,
      keywords,
      providerPayloads: {
        transcription: transcriptionRaw || '',
        autoChapters: autoChaptersRaw || '',
        summarization: summarizationRaw || '',
        meetingAssistance: meetingAssistanceRaw || '',
        identityRecognition: identityRecognitionRaw || ''
      }
    };

    return {
      artifacts,
      summary: summarizeArtifacts(smartSummary, transcriptMarkdown),
      transcriptPreview: transcriptPreview(transcriptMarkdown)
    };
  }

  private async fetchPublicBody(url?: string): Promise<string | undefined> {
    if (!url || url.trim().length === 0) {
      return undefined;
    }
    const response = await harmonyHttpClient.requestText(url, http.RequestMethod.GET, {
      Accept: 'application/json'
    });
    if (response.statusCode < 200 || response.statusCode >= 300) {
      return undefined;
    }
    return response.textBody;
  }

  private async signedRequest(
    method: 'GET' | 'PUT',
    resource: string,
    body?: string
  ) {
    const harmonyRuntimeConfig = getHarmonyRuntimeConfig();
    const baseUrl = normalizeTingwuBaseUrl(harmonyRuntimeConfig.TINGWU_BASE_URL);
    const accessKeyId = harmonyRuntimeConfig.TINGWU_ACCESS_KEY_ID.trim();
    const accessKeySecret = harmonyRuntimeConfig.TINGWU_ACCESS_KEY_SECRET.trim();
    const appKey = harmonyRuntimeConfig.TINGWU_APP_KEY.trim();
    if (!accessKeyId || !accessKeySecret || !appKey) {
      throw new Error('Tingwu config is incomplete in HarmonyContainerConfig.ets');
    }

    const date = buildRfc1123Date();
    const nonce = buildNonce();
    const contentType = 'application/json; charset=UTF-8';
    const headers: Record<string, string> = {
      Accept: 'application/json',
      'Content-Type': contentType,
      Date: date,
      'x-acs-date': date,
      'x-acs-access-key-id': accessKeyId,
      'x-acs-signature-method': 'HMAC-SHA1',
      'x-acs-signature-version': '1.0',
      'x-acs-signature-nonce': nonce,
      'x-tingwu-app-key': appKey
    };
    if (harmonyRuntimeConfig.TINGWU_SECURITY_TOKEN.trim().length > 0) {
      headers['x-acs-security-token'] = harmonyRuntimeConfig.TINGWU_SECURITY_TOKEN.trim();
    }

    const stringToSign = this.buildStringToSign(method, headers, contentType, date, resource);
    const signature = hmacSha1Base64(accessKeySecret, stringToSign);
    headers.Authorization = `acs ${accessKeyId}:${signature}`;

    const response = await harmonyHttpClient.requestText(
      `${baseUrl}${resource}`,
      method === 'GET' ? http.RequestMethod.GET : http.RequestMethod.PUT,
      headers,
      body
    );

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw new Error(`Tingwu HTTP ${response.statusCode}: ${response.textBody || ''}`.trim());
    }
    return response;
  }

  private buildStringToSign(
    method: string,
    headers: Record<string, string>,
    contentType: string,
    date: string,
    resource: string
  ): string {
    const canonicalHeaderLines = Object.keys(headers)
      .filter((key: string) => key.toLowerCase().startsWith('x-acs-'))
      .sort()
      .map((key: string) => `${key.toLowerCase()}:${headers[key].trim()}`)
      .join('\n');
    return `${method}\napplication/json\n\n${contentType}\n${date}\n${canonicalHeaderLines}\n/${resource}`;
  }

  private parseJson(raw: string): Object {
    try {
      return JSON.parse(raw) as Object;
    } catch (error) {
      return {};
    }
  }

  private extractTranscriptionRoot(raw?: string): Record<string, Object> {
    const payload = safeObject(this.parseJson(raw || '{}')) || {};
    return safeObject(payload.Transcription) || payload;
  }

  private parseDiarizedSegments(root: Record<string, Object>): HarmonyDiarizedSegment[] {
    return arrayOfObjects(root.Segments).map((segment: Record<string, Object>, index: number) => ({
      speakerId: cleanString(segment.Speaker) || cleanString(segment.SpeakerId),
      speakerIndex: Number(segment.Id || index),
      startMs: Math.floor(Number(segment.Start || 0) * 1000),
      endMs: Math.floor(Number(segment.End || 0) * 1000),
      text: cleanString(segment.Text) || ''
    })).filter((segment: HarmonyDiarizedSegment) => segment.text.length > 0);
  }

  private parseTranscriptionSpeakerLabels(root: Record<string, Object>): Record<string, string> {
    const labels: Record<string, string> = {};
    arrayOfObjects(root.Speakers).forEach((speaker: Record<string, Object>) => {
      const speakerId = cleanString(speaker.Id) || cleanString(speaker.SpeakerId);
      const label = cleanString(speaker.Name) || cleanString(speaker.SpeakerName);
      if (speakerId && label) {
        labels[speakerId] = label;
      }
    });
    return labels;
  }

  private parseIdentitySpeakerLabels(raw?: string): Record<string, string> {
    const payload = this.parseJson(raw || '{}');
    const labels: Record<string, string> = {};
    const root = safeObject(payload);
    const items = arrayOfObjects(root?.IdentityRecognition)
      .concat(arrayOfObjects(safeObject(root?.Data)?.IdentityRecognition));
    items.forEach((item: Record<string, Object>) => {
      const speakerId = cleanString(item.SpeakerId) || cleanString(item.Speaker);
      const identity = safeObject(item.Identity);
      const label = cleanString(identity?.Name) || cleanString(item.IdentityName) || cleanString(item.Name);
      if (speakerId && label) {
        labels[speakerId] = label;
      }
    });
    return labels;
  }

  private mergeSpeakerLabels(
    base: Record<string, string>,
    segments: HarmonyDiarizedSegment[],
    identityLabels: Record<string, string>
  ): Record<string, string> {
    const merged: Record<string, string> = {};
    Object.keys(base).forEach((key: string) => {
      merged[key] = base[key];
    });
    segments.forEach((segment: HarmonyDiarizedSegment) => {
      const key = segment.speakerId;
      if (!key) {
        return;
      }
      if (!merged[key]) {
        merged[key] = key;
      }
    });
    Object.keys(identityLabels).forEach((key: string) => {
      merged[key] = identityLabels[key];
    });
    return merged;
  }

  private parseChapters(raw?: string): HarmonyChapter[] {
    const payload = safeObject(this.parseJson(raw || '{}')) || {};
    const items = arrayOfObjects(payload.AutoChapters)
      .concat(arrayOfObjects(payload.Chapters))
      .concat(arrayOfObjects(safeObject(payload.Data)?.AutoChapters))
      .concat(arrayOfObjects(safeObject(payload.Data)?.Chapters));
    return items.map((item: Record<string, Object>, index: number) => ({
      title: cleanString(item.Title) || cleanString(item.Head) || `Chapter ${index + 1}`,
      startMs: Math.floor(Number(item.Start || item.StartTime || 0) * 1000),
      endMs: Math.floor(Number(item.End || item.EndTime || 0) * 1000),
      summary: cleanString(item.Summary) || cleanString(item.Text)
    }));
  }

  private parseSmartSummary(raw?: string): HarmonySmartSummary | undefined {
    const payload = safeObject(this.parseJson(raw || '{}')) || {};
    const summaryRoot = safeObject(payload.Summarization)
      || safeObject(payload.Data)
      || payload;
    const paragraphSummary = cleanString(summaryRoot.ParagraphSummary)
      || cleanString(summaryRoot.Paragraph)
      || cleanString(summaryRoot.Summary)
      || cleanString(summaryRoot.Conversational);
    const keyPoints = uniqueStrings(
      safeStringArray(summaryRoot.KeyPoints)
        .concat(safeStringArray(summaryRoot.KeySentences))
        .concat(safeStringArray(summaryRoot.BulletPoints))
    );
    const speakerSummaries: HarmonySpeakerSummary[] = makeSpeakerSummaries(
      arrayOfObjects(summaryRoot.SpeakerSummaries).concat(arrayOfObjects(summaryRoot.Speakers))
    );
    const questionAnswers: HarmonyQuestionAnswer[] = makeQuestionAnswers(
      arrayOfObjects(summaryRoot.QuestionsAnswering).concat(arrayOfObjects(summaryRoot.Questions))
    );

    if (!paragraphSummary && keyPoints.length === 0 && speakerSummaries.length === 0 && questionAnswers.length === 0) {
      return undefined;
    }
    return {
      summary: paragraphSummary,
      keyPoints,
      speakerSummaries,
      questionAnswers
    };
  }

  private parseKeywords(raw?: string): string[] {
    const payload = safeObject(this.parseJson(raw || '{}')) || {};
    const meetingRoot = safeObject(payload.MeetingAssistance) || payload;
    return uniqueStrings(safeStringArray(meetingRoot.Keywords));
  }

  private makeResultLinks(resultLinks: Record<string, string>): HarmonyResultLink[] {
    return Object.keys(resultLinks).map((label: string) => ({
      label,
      url: resultLinks[label]
    }));
  }

  private extractExtraResultUrls(resultLinks: Record<string, string>): Record<string, string> {
    const extras: Record<string, string> = {};
    Object.keys(resultLinks).forEach((label: string) => {
      if (label !== 'Transcription' && label !== 'AutoChapters') {
        extras[label] = resultLinks[label];
      }
    });
    return extras;
  }

  private sleep(durationMs: number): Promise<void> {
    return new Promise((resolve: () => void) => {
      setTimeout(() => resolve(), durationMs);
    });
  }
}

export const harmonyTingwuService = new HarmonyTingwuService();
