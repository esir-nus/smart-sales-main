import { http } from '@kit.NetworkKit';

export interface HarmonyHttpResponse {
  statusCode: number;
  headers: Record<string, string>;
  textBody?: string;
  binaryBody?: ArrayBuffer;
}

class HarmonyHttpClient {
  async requestText(
    url: string,
    method: http.RequestMethod,
    headers: Record<string, string>,
    extraData?: string | ArrayBuffer
  ): Promise<HarmonyHttpResponse> {
    return this.request(url, method, headers, http.HttpDataType.STRING, extraData);
  }

  async requestBinary(
    url: string,
    method: http.RequestMethod,
    headers: Record<string, string>,
    extraData?: string | ArrayBuffer
  ): Promise<HarmonyHttpResponse> {
    return this.request(url, method, headers, http.HttpDataType.ARRAY_BUFFER, extraData);
  }

  private async request(
    url: string,
    method: http.RequestMethod,
    headers: Record<string, string>,
    expectDataType: http.HttpDataType,
    extraData?: string | ArrayBuffer
  ): Promise<HarmonyHttpResponse> {
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

export const harmonyHttpClient = new HarmonyHttpClient();
