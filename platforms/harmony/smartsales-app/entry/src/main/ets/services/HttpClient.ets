// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/HttpClient.ets
// Module: smartsales-app/entry
// Summary: HTTP 传输层封装 — 迁移自 tingwu-container HarmonyHttpClient.ets
// Author: created on 2026-04-16

import { http } from '@kit.NetworkKit';

export interface HttpResponse {
  statusCode: number;
  headers: Record<string, string>;
  textBody?: string;
  binaryBody?: ArrayBuffer;
}

class HttpClient {
  async requestText(
    url: string,
    method: http.RequestMethod,
    headers: Record<string, string>,
    extraData?: string | ArrayBuffer
  ): Promise<HttpResponse> {
    return this.request(url, method, headers, http.HttpDataType.STRING, extraData);
  }

  async requestBinary(
    url: string,
    method: http.RequestMethod,
    headers: Record<string, string>,
    extraData?: string | ArrayBuffer
  ): Promise<HttpResponse> {
    return this.request(url, method, headers, http.HttpDataType.ARRAY_BUFFER, extraData);
  }

  private async request(
    url: string,
    method: http.RequestMethod,
    headers: Record<string, string>,
    expectDataType: http.HttpDataType,
    extraData?: string | ArrayBuffer
  ): Promise<HttpResponse> {
    const client = http.createHttp();
    try {
      const response = await client.request(url, {
        method,
        header: headers,
        extraData,
        expectDataType,
        connectTimeout: 15_000,
        readTimeout: 60_000
      });
      const normalizedHeaders: Record<string, string> = {};
      const rawHeaders = response.header as Record<string, string> | undefined;
      if (rawHeaders) {
        Object.keys(rawHeaders).forEach((key: string) => {
          normalizedHeaders[key] = `${rawHeaders[key]}`;
        });
      }
      return {
        statusCode: response.responseCode,
        headers: normalizedHeaders,
        textBody: typeof response.result === 'string' ? response.result : undefined,
        binaryBody: response.result instanceof ArrayBuffer ? response.result : undefined
      };
    } finally {
      client.destroy();
    }
  }
}

export const httpClient = new HttpClient();
