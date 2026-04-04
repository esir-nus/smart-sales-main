import { http } from '@kit.NetworkKit';
import {
  normalizeOssEndpoint
} from '../config/HarmonyContainerConfig';
import { getHarmonyRuntimeConfig } from '../config/HarmonyContainerRuntimeConfig';
import { harmonyFileStore } from './HarmonyFileStore';
import { harmonyHttpClient } from './HarmonyHttpClient';
import { hmacSha1Base64, buildRfc1123Date } from '../utils/HarmonySignatureUtils';

export interface HarmonyOssUploadResult {
  objectKey: string;
  publicUrl: string;
}

class HarmonyOssService {
  async uploadAudio(
    localPath: string,
    objectKey: string,
    contentType: string
  ): Promise<HarmonyOssUploadResult> {
    const harmonyRuntimeConfig = getHarmonyRuntimeConfig();
    const bucketName = harmonyRuntimeConfig.OSS_BUCKET_NAME.trim();
    const accessKeyId = harmonyRuntimeConfig.OSS_ACCESS_KEY_ID.trim();
    const accessKeySecret = harmonyRuntimeConfig.OSS_ACCESS_KEY_SECRET.trim();
    const endpointHost = normalizeOssEndpoint(harmonyRuntimeConfig.OSS_ENDPOINT);
    if (!bucketName || !accessKeyId || !accessKeySecret || !endpointHost) {
      throw new Error('OSS config is incomplete in HarmonyContainerConfig.ets');
    }

    const normalizedKey = objectKey.replace(/^\/+/, '');
    const encodedPath = normalizedKey.split('/').map((segment: string) => encodeURIComponent(segment)).join('/');
    const publicUrl = `https://${bucketName}.${endpointHost}/${encodedPath}`;
    const date = buildRfc1123Date();
    const stringToSign = `PUT\n\n${contentType}\n${date}\n/${bucketName}/${normalizedKey}`;
    const signature = hmacSha1Base64(accessKeySecret, stringToSign);
    const uploadBody = harmonyFileStore.readBinary(localPath);

    const response = await harmonyHttpClient.requestText(publicUrl, http.RequestMethod.PUT, {
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

export const harmonyOssService = new HarmonyOssService();
