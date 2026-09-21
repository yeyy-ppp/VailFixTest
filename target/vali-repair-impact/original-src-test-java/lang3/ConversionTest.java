package lang3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

class ConversionTest {
         @Test
         void binaryBeMsb0ToHexDigitWithPos() {
             assertThrows(IllegalArgumentException.class, () -> Conversion.binaryBeMsb0ToHexDigit(new boolean[0], 0));
             assertThrows(IndexOutOfBoundsException.class, () -> Conversion.binaryBeMsb0ToHexDigit(new boolean[3], 3));

             boolean[] bits1 = {true, false, false, true};
             assertEquals('9', Conversion.binaryBeMsb0ToHexDigit(bits1, 0));

             boolean[] bits2 = {false, true, true, false};
             assertEquals('6', Conversion.binaryBeMsb0ToHexDigit(bits2, 0));

             boolean[] bits3 = {true, false};
             assertEquals('2', Conversion.binaryBeMsb0ToHexDigit(bits3, 0));
         }

         @Test
         void binaryToHexDigitWithPos() {
             assertThrows(IllegalArgumentException.class, () -> Conversion.binaryToHexDigit(new boolean[0], 0));

             boolean[] bits1 = {true, true, true, true};
             assertEquals('f', Conversion.binaryToHexDigit(bits1, 0));

             boolean[] bits2 = {false, false, false, true};
             assertEquals('1', Conversion.binaryToHexDigit(bits2, 3));

             boolean[] bits3 = {true, false, true};
             assertEquals('5', Conversion.binaryToHexDigit(bits3, 0));
         }

         @Test
         void binaryToHexDigitMsb0_4bits() {
             assertThrows(IllegalArgumentException.class, () -> Conversion.binaryToHexDigitMsb0_4bits(new boolean[9], 0));
             assertThrows(IllegalArgumentException.class, () -> Conversion.binaryToHexDigitMsb0_4bits(new boolean[3], 0));

             boolean[] bits1 = {true, false, true, true};
             assertEquals('b', Conversion.binaryToHexDigitMsb0_4bits(bits1, 0));

             boolean[] bits2 = {false, true, true, false};
             assertEquals('6', Conversion.binaryToHexDigitMsb0_4bits(bits2, 0));

             boolean[] bits3 = {false, false, false, false};
             assertEquals('0', Conversion.binaryToHexDigitMsb0_4bits(bits3, 0));
         }

         @Test
         void hexDigitToBinary() {
             boolean[] f = {false, false, false, false};
             assertArrayEquals(f, Conversion.hexDigitToBinary('0'));

             boolean[] a = {true, false, true, false};
             assertArrayEquals(a, Conversion.hexDigitToBinary('5'));

             boolean[] b = {true, true, true, true};
             assertArrayEquals(b, Conversion.hexDigitToBinary('f'));

             assertThrows(IllegalArgumentException.class, () -> Conversion.hexDigitToBinary('x'));
         }

         @Test
         void intToHexDigit() {
             assertEquals('a', Conversion.intToHexDigit(10));
             assertEquals('0', Conversion.intToHexDigit(0));
             assertEquals('f', Conversion.intToHexDigit(15));

             assertThrows(IllegalArgumentException.class, () -> Conversion.intToHexDigit(16));
             assertThrows(IllegalArgumentException.class, () -> Conversion.intToHexDigit(-1));
         }

         @Test
         void intToHexDigitMsb0() {
             assertEquals('8', Conversion.intToHexDigitMsb0(1));
             assertEquals('4', Conversion.intToHexDigitMsb0(2));
             assertEquals('c', Conversion.intToHexDigitMsb0(3));
             assertEquals('f', Conversion.intToHexDigitMsb0(15));

             assertThrows(IllegalArgumentException.class, () -> Conversion.intToHexDigitMsb0(16));
         }

