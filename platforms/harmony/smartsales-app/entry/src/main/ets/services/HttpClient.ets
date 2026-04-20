// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/HttpClient.ets
// Module: smartsales-app/entry
// Summary: HTTP 传输层封装 — 迁移自 tingwu-container HarmonyHttpClient.ets
// Author: created on 2026-04-16

import { http } from '@kit.NetworkKit';

export interface HttpResponse {
  statusCode: number;
  textBody?: string;
  binaryBody?: ArrayBuffer;
}

class HttpClient {
  async requestText(
    url: string,
    method: http.RequestMethod,
    headers: Object,
    extraData?: string | ArrayBuffer
  ): Promise<HttpResponse> {
    return this.request(url, method, headers, http.HttpDataType.STRING, extraData);
  }

  async requestBinary(
    url: string,
    method: http.RequestMethod,
    headers: Object,
    extraData?: string | ArrayBuffer
  ): Promise<HttpResponse> {
    return this.request(url, method, headers, http.HttpDataType.ARRAY_BUFFER, extraData);
  }

  private async request(
    url: string,
    method: http.RequestMethod,
    headers: Object,
    expectDataType: http.HttpDataType,
    extraData?: string | ArrayBuffer
  ): Promise<HttpResponse> {
    const client = http.createHttp();
    try {
      try {
        const response = await client.request(url, {
          method,
          header: headers,
          extraData,
          expectDataType,
          connectTimeout: 15_000,
          readTimeout: 60_000
        });
        return {
          statusCode: response.responseCode,
          textBody: typeof response.result === 'string' ? response.result : undefined,
          binaryBody: response.result instanceof ArrayBuffer ? response.result : undefined
        };
      } catch (error) {
        const response = this.extractErrorResponse(error);
        if (response) {
          return response;
        }
        throw this.asError(error);
      }
    } finally {
      client.destroy();
    }
  }

  private extractErrorResponse(error: Object | null | undefined): HttpResponse | undefined {
    if (!error || typeof error !== 'object') {
      return undefined;
    }
    const record = error as Record<string, Object>;
    const rawStatus = record.responseCode ?? record.statusCode;
    const statusCode = Number(rawStatus);
    if (!Number.isFinite(statusCode) || statusCode <= 0) {
      return undefined;
    }
    const rawResult = record.result;
    return {
      statusCode,
      textBody: typeof rawResult === 'string' ? rawResult : undefined,
      binaryBody: rawResult instanceof ArrayBuffer ? rawResult : undefined
    };
  }

  private asError(error: Object | null | undefined): Error {
    if (error instanceof Error) {
      return error;
    }
    if (error && typeof error === 'object') {
      try {
        return new Error(JSON.stringify(error));
      } catch {
        return new Error(`${error}`);
      }
    }
    return new Error(`${error}`);
  }
}

export const httpClient = new HttpClient();
