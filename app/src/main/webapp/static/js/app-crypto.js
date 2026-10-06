/*
 * Campus Events client-side envelope helper.
 *
 * SECURITY TRAINING LAB ONLY - this file deliberately contains the static
 * RC4 key shared with the server (also hardcoded in WEB-INF/classes/app.properties).
 * Anyone can read this file and forge/decrypt every application envelope.
 *
 * Envelope format: { "enc": base64( RC4(key, utf8(body)) ) }
 * RC4 keystream restarts on every message (classic RC4 reuse weakness).
 */
(function () {
  'use strict';

  var KEY = new TextEncoder().encode('Rc4StaticKey#Campus2026');

  function rc4(keyBytes, dataBytes) {
    var s = new Uint8Array(256);
    for (var i = 0; i < 256; i++) s[i] = i;
    var j = 0;
    for (var i = 0; i < 256; i++) {
      j = (j + s[i] + keyBytes[i % keyBytes.length]) & 255;
      var t = s[i]; s[i] = s[j]; s[j] = t;
    }
    var out = new Uint8Array(dataBytes.length);
    var i = 0, j = 0;
    for (var k = 0; k < dataBytes.length; k++) {
      i = (i + 1) & 255;
      j = (j + s[i]) & 255;
      var t = s[i]; s[i] = s[j]; s[j] = t;
      out[k] = dataBytes[k] ^ s[(s[i] + s[j]) & 255];
    }
    return out;
  }

  function bytesToB64(bytes) {
    var bin = '';
    var chunk = 0x8000;
    for (var i = 0; i < bytes.length; i += chunk) {
      bin += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
    }
    return btoa(bin);
  }

  function b64ToBytes(b64) {
    var bin = atob(b64);
    var out = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
    return out;
  }

  window.AppCrypto = {
    key: 'Rc4StaticKey#Campus2026',

    encrypt: function (text) {
      return bytesToB64(rc4(KEY, new TextEncoder().encode(text)));
    },

    decrypt: function (b64) {
      return new TextDecoder().decode(rc4(KEY, b64ToBytes(b64)));
    },

    decryptDocument: function (b64) {
      return this.decrypt(b64);
    },

    /** Wrap a plain object into an envelope body ready for fetch(). */
    encryptBody: function (obj) {
      return JSON.stringify({ enc: this.encrypt(JSON.stringify(obj)) });
    },

    /** Decrypt an envelope-shaped response body {enc: "..."} into an object. */
    decryptBody: function (env) {
      if (!env || typeof env.enc !== 'string') {
        throw new Error('Response was not an encrypted envelope');
      }
      return JSON.parse(this.decrypt(env.enc));
    }
  };
})();
