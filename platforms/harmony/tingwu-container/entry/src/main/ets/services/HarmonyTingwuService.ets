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

type HarmonyJsonObject = Record<string, Object>;
type HarmonyStringMap = Record<string, string>;

interface HarmonyTingwuSubmitInput {
  SourceLanguage: string;
  TaskKey: string;
  FileUrl: string;
}

interface HarmonyTingwuSubmitDiarization {
  OutputLevel: number;
}

interface HarmonyTingwuSubmitTranscription {
  DiarizationEnabled: boolean;
  Diarization: HarmonyTingwuSubmitDiarization;
}

interface HarmonyTingwuSubmitSummarization {
  Types: string[];
}

interface HarmonyTingwuSubmitMeetingAssistance {
  Types: string[];
}

interface HarmonyTingwuSubmitTranscoding {
  TargetAudioFormat: string;
}

interface HarmonyTingwuSubmitParameters {
  Transcription: HarmonyTingwuSubmitTranscription;
  AutoChaptersEnabled: boolean;
  SummarizationEnabled: boolean;
  Summarization: HarmonyTingwuSubmitSummarization;
  MeetingAssistanceEnabled: boolean;
  MeetingAssistance: HarmonyTingwuSubmitMeetingAssistance;
  Transcoding: HarmonyTingwuSubmitTranscoding;
}

interface HarmonyTingwuSubmitRequest {
  AppKey: string;
  Input: HarmonyTingwuSubmitInput;
  Parameters: HarmonyTingwuSubmitParameters;
}

interface HarmonyTingwuGetHeaders {
  Accept: string;
}

interface HarmonyTingwuHeaderState {
  accept: string;
  contentType: string;
  date: string;
  xAcsDate: string;
  xAcsAccessKeyId: string;
  xAcsSignatureMethod: string;
  xAcsSignatureVersion: string;
  xAcsSignatureNonce: string;
  xTingwuAppKey: string;
  xAcsSecurityToken?: string;
  authorization?: string;
}

interface HarmonyTingwuRequestHeaders {
  Accept: string;
  'Content-Type': string;
  Date: string;
  'x-acs-date': string;
  'x-acs-access-key-id': string;
  'x-acs-signature-method': string;
  'x-acs-signature-version': string;
  'x-acs-signature-nonce': string;
  'x-tingwu-app-key': string;
  'x-acs-security-token'?: string;
  Authorization?: string;
}

