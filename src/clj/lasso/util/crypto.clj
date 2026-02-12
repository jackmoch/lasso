(ns lasso.util.crypto
  "Cryptographic utilities for session key encryption and Last.fm API signatures."
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.hash :as hash])
  (:import [java.util UUID Base64]
           [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec IvParameterSpec]
           [java.security SecureRandom]))

(defn generate-uuid
  "Generate a random UUID string for session IDs."
  []
  (str (UUID/randomUUID)))

(defn md5
  "Generate MD5 hash of a string. Used for Last.fm API signature generation."
  [s]
  (-> s
      (hash/md5)
      (codecs/bytes->hex)))

(defn- get-encryption-key
  "Get encryption key from environment or generate one.
   Key must be 32 bytes for AES-256."
  [secret]
  (let [key-bytes (codecs/str->bytes secret)]
    (if (= 32 (count key-bytes))
      key-bytes
      ;; Pad or truncate to 32 bytes
      (let [padded (byte-array 32)]
        (System/arraycopy key-bytes 0 padded 0 (min 32 (count key-bytes)))
        padded))))

(defn encrypt
  "Encrypt a string using AES-256-CBC encryption.
   Returns base64-encoded encrypted data with IV prepended."
  [plaintext encryption-secret]
  (let [key-bytes (get-encryption-key encryption-secret)
        key-spec (SecretKeySpec. key-bytes "AES")
        cipher (Cipher/getInstance "AES/CBC/PKCS5Padding")

        ;; Generate random IV
        iv-bytes (byte-array 16)
        _ (.nextBytes (SecureRandom.) iv-bytes)
        iv-spec (IvParameterSpec. iv-bytes)

        ;; Encrypt
        _ (.init cipher Cipher/ENCRYPT_MODE key-spec iv-spec)
        plaintext-bytes (codecs/str->bytes plaintext)
        encrypted-bytes (.doFinal cipher plaintext-bytes)

        ;; Prepend IV to encrypted data
        result (byte-array (+ 16 (count encrypted-bytes)))]
    (System/arraycopy iv-bytes 0 result 0 16)
    (System/arraycopy encrypted-bytes 0 result 16 (count encrypted-bytes))
    (.encodeToString (Base64/getEncoder) result)))

(defn decrypt
  "Decrypt a base64-encoded encrypted string using AES-256-CBC.
   Expects IV to be prepended to the encrypted data."
  [encrypted-b64 encryption-secret]
  (let [key-bytes (get-encryption-key encryption-secret)
        key-spec (SecretKeySpec. key-bytes "AES")
        cipher (Cipher/getInstance "AES/CBC/PKCS5Padding")

        ;; Decode and extract IV and encrypted data
        encrypted-with-iv (.decode (Base64/getDecoder) encrypted-b64)
        iv-bytes (byte-array 16)
        encrypted-bytes (byte-array (- (count encrypted-with-iv) 16))]
    (System/arraycopy encrypted-with-iv 0 iv-bytes 0 16)
    (System/arraycopy encrypted-with-iv 16 encrypted-bytes 0 (count encrypted-bytes))

    ;; Decrypt
    (let [iv-spec (IvParameterSpec. iv-bytes)]
      (.init cipher Cipher/DECRYPT_MODE key-spec iv-spec)
      (let [decrypted-bytes (.doFinal cipher encrypted-bytes)]
        (codecs/bytes->str decrypted-bytes)))))