         @Test
         void binaryToByte() {
             boolean[] src = {true, false, true};
             byte result = Conversion.binaryToByte(src, 0, (byte)0, 0, 3);
             assertEquals(5, result);

             assertThrows(IllegalArgumentException.class,
                 () -> Conversion.binaryToByte(new boolean[10], 0, (byte)0, 0, 10));
         }

         @Test
         void byteToBinary() {
             boolean[] dst = new boolean[4];
             boolean[] expected = {false, true, false, true};
             assertArrayEquals(expected, Conversion.byteToBinary((byte)10, 0, dst, 0, 4));
         }

         @Test
         void hexDigitMsb0ToInt() {
             assertEquals(5, Conversion.hexDigitMsb0ToInt('A'));
             assertEquals(0xF, Conversion.hexDigitMsb0ToInt('f'));
             assertEquals(0x0, Conversion.hexDigitMsb0ToInt('0'));

             assertThrows(IllegalArgumentException.class, () -> Conversion.hexDigitMsb0ToInt('x'));
         }

         @Test
         void hexToInt() {
             String hex = "1a";
             int result = Conversion.hexToInt(hex, 0, 0, 0, 2);
             assertEquals(0xa1, result);
         }

         @Test
         void byteArrayToUuid() {
             byte[] bytes = new byte[16];
             UUID uuid = Conversion.byteArrayToUuid(bytes, 0);
             assertEquals(0, uuid.getMostSignificantBits());
             assertEquals(0, uuid.getLeastSignificantBits());

             assertThrows(IllegalArgumentException.class,
                 () -> Conversion.byteArrayToUuid(new byte[15], 0));
         }

         @Test
         void uuidToByteArray() {
             UUID uuid = UUID.randomUUID();
             byte[] bytes = new byte[16];
             byte[] result = Conversion.uuidToByteArray(uuid, bytes, 0, 16);
             assertEquals(uuid, Conversion.byteArrayToUuid(result, 0));

             assertThrows(IllegalArgumentException.class,
                 () -> Conversion.uuidToByteArray(uuid, new byte[10], 0, 17));
         }

         @Test
         void binaryToInt() {
             boolean[] src = {true, false, true};
             int result = Conversion.binaryToInt(src, 0, 0, 0, 3);
             assertEquals(5, result);
         }

         @Test
         void binaryToLong() {
             boolean[] src = {true, false, true};
             long result = Conversion.binaryToLong(src, 0, 0L, 0, 3);
             assertEquals(5L, result);
         }

         @Test
         void binaryToShort() {
             boolean[] src = {true, false, true};
             short result = Conversion.binaryToShort(src, 0, (short)0, 0, 3);
             assertEquals(5, result);
         }

         @Test
         void byteArrayToInt() {
             byte[] src = {0x34, 0x12};
             int result = Conversion.byteArrayToInt(src, 0, 0, 0, 2);
             assertEquals(0x1234, result);
         }

         @Test
         void byteArrayToLong() {
             byte[] src = {0x34, 0x12};
             long result = Conversion.byteArrayToLong(src, 0, 0L, 0, 2);
             assertEquals(0x1234L, result);
         }

         @Test
         void byteArrayToShort() {
             byte[] src = {0x34, 0x12};
             short result = Conversion.byteArrayToShort(src, 0, (short)0, 0, 2);
             assertEquals(0x1234, result);
         }

         @Test
         void byteToHex() {
             String result = Conversion.byteToHex((byte)0xAB, 0, "", 0, 2);
             assertEquals("ba", result);
         }

         @Test
         void hexDigitMsb0ToBinary() {
             boolean[] expected = {true, true, false, true};
             assertArrayEquals(expected, Conversion.hexDigitMsb0ToBinary('d'));
         }

         @Test
         void hexDigitToInt() {
             assertEquals(10, Conversion.hexDigitToInt('A'));
             assertThrows(IllegalArgumentException.class, () -> Conversion.hexDigitToInt('X'));
         }