interface HarmonyTingwuProviderPayloads {
  transcription: string;
  autoChapters: string;
  summarization: string;
  meetingAssistance: string;
  identityRecognition: string;
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
  Result?: HarmonyStringMap;
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
    const input: HarmonyTingwuSubmitInput = {
      SourceLanguage: 'cn',
      TaskKey: taskKey,
      FileUrl: fileUrl
    };
    const diarization: HarmonyTingwuSubmitDiarization = {
      OutputLevel: 1
    };
    const transcription: HarmonyTingwuSubmitTranscription = {
      DiarizationEnabled: true,
      Diarization: diarization
    };
    const summarization: HarmonyTingwuSubmitSummarization = {
      Types: ['Paragraph', 'Conversational', 'QuestionsAnswering']
    };
    const meetingAssistance: HarmonyTingwuSubmitMeetingAssistance = {
      Types: ['Actions', 'KeyInformation']
    };
    const transcoding: HarmonyTingwuSubmitTranscoding = {
      TargetAudioFormat: 'mp3'
    };
    const parameters: HarmonyTingwuSubmitParameters = {
      Transcription: transcription,
      AutoChaptersEnabled: true,
      SummarizationEnabled: true,
      Summarization: summarization,
      MeetingAssistanceEnabled: true,
      MeetingAssistance: meetingAssistance,
      Transcoding: transcoding
    };
    const requestBody: HarmonyTingwuSubmitRequest = {
      AppKey: harmonyRuntimeConfig.TINGWU_APP_KEY.trim(),
      Input: input,
      Parameters: parameters
    };
    const body = JSON.stringify(requestBody);
    const response = await this.signedRequest('PUT', 'tasks?type=offline', body);
    const payload = this.parseJsonObject(response.textBody || '{}');
    if (cleanString(payload.Code) && payload.Code !== '0') {
      throw new Error(`Tingwu submit failed: ${cleanString(payload.Message) || payload.Code}`);
    }
    const data = safeObject(payload.Data) || this.emptyJsonObject();
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
    const payload = this.parseJsonObject(response.textBody || '{}') as HarmonyTingwuStatusEnvelope;
    if (payload.Code && payload.Code !== '0' && payload.Code !== 'TaskNotFound') {
      throw new Error(`Tingwu status failed: ${payload.Message || payload.Code}`);
    }
    return payload;
  }

  private async fetchArtifacts(data: HarmonyTingwuStatusData): Promise<HarmonyTingwuCompletedResult> {
    const resultLinks = data.Result || this.emptyStringMap();
    const transcriptionUrl = resultLinks.Transcription;
    const autoChaptersUrl = resultLinks.AutoChapters;
    const summarizationUrl = resultLinks.Summarization;
    const meetingAssistanceUrl = resultLinks.MeetingAssistance;
    const identityRecognitionUrl = resultLinks.IdentityRecognition;
    const artifactBodies = await Promise.all([
      this.fetchPublicBody(transcriptionUrl),
      this.fetchPublicBody(autoChaptersUrl),
      this.fetchPublicBody(summarizationUrl),
      this.fetchPublicBody(meetingAssistanceUrl),
      this.fetchPublicBody(identityRecognitionUrl)
    ]);
    const transcriptionRaw = artifactBodies[0];
    const autoChaptersRaw = artifactBodies[1];
    const summarizationRaw = artifactBodies[2];
    const meetingAssistanceRaw = artifactBodies[3];
    const identityRecognitionRaw = artifactBodies[4];

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

    const providerPayloads: HarmonyTingwuProviderPayloads = {
      transcription: transcriptionRaw || '',
      autoChapters: autoChaptersRaw || '',
      summarization: summarizationRaw || '',
      meetingAssistance: meetingAssistanceRaw || '',
      identityRecognition: identityRecognitionRaw || ''
    };
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
      providerPayloads
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
    const headers: HarmonyTingwuGetHeaders = {
      Accept: 'application/json'
    };
    const response = await harmonyHttpClient.requestText(url, http.RequestMethod.GET, headers);
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
    const headerState: HarmonyTingwuHeaderState = {
      accept: 'application/json',
      contentType: contentType,
      date: date,
      xAcsDate: date,
      xAcsAccessKeyId: accessKeyId,
      xAcsSignatureMethod: 'HMAC-SHA1',
      xAcsSignatureVersion: '1.0',
      xAcsSignatureNonce: nonce,
      xTingwuAppKey: appKey
    };
    if (harmonyRuntimeConfig.TINGWU_SECURITY_TOKEN.trim().length > 0) {
      headerState.xAcsSecurityToken = harmonyRuntimeConfig.TINGWU_SECURITY_TOKEN.trim();
    }

    const stringToSign = this.buildStringToSign(method, headerState, contentType, date, resource);
    const signature = hmacSha1Base64(accessKeySecret, stringToSign);
    headerState.authorization = `acs ${accessKeyId}:${signature}`;
    const headers = this.createRequestHeaders(headerState);

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
    headers: HarmonyTingwuHeaderState,
    contentType: string,
    date: string,
    resource: string
  ): string {
    const canonicalLines: string[] = [];
    canonicalLines.push(`x-acs-access-key-id:${headers.xAcsAccessKeyId.trim()}`);
    canonicalLines.push(`x-acs-date:${headers.xAcsDate.trim()}`);
    if (headers.xAcsSecurityToken) {
      canonicalLines.push(`x-acs-security-token:${headers.xAcsSecurityToken.trim()}`);
    }
    canonicalLines.push(`x-acs-signature-method:${headers.xAcsSignatureMethod.trim()}`);
    canonicalLines.push(`x-acs-signature-nonce:${headers.xAcsSignatureNonce.trim()}`);
    canonicalLines.push(`x-acs-signature-version:${headers.xAcsSignatureVersion.trim()}`);
    const canonicalHeaderLines = canonicalLines.join('\n');
    return `${method}\napplication/json\n\n${contentType}\n${date}\n${canonicalHeaderLines}\n/${resource}`;
  }

  private createRequestHeaders(state: HarmonyTingwuHeaderState): HarmonyTingwuRequestHeaders {
    const headers: HarmonyTingwuRequestHeaders = {
      Accept: state.accept,
      'Content-Type': state.contentType,
      Date: state.date,
      'x-acs-date': state.xAcsDate,
      'x-acs-access-key-id': state.xAcsAccessKeyId,
      'x-acs-signature-method': state.xAcsSignatureMethod,
      'x-acs-signature-version': state.xAcsSignatureVersion,
      'x-acs-signature-nonce': state.xAcsSignatureNonce,
      'x-tingwu-app-key': state.xTingwuAppKey,
      'x-acs-security-token': state.xAcsSecurityToken,
      Authorization: state.authorization
    };
    return headers;
  }

  private parseJsonObject(raw: string): HarmonyJsonObject {
    try {
      const parsed: Object = JSON.parse(raw) as Object;
      const payload = safeObject(parsed);
      return payload ? payload as HarmonyJsonObject : this.emptyJsonObject();
    } catch (_error) {
      return this.emptyJsonObject();
    }
  }

  private extractTranscriptionRoot(raw?: string): HarmonyJsonObject {
    const payload = this.parseJsonObject(raw || '{}');
    return safeObject(payload.Transcription) as HarmonyJsonObject || payload;
  }

  private parseDiarizedSegments(root: HarmonyJsonObject): HarmonyDiarizedSegment[] {
    const result: HarmonyDiarizedSegment[] = [];
    arrayOfObjects(root.Segments).forEach((segment, index: number) => {
      const nextSegment: HarmonyDiarizedSegment = {
        speakerId: cleanString(segment.Speaker) || cleanString(segment.SpeakerId),
        speakerIndex: Number(segment.Id || index),
        startMs: Math.floor(Number(segment.Start || 0) * 1000),
        endMs: Math.floor(Number(segment.End || 0) * 1000),
        text: cleanString(segment.Text) || ''
      };
      if (nextSegment.text.length > 0) {
        result.push(nextSegment);
      }
    });
    return result;
  }

  private parseTranscriptionSpeakerLabels(root: HarmonyJsonObject): HarmonyStringMap {
    const labels = this.emptyStringMap();
    arrayOfObjects(root.Speakers).forEach((speaker) => {
      const speakerId = cleanString(speaker.Id) || cleanString(speaker.SpeakerId);
      const label = cleanString(speaker.Name) || cleanString(speaker.SpeakerName);
      if (speakerId && label) {
        labels[speakerId] = label;
      }
    });
    return labels;
  }

  private parseIdentitySpeakerLabels(raw?: string): HarmonyStringMap {
    const payload = this.parseJsonObject(raw || '{}');
    const labels = this.emptyStringMap();
    const root = safeObject(payload);
    const items = arrayOfObjects(root?.IdentityRecognition)
      .concat(arrayOfObjects(safeObject(root?.Data)?.IdentityRecognition));
    items.forEach((item) => {
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
    base: HarmonyStringMap,
    segments: HarmonyDiarizedSegment[],
    identityLabels: HarmonyStringMap
  ): HarmonyStringMap {
    const merged = this.emptyStringMap();
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
    const payload = this.parseJsonObject(raw || '{}');
    const items = arrayOfObjects(payload.AutoChapters)
      .concat(arrayOfObjects(payload.Chapters))
      .concat(arrayOfObjects(safeObject(payload.Data)?.AutoChapters))
      .concat(arrayOfObjects(safeObject(payload.Data)?.Chapters));
    const chapters: HarmonyChapter[] = [];
    items.forEach((item, index: number) => {
      const chapter: HarmonyChapter = {
        title: cleanString(item.Title) || cleanString(item.Head) || `Chapter ${index + 1}`,
        startMs: Math.floor(Number(item.Start || item.StartTime || 0) * 1000),
        endMs: Math.floor(Number(item.End || item.EndTime || 0) * 1000),
        summary: cleanString(item.Summary) || cleanString(item.Text)
      };
      chapters.push(chapter);
    });
    return chapters;
  }

  private parseSmartSummary(raw?: string): HarmonySmartSummary | undefined {
    const payload = this.parseJsonObject(raw || '{}');
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
    const payload = this.parseJsonObject(raw || '{}');
    const meetingRoot = safeObject(payload.MeetingAssistance) || payload;
    return uniqueStrings(safeStringArray(meetingRoot.Keywords));
  }

  private makeResultLinks(resultLinks: HarmonyStringMap): HarmonyResultLink[] {
    const links: HarmonyResultLink[] = [];
    Object.keys(resultLinks).forEach((label: string) => {
      const nextLink: HarmonyResultLink = {
        label,
        url: resultLinks[label]
      };
      links.push(nextLink);
    });
    return links;
  }

  private extractExtraResultUrls(resultLinks: HarmonyStringMap): HarmonyStringMap {
    const extras = this.emptyStringMap();
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

  private emptyJsonObject(): HarmonyJsonObject {
    const empty: HarmonyJsonObject = {};
    return empty;
  }

  private emptyStringMap(): HarmonyStringMap {
    const empty: HarmonyStringMap = {};
    return empty;
  }
}

export const harmonyTingwuService = new HarmonyTingwuService();
