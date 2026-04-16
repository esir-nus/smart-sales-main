// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/OssService.ets
// Module: smartsales-app/entry
// Summary: OSS 对象存储上传服务 — 迁移自 tingwu-container HarmonyOssService.ets
// Author: created on 2026-04-16

import { http } from '@kit.NetworkKit';
import { normalizeOssEndpoint } from '../config/AppConfig';
import { getAppRuntimeConfig } from '../config/AppRuntimeConfig';
import { fileStore } from './FileStore';
import { httpClient } from './HttpClient';
import { hmacSha1Base64, buildRfc1123Date } from '../utils/SignatureUtils';

export interface OssUploadResult {
  objectKey: string;
  publicUrl: string;
}

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

    const response = await httpClient.requestText(publicUrl, http.RequestMethod.PUT, {
      Date: date,
      Authorization: `OSS ${accessKeyId}:${signature}`,
      'Content-Type': contentType
    }, uploadBody);

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw new Error(`OSS upload failed (${response.statusCode}) ${response.textBody || ''}`.trim());
    }

    return {
      objectKey: normalizedKey,
      publicUrl
    };
  }
}

export const ossService = new OssService();