         @Test
         void hexToByte() {
             byte result = Conversion.hexToByte("1a", 0, (byte)0, 0, 2);
             assertEquals((byte)0xa1, result);
         }

         @Test
         void hexToLong() {
             long result = Conversion.hexToLong("1a", 0, 0L, 0, 2);
             assertEquals(0xa1L, result);
         }

         @Test
         void hexToShort() {
             short result = Conversion.hexToShort("1a", 0, (short)0, 0, 2);
             assertEquals((short)0xa1, result);
         }

         @Test
         void intArrayToLong() {
             int[] src = {0x1234, 0x5678};
             long result = Conversion.intArrayToLong(src, 0, 0L, 0, 2);
             assertEquals(0x567800001234L, result);
         }

         @Test
         void intToBinary() {
             boolean[] dst = new boolean[4];
             boolean[] expected = {false, true, false, true};
             assertArrayEquals(expected, Conversion.intToBinary(10, 0, dst, 0, 4));
         }
         @Test
         void intToByteArray() {
             byte[] dst = new byte[2];
             byte[] expected = {0x34, 0x12};
             assertArrayEquals(expected, Conversion.intToByteArray(0x1234, 0, dst, 0, 2));
         }

         @Test
         void intToShortArray() {
             short[] dst = new short[2];
             short[] expected = {0x5678, 0x1234};
             assertArrayEquals(expected, Conversion.intToShortArray(0x12345678, 0, dst, 0, 2));
         }

         @Test
         void longToBinary() {
             boolean[] dst = new boolean[4];
             boolean[] expected = {false, true, false, true};
             assertArrayEquals(expected, Conversion.longToBinary(10L, 0, dst, 0, 4));
         }

         @Test
         void longToByteArray() {
             byte[] dst = new byte[4];
             byte[] expected = {0x78, (byte)0x56, 0x34, 0x12};
             assertArrayEquals(expected, Conversion.longToByteArray(0x12345678L, 0, dst, 0, 4));
         }

         @Test
         void longToHex() {
             String result = Conversion.longToHex(0x12345678L, 0, "", 0, 8);
             assertEquals("87654321", result);
         }

         @Test
         void longToIntArray() {
             int[] dst = new int[2];
             int[] expected = {0x12345678, 0};
             assertArrayEquals(expected, Conversion.longToIntArray(0x12345678L, 0, dst, 0, 2));
         }

         @Test
         void longToShortArray() {
             short[] dst = new short[4];
             short[] expected = {0x5678, 0x1234, 0, 0};
             assertArrayEquals(expected, Conversion.longToShortArray(0x12345678L, 0, dst, 0, 4));
         }

         @Test
         void shortArrayToInt() {
             short[] src = {0x5678, 0x1234};
             int result = Conversion.shortArrayToInt(src, 0, 0, 0, 2);
             assertEquals(0x12345678, result);
         }

         @Test
         void shortArrayToLong() {
             short[] src = {0x5678, 0x1234};
             long result = Conversion.shortArrayToLong(src, 0, 0L, 0, 2);
             assertEquals(0x12345678L, result);
         }

         @Test
         void shortToBinary() {
             boolean[] dst = new boolean[4];
             boolean[] expected = {false, true, false, true};
             assertArrayEquals(expected, Conversion.shortToBinary((short)10, 0, dst, 0, 4));
         }

         @Test
         void shortToByteArray() {
             byte[] dst = new byte[2];
             byte[] expected = {0x34, 0x12};
             assertArrayEquals(expected, Conversion.shortToByteArray((short)0x1234, 0, dst, 0, 2));
         }

         @Test
         void shortToHex() {
             String result = Conversion.shortToHex((short)0x1234, 0, "", 0, 4);
             assertEquals("4321", result);
         }

         @Test
         void conversionConstructor() {
             new Conversion();
         }
}
