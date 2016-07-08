/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.commonsware.cwac.netsecurity.luni;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
/**
 * Perform encoding and decoding of Base64 byte arrays as described in
 * http://www.ietf.org/rfc/rfc2045.txt
 */
public final class Base64 {
  private static final byte[] BASE_64_ALPHABET = initializeBase64Alphabet();
  private static byte[] initializeBase64Alphabet() {
    return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
      .getBytes(StandardCharsets.US_ASCII);
  }
  // Bit masks for the 4 output 6-bit values from 3 input bytes.
  private static final int FIRST_OUTPUT_BYTE_MASK = 0x3f << 18;
  private static final int SECOND_OUTPUT_BYTE_MASK = 0x3f << 12;
  private static final int THIRD_OUTPUT_BYTE_MASK = 0x3f << 6;
  private static final int FOURTH_OUTPUT_BYTE_MASK = 0x3f;
  private Base64() {}
  public static String encode(byte[] in) {
    int len = in.length;
    int outputLen = computeEncodingOutputLen(len);
    byte[] output = new byte[outputLen];
    int outputIndex = 0;
    for (int i = 0; i < len; i += 3) {
      // Only a "triplet" if there are there are at least three remaining bytes
      // in the input...
      // Mask with 0xff to avoid signed extension.
      int byteTripletAsInt = in[i] & 0xff;
      if (i + 1 < len) {
        // Add second byte to the triplet.
        byteTripletAsInt <<= 8;
        byteTripletAsInt |= in[i + 1] & 0xff;
        if (i + 2 < len) {
          byteTripletAsInt <<= 8;
          byteTripletAsInt |= in[i + 2] & 0xff;
        } else {
          // Insert 2 zero bits as to make output 18 bits long.
          byteTripletAsInt <<= 2;
        }
      } else {
        // Insert 4 zero bits as to make output 12 bits long.
        byteTripletAsInt <<= 4;
      }
      if (i + 2 < len) {
        // The int may have up to 24 non-zero bits.
        output[outputIndex++] = BASE_64_ALPHABET[
          (byteTripletAsInt & FIRST_OUTPUT_BYTE_MASK) >>> 18];
      }
      if (i + 1 < len) {
        // The int may have up to 18 non-zero bits.
        output[outputIndex++] = BASE_64_ALPHABET[
          (byteTripletAsInt & SECOND_OUTPUT_BYTE_MASK) >>> 12];
      }
      output[outputIndex++] = BASE_64_ALPHABET[
        (byteTripletAsInt & THIRD_OUTPUT_BYTE_MASK) >>> 6];
      output[outputIndex++] = BASE_64_ALPHABET[
        byteTripletAsInt & FOURTH_OUTPUT_BYTE_MASK];
    }
    int inLengthMod3 = len % 3;
    // Add padding as per the spec.
    if (inLengthMod3 > 0) {
      output[outputIndex++] = '=';
      if (inLengthMod3 == 1) {
        output[outputIndex++] = '=';
      }
    }
    return new String(output, StandardCharsets.US_ASCII);
  }
  private static int computeEncodingOutputLen(int inLength) {
    int inLengthMod3 = inLength % 3;
    int outputLen = (inLength / 3) * 4;
    if (inLengthMod3 == 2) {
      // Need 3 6-bit characters as to express the last 16 bits, plus 1 padding.
      outputLen += 4;
    } else if (inLengthMod3 == 1) {
      // Need 2 6-bit characters as to express the last 8 bits, plus 2 padding.
      outputLen += 4;
    }
    return outputLen;
  }
  public static byte[] decode(byte[] in) {
    return decode(in, in.length);
  }
  /** Decodes the input from position 0 (inclusive) to len (exclusive). */
  public static byte[] decode(byte[] in, int len) {
    final int inLength = Math.min(in.length, len);
    // Overestimating 3 bytes per each 4 blocks of input (plus a possibly incomplete one).
    ByteArrayOutputStream output = new ByteArrayOutputStream((inLength / 4) * 3 + 3);
    // Position in the input. Use an array so we can pass it to {@code getNextByte}.
    int[] pos = new int[1];
    try {
      while (pos[0] < inLength) {
        int byteTripletAsInt = 0;
        // j is the index in a 4-tuple of 6-bit characters where are trying to read from the
        // input.
        for (int j = 0; j < 4; j++) {
          byte c = getNextByte(in, pos, inLength);
          if (c == END_OF_INPUT || c == PAD_AS_BYTE) {
            // Padding or end of file...
            switch (j) {
              case 0:
              case 1:
                return (c == END_OF_INPUT) ? output.toByteArray() : null;
              case 2:
                // The input is over with two 6-bit characters: a single byte padded
                // with 4 extra 0's.
                if (c == END_OF_INPUT) {
                  // Do not consider the block, since padding is not present.
                  return checkNoTrailingAndReturn(output, in, pos[0], inLength);
                }
                // We are at a pad character, consume and look for the second one.
                pos[0]++;
                c = getNextByte(in, pos, inLength);
                if (c == END_OF_INPUT) {
                  // Do not consider the block, since padding is not present.
                  return checkNoTrailingAndReturn(output, in, pos[0], inLength);
                }
                if (c == PAD_AS_BYTE) {
                  byteTripletAsInt >>= 4;
                  output.write(byteTripletAsInt);
                  return checkNoTrailingAndReturn(output, in, pos[0], inLength);
                }
                // Something other than pad and non-alphabet characters, illegal.
                return null;
              case 3:
                // The input is over with three 6-bit characters: two bytes padded
                // with 2 extra 0's.
                if (c == PAD_AS_BYTE) {
                  // Consider the block only if padding is present.
                  byteTripletAsInt >>= 2;
                  output.write(byteTripletAsInt >> 8);
                  output.write(byteTripletAsInt & 0xff);
                }
                return checkNoTrailingAndReturn(output, in, pos[0], inLength);
            }
          } else {
            byteTripletAsInt <<= 6;
            byteTripletAsInt += (c & 0xff);
            pos[0]++;
          }
        }
        // We have four 6-bit characters: output the corresponding 3 bytes
        output.write(byteTripletAsInt >> 16);
        output.write((byteTripletAsInt >> 8) & 0xff);
        output.write(byteTripletAsInt & 0xff);
      }
      return checkNoTrailingAndReturn(output, in, pos[0], inLength);
    } catch (InvalidBase64ByteException e) {
      return null;
    }
  }
  /**
   * On decoding, an illegal character always return null.
   *
   * Using this exception to avoid "if" checks every time.
   */
  private static class InvalidBase64ByteException extends Exception { }
  /**
   * Obtain the numeric value corresponding to the next relevant byte in the input.
   *
   * Calculates the numeric value (6-bit, 0 <= x <= 63) of the next Base64 encoded byte in
   * {@code in} at or after {@code pos[0]} and before {@code inLength}. Returns
   * {@link #WHITESPACE_AS_BYTE}, {@link #PAD_AS_BYTE}, {@link #END_OF_INPUT} or the 6-bit value.
   * {@code pos[0]} is updated as a side effect of this method.
   */
  private static byte getNextByte(byte[] in, int[] pos, int inLength)
    throws InvalidBase64ByteException {
    // Ignore all whitespace.
    while (pos[0] < inLength) {
      byte c = base64AlphabetToNumericalValue(in[pos[0]]);
      if (c != WHITESPACE_AS_BYTE) {
        return c;
      }
      pos[0]++;
    }
    return END_OF_INPUT;
  }
  /**
   * Check that there are no invalid trailing characters (ie, other then whitespace and padding)
   *
   * Returns {@code output} as a byte array in case of success, {@code null} in case of invalid
   * characters.
   */
  private static byte[] checkNoTrailingAndReturn(
    ByteArrayOutputStream output, byte[] in, int i, int inLength)
    throws InvalidBase64ByteException{
    while (i < inLength) {
      byte c = base64AlphabetToNumericalValue(in[i]);
      if (c != WHITESPACE_AS_BYTE && c != PAD_AS_BYTE) {
        return null;
      }
      i++;
    }
    return output.toByteArray();
  }
  private static final byte PAD_AS_BYTE = -1;
  private static final byte WHITESPACE_AS_BYTE = -2;
  private static final byte END_OF_INPUT = -3;
  private static byte base64AlphabetToNumericalValue(byte c) throws InvalidBase64ByteException {
    if ('A' <= c && c <= 'Z') {
      return (byte) (c - 'A');
    }
    if ('a' <= c && c <= 'z') {
      return (byte) (c - 'a' + 26);
    }
    if ('0' <= c && c <= '9') {
      return (byte) (c - '0' + 52);
    }
    if (c == '+') {
      return (byte) 62;
    }
    if (c == '/') {
      return (byte) 63;
    }
    if (c == '=') {
      return PAD_AS_BYTE;
    }
    if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
      return WHITESPACE_AS_BYTE;
    }
    throw new InvalidBase64ByteException();
  }
}