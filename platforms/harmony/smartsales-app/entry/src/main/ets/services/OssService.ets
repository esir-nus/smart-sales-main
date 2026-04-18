// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/OssService.ets
// Module: smartsales-app/entry
// Summary: OSS 对象存储上传服务 — 迁移自 tingwu-container HarmonyOssService.ets
// Author: created on 2026-04-16

import { http } from '@kit.NetworkKit';
import { normalizeOssEndpoint } from '../config/AppConfig';
import { getAppRuntimeConfig } from '../config/AppRuntimeConfig';
import { AudioErrorReason, AudioPipelineError } from '../domain/Audio';
import { fileStore } from './FileStore';
import { httpClient } from './HttpClient';
import { hmacSha1Base64, buildRfc1123Date } from '../utils/SignatureUtils';

export interface OssUploadResult {
  objectKey: string;
  publicUrl: string;
}

interface OssUploadHeaders {
  Date: string;
  Authorization: string;
  'Content-Type': string;
}

const RETRY_DELAYS_MS: number[] = [1000, 2000, 4000];

class OssService {
  async uploadAudio(
    localPath: string,
    objectKey: string,
    contentType: string
  ): Promise<OssUploadResult> {
    const runtimeConfig = getAppRuntimeConfig();
    const bucketName = runtimeConfig.OSS_BUCKET_NAME.trim();
    const accessKeyId = runtimeConfig.OSS_ACCESS_KEY_ID.trim();
    const accessKeySecret = runtimeConfig.OSS_ACCESS_KEY_SECRET.trim();
    const endpointHost = normalizeOssEndpoint(runtimeConfig.OSS_ENDPOINT);
    if (!bucketName || !accessKeyId || !accessKeySecret || !endpointHost) {
      throw new Error('OSS config is incomplete in AppConfig.ets');
    }

    const normalizedKey = objectKey.replace(/^\/+/, '');
    const encodedPath = normalizedKey.split('/').map((segment: string) => encodeURIComponent(segment)).join('/');
    const publicUrl = `https://${bucketName}.${endpointHost}/${encodedPath}`;
    const date = buildRfc1123Date();
    const stringToSign = `PUT\n\n${contentType}\n${date}\n/${bucketName}/${normalizedKey}`;
    const signature = hmacSha1Base64(accessKeySecret, stringToSign);
    const uploadBody = fileStore.readBinary(localPath);
    const headers: OssUploadHeaders = {
      Date: date,
      Authorization: `OSS ${accessKeyId}:${signature}`,
      'Content-Type': contentType
    };

    let attempt = 0;
    while (attempt <= RETRY_DELAYS_MS.length) {
      try {
        console.info(`[HS-010][OSS] upload attempt=${attempt + 1} key=${normalizedKey}`);
        const response = await httpClient.requestText(
          publicUrl,
          http.RequestMethod.PUT,
          headers,
          uploadBody
        );

        if (response.statusCode >= 200 && response.statusCode < 300) {
          return {
            objectKey: normalizedKey,
            publicUrl
          };
        }

        const body = response.textBody || '';
        if (this.isAuthFailure(response.statusCode, body)) {
          console.error(`[HS-010][OSS] auth failed status=${response.statusCode} key=${normalizedKey}`);
          throw new AudioPipelineError(
            AudioErrorReason.AUTH_FAILED,
            `OSS auth failed (${response.statusCode}) ${body}`.trim(),
            this.extractAuthDetailCode(body),
            response.statusCode
          );
        }

        if (response.statusCode >= 500 && response.statusCode < 600) {
          if (attempt < RETRY_DELAYS_MS.length) {
            const delayMs = RETRY_DELAYS_MS[attempt];
            console.warn(`[HS-010][OSS] retryable server failure status=${response.statusCode} delayMs=${delayMs}`);
            await this.sleep(delayMs);
            attempt += 1;
            continue;
          }
          throw new AudioPipelineError(
            AudioErrorReason.UPLOAD_FAILED,
            `OSS upload failed (${response.statusCode}) ${body}`.trim(),
            undefined,
            response.statusCode
          );
        }

        throw new AudioPipelineError(
          AudioErrorReason.UPLOAD_FAILED,
          `OSS upload failed (${response.statusCode}) ${body}`.trim(),
          undefined,
          response.statusCode
        );
      } catch (error) {
        if (error instanceof AudioPipelineError) {
          throw error;
        }
        const serializedFailure = this.serializeFailure(error);
        if (this.isAuthFailure(0, serializedFailure) || this.hasThrownAuthStatus(serializedFailure)) {
          const detailCode = this.extractAuthDetailCode(serializedFailure);
          console.error(`[HS-010][OSS] auth failed via thrown transport detail=${detailCode || 'UNKNOWN'} key=${normalizedKey}`);
          throw new AudioPipelineError(
            AudioErrorReason.AUTH_FAILED,
            `OSS auth failed ${serializedFailure}`.trim(),
            detailCode
          );
        }
        const failure = error instanceof Error ? error : new Error(serializedFailure);
        if (attempt < RETRY_DELAYS_MS.length) {
          const delayMs = RETRY_DELAYS_MS[attempt];
          console.warn(`[HS-010][OSS] network retry attempt=${attempt + 1} delayMs=${delayMs} message=${failure.message}`);
          await this.sleep(delayMs);
          attempt += 1;
          continue;
        }
        throw new AudioPipelineError(
          AudioErrorReason.NETWORK_FAILED,
          `OSS upload network failure: ${failure.message}`
        );
      }
    }
    throw new AudioPipelineError(AudioErrorReason.UNKNOWN, 'OSS upload exited unexpectedly');
  }

  private isAuthFailure(statusCode: number, body: string): boolean {
    if (statusCode === 401 || statusCode === 403) {
      return true;
    }
    return body.includes('SignatureDoesNotMatch') || body.includes('InvalidAccessKeyId');
  }

  private extractAuthDetailCode(body: string): string | undefined {
    if (body.includes('SignatureDoesNotMatch')) {
      return 'SignatureDoesNotMatch';
    }
    if (body.includes('InvalidAccessKeyId')) {
      return 'InvalidAccessKeyId';
    }
    return undefined;
  }

  private hasThrownAuthStatus(body: string): boolean {
    return body.includes('"responseCode":401')
      || body.includes('"responseCode":403')
      || body.includes('"statusCode":401')
      || body.includes('"statusCode":403')
      || body.includes('responseCode=401')
      || body.includes('responseCode=403')
      || body.includes('statusCode=401')
      || body.includes('statusCode=403');
  }

  private serializeFailure(error: Object | null | undefined): string {
    if (error instanceof Error) {
      return error.message;
    }
    if (error && typeof error === 'object') {
      try {
        return JSON.stringify(error);
      } catch {
        return `${error}`;
      }
    }
    return `${error}`;
  }

  private sleep(durationMs: number): Promise<void> {
    return new Promise((resolve: () => void) => {
      setTimeout(() => resolve(), durationMs);
    });
  }
}

export const ossService = new OssService();
