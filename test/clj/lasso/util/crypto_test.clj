(ns lasso.util.crypto-test
  (:require [clojure.test :refer [deftest is testing]]
            [lasso.util.crypto :as crypto]))

(def test-secret "12345678901234567890123456789012") ; 32 bytes

(deftest generate-uuid-test
  (testing "UUID generation"
    (let [uuid1 (crypto/generate-uuid)
          uuid2 (crypto/generate-uuid)]
      (is (string? uuid1) "UUID should be a string")
      (is (not= uuid1 uuid2) "UUIDs should be unique")
      (is (re-matches #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" uuid1)
          "UUID should match standard format"))))

(deftest md5-test
  (testing "MD5 hash generation"
    (is (= "098f6bcd4621d373cade4e832627b4f6" (crypto/md5 "test"))
        "MD5 of 'test' should match known value")
    (is (= "d41d8cd98f00b204e9800998ecf8427e" (crypto/md5 ""))
        "MD5 of empty string should match known value")
    (is (= "5d41402abc4b2a76b9719d911017c592" (crypto/md5 "hello"))
        "MD5 of 'hello' should match known value")))

(deftest encrypt-decrypt-roundtrip-test
  (testing "Encrypt/decrypt roundtrip"
    (let [plaintext "my-secret-session-key"
          encrypted (crypto/encrypt plaintext test-secret)
          decrypted (crypto/decrypt encrypted test-secret)]
      (is (string? encrypted) "Encrypted value should be a string")
      (is (not= plaintext encrypted) "Encrypted should differ from plaintext")
      (is (= plaintext decrypted) "Decrypted should match original plaintext")))

  (testing "Multiple encryptions produce different ciphertexts (IV randomization)"
    (let [plaintext "test-data"
          encrypted1 (crypto/encrypt plaintext test-secret)
          encrypted2 (crypto/encrypt plaintext test-secret)]
      (is (not= encrypted1 encrypted2) "Same plaintext should encrypt differently each time")
      (is (= plaintext (crypto/decrypt encrypted1 test-secret)))
      (is (= plaintext (crypto/decrypt encrypted2 test-secret)))))

  (testing "Encryption works with various string lengths"
    (doseq [text ["a" "short" "a bit longer text" "very long text with many characters that spans multiple blocks"]]
      (let [encrypted (crypto/encrypt text test-secret)
            decrypted (crypto/decrypt encrypted test-secret)]
        (is (= text decrypted) (str "Should encrypt/decrypt: " text))))))

(deftest encryption-with-different-secrets-test
  (testing "Decryption fails with wrong secret"
    (let [plaintext "secret-data"
          secret1 "12345678901234567890123456789012"
          secret2 "abcdefghijklmnopqrstuvwxyz123456"
          encrypted (crypto/encrypt plaintext secret1)]
      (is (thrown? Exception
                   (crypto/decrypt encrypted secret2))
          "Decryption should fail with wrong secret"))))
