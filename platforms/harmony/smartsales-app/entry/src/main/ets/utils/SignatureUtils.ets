// File: platforms/harmony/smartsales-app/entry/src/main/ets/utils/SignatureUtils.ets
// Module: smartsales-app/entry
// Summary: 签名工具 — SHA1、HMAC-SHA1、Base64、RFC1123 日期、随机串
// Author: created on 2026-04-16
// 迁移来源: platforms/harmony/tingwu-container/entry/src/main/ets/utils/HarmonySignatureUtils.ets

import { util } from '@kit.ArkTS';

const BASE64_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

// 32 位循环左移
function rotateLeft(value: number, shift: number): number {
  return ((value << shift) | (value >>> (32 - shift))) >>> 0;
}

// 字符串转 UTF-8 字节数组
function stringToBytes(value: string): Uint8Array {
  return new util.TextEncoder().encodeInto(value);
}

// 字节数组转 Base64 字符串
function bytesToBase64(bytes: Uint8Array): string {
  let result = '';
  for (let index = 0; index < bytes.length; index += 3) {
    const first = bytes[index];
    const second = index + 1 < bytes.length ? bytes[index + 1] : 0;
    const third = index + 2 < bytes.length ? bytes[index + 2] : 0;
    const chunk = (first << 16) | (second << 8) | third;
    result += BASE64_ALPHABET.charAt((chunk >> 18) & 63);
    result += BASE64_ALPHABET.charAt((chunk >> 12) & 63);
    result += index + 1 < bytes.length ? BASE64_ALPHABET.charAt((chunk >> 6) & 63) : '=';
    result += index + 2 < bytes.length ? BASE64_ALPHABET.charAt(chunk & 63) : '=';
  }
  return result;
}

// SHA1 哈希实现（手写，无外部依赖）
function sha1(bytes: Uint8Array): Uint8Array {
  const bitLength = bytes.length * 8;
  const totalLength = (((bytes.length + 9 + 63) >> 6) << 6);
  const padded = new Uint8Array(totalLength);
  padded.set(bytes);
  padded[bytes.length] = 0x80;
  const view = new DataView(padded.buffer);
  view.setUint32(totalLength - 4, bitLength >>> 0, false);
  view.setUint32(totalLength - 8, Math.floor(bitLength / 0x100000000), false);

  let h0 = 0x67452301;
  let h1 = 0xefcdab89;
  let h2 = 0x98badcfe;
  let h3 = 0x10325476;
  let h4 = 0xc3d2e1f0;

  const words = new Uint32Array(80);
  for (let offset = 0; offset < totalLength; offset += 64) {
    for (let index = 0; index < 16; index += 1) {
      words[index] = view.getUint32(offset + index * 4, false);
    }
    for (let index = 16; index < 80; index += 1) {
      words[index] = rotateLeft(
        words[index - 3] ^ words[index - 8] ^ words[index - 14] ^ words[index - 16], 1
      );
    }

    let a = h0;
    let b = h1;
    let c = h2;
    let d = h3;
    let e = h4;

    for (let index = 0; index < 80; index += 1) {
      let f = 0;
      let k = 0;
      if (index < 20) {
        f = (b & c) | (~b & d);
        k = 0x5a827999;
      } else if (index < 40) {
        f = b ^ c ^ d;
        k = 0x6ed9eba1;
      } else if (index < 60) {
        f = (b & c) | (b & d) | (c & d);
        k = 0x8f1bbcdc;
      } else {
        f = b ^ c ^ d;
        k = 0xca62c1d6;
      }
      const temp = (rotateLeft(a, 5) + f + e + k + words[index]) >>> 0;
      e = d;
      d = c;
      c = rotateLeft(b, 30) >>> 0;
      b = a;
      a = temp;
    }

    h0 = (h0 + a) >>> 0;
    h1 = (h1 + b) >>> 0;
    h2 = (h2 + c) >>> 0;
    h3 = (h3 + d) >>> 0;
    h4 = (h4 + e) >>> 0;
  }

  const digest = new Uint8Array(20);
  const digestView = new DataView(digest.buffer);
  digestView.setUint32(0, h0, false);
  digestView.setUint32(4, h1, false);
  digestView.setUint32(8, h2, false);
  digestView.setUint32(12, h3, false);
  digestView.setUint32(16, h4, false);
  return digest;
}

// HMAC-SHA1 签名，返回 Base64 编码字符串
export function hmacSha1Base64(secret: string, payload: string): string {
  const blockSize = 64;
  let key = stringToBytes(secret);
  if (key.length > blockSize) {
    key = sha1(key);
  }
  const paddedKey = new Uint8Array(blockSize);
  paddedKey.set(key.slice(0, blockSize));
  const outerPad = new Uint8Array(blockSize);
  const innerPad = new Uint8Array(blockSize);
  for (let index = 0; index < blockSize; index += 1) {
    outerPad[index] = paddedKey[index] ^ 0x5c;
    innerPad[index] = paddedKey[index] ^ 0x36;
  }

  const payloadBytes = stringToBytes(payload);
  const innerPayload = new Uint8Array(blockSize + payloadBytes.length);
  innerPayload.set(innerPad, 0);
  innerPayload.set(payloadBytes, blockSize);
  const innerHash = sha1(innerPayload);

  const outerPayload = new Uint8Array(blockSize + innerHash.length);
  outerPayload.set(outerPad, 0);
  outerPayload.set(innerHash, blockSize);
  return bytesToBase64(sha1(outerPayload));
}

// RFC1123 格式日期字符串（OSS/听悟请求头使用）
export function buildRfc1123Date(): string {
  return new Date().toUTCString();
}

// 随机 nonce（请求防重放）
export function buildNonce(): string {
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 10)}`;
}
