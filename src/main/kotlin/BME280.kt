// Distributed with a free-will license.
// Use it any way you want, profit or free, provided it fits in the licenses of its associated works.
// BME280
// This code is designed to work with the BME280_I2CS I2C Mini Module available from ControlEverything.com.
// https://www.controleverything.com/content/Humidity?sku=BME280_I2CS#tabs-0-product_tabset-2

import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CFactory
import kotlin.experimental.and

object BME280 {
    @JvmStatic
    fun main(args: Array<String>) {
        // Create I2C bus
        val bus = I2CFactory.getInstance(I2CBus.BUS_1)
        // Get I2C device, BME280 I2C address is 0x76(108)
        val device = bus.getDevice(0x76)

        // Read 24 bytes of data from address 0x88(136)
        val b1 = ByteArray(24)
        device.read(0x88, b1, 0, 24)

        // Convert the data
        // temp coefficients
        val dig_T1 = (b1[0] and 0xFF.toByte()) + (b1[1] and 0xFF.toByte()) * 256
        var dig_T2 = (b1[2] and 0xFF.toByte()) + (b1[3] and 0xFF.toByte()) * 256
        if (dig_T2 > 32767) {
            dig_T2 -= 65536
        }
        var dig_T3 = (b1[4] and 0xFF.toByte()) + (b1[5] and 0xFF.toByte()) * 256
        if (dig_T3 > 32767) {
            dig_T3 -= 65536
        }

        // pressure coefficients
        val dig_P1 = (b1[6] and 0xFF.toByte()) + (b1[7] and 0xFF.toByte()) * 256
        var dig_P2 = (b1[8] and 0xFF.toByte()) + (b1[9] and 0xFF.toByte()) * 256
        if (dig_P2 > 32767) {
            dig_P2 -= 65536
        }
        var dig_P3 = (b1[10] and 0xFF.toByte()) + (b1[11] and 0xFF.toByte()) * 256
        if (dig_P3 > 32767) {
            dig_P3 -= 65536
        }
        var dig_P4 = (b1[12] and 0xFF.toByte()) + (b1[13] and 0xFF.toByte()) * 256
        if (dig_P4 > 32767) {
            dig_P4 -= 65536
        }
        var dig_P5 = (b1[14] and 0xFF.toByte()) + (b1[15] and 0xFF.toByte()) * 256
        if (dig_P5 > 32767) {
            dig_P5 -= 65536
        }
        var dig_P6 = (b1[16] and 0xFF.toByte()) + (b1[17] and 0xFF.toByte()) * 256
        if (dig_P6 > 32767) {
            dig_P6 -= 65536
        }
        var dig_P7 = (b1[18] and 0xFF.toByte()) + (b1[19] and 0xFF.toByte()) * 256
        if (dig_P7 > 32767) {
            dig_P7 -= 65536
        }
        var dig_P8 = (b1[20] and 0xFF.toByte()) + (b1[21] and 0xFF.toByte()) * 256
        if (dig_P8 > 32767) {
            dig_P8 -= 65536
        }
        var dig_P9 = (b1[22] and 0xFF.toByte()) + (b1[23] and 0xFF.toByte()) * 256
        if (dig_P9 > 32767) {
            dig_P9 -= 65536
        }

        // Read 1 byte of data from address 0xA1(161)
        val dig_H1 = device.read(0xA1) as Byte and 0xFF.toByte()

        // Read 7 bytes of data from address 0xE1(225)
        device.read(0xE1, b1, 0, 7)

        // Convert the data
        // humidity coefficients
        var dig_H2 = (b1[0] and 0xFF.toByte()) + b1[1] * 256
        if (dig_H2 > 32767) {
            dig_H2 -= 65536
        }
        val dig_H3 = b1[2] and 0xFF.toByte()
        var dig_H4 = (b1[3] and 0xFF.toByte()) * 16 + (b1[4] and 0xF)
        if (dig_H4 > 32767) {
            dig_H4 -= 65536
        }
        var dig_H5: Int = (b1[4] and 0xFF.toByte()) / 16 + (b1[5] and 0xFF.toByte()) * 16
        if (dig_H5 > 32767) {
            dig_H5 -= 65536
        }
        var dig_H6: Int = (b1[6] and 0xFF.toByte()).toInt()
        if (dig_H6 > 127) {
            dig_H6 -= 256
        }

        // Select control humidity register
        // Humidity over sampling rate = 1
        device.write(0xF2, 0x01.toByte())
        // Select control measurement register
        // Normal mode, temp and pressure over sampling rate = 1
        device.write(0xF4, 0x27.toByte())
        // Select config register
        // Stand_by time = 1000 ms
        device.write(0xF5, 0xA0.toByte())

        // Read 8 bytes of data from address 0xF7(247)
        // pressure msb1, pressure msb, pressure lsb, temp msb1, temp msb, temp lsb, humidity lsb, humidity msb
        val data = ByteArray(8)
        device.read(0xF7, data, 0, 8)

        // Convert pressure and temperature data to 19-bits
        val adc_p =
            ((data[0] and 0xFF.toByte()).toLong() * 65536 + (data[1] and 0xFF.toByte()).toLong() * 256 + (data[2] and 0xF0.toByte()).toLong()) / 16
        val adc_t =
            ((data[3] and 0xFF.toByte()).toLong() * 65536 + (data[4] and 0xFF.toByte()).toLong() * 256 + (data[5] and 0xF0.toByte()).toLong()) / 16
        // Convert the humidity data
        val adc_h = (data[6] and 0xFF.toByte()).toLong() * 256 + (data[7] and 0xFF.toByte()).toLong()

        // Temperature offset calculations
        var var1 = (adc_t.toDouble() / 16384.0 - dig_T1.toDouble() / 1024.0) * dig_T2.toDouble()
        var var2 =
            (adc_t.toDouble() / 131072.0 - dig_T1.toDouble() / 8192.0) * (adc_t.toDouble() / 131072.0 - dig_T1.toDouble() / 8192.0) * dig_T3.toDouble()
        val t_fine = (var1 + var2).toLong().toDouble()
        val cTemp = (var1 + var2) / 5120.0
        val fTemp = cTemp * 1.8 + 32

        // Pressure offset calculations
        var1 = t_fine / 2.0 - 64000.0
        var2 = var1 * var1 * dig_P6.toDouble() / 32768.0
        var2 += var1 * dig_P5.toDouble() * 2.0
        var2 = var2 / 4.0 + dig_P4.toDouble() * 65536.0
        var1 = (dig_P3.toDouble() * var1 * var1 / 524288.0 + dig_P2.toDouble() * var1) / 524288.0
        var1 = (1.0 + var1 / 32768.0) * dig_P1.toDouble()
        var p = 1048576.0 - adc_p.toDouble()
        p = (p - var2 / 4096.0) * 6250.0 / var1
        var1 = dig_P9.toDouble() * p * p / 2147483648.0
        var2 = p * dig_P8.toDouble() / 32768.0
        val pressure = (p + (var1 + var2 + dig_P7.toDouble()) / 16.0) / 100

        // Humidity offset calculations
        var var_H = t_fine - 76800.0
        var_H =
            (adc_h - (dig_H4 * 64.0 + dig_H5 / 16384.0 * var_H)) * (dig_H2 / 65536.0 * (1.0 + dig_H6 / 67108864.0 * var_H * (1.0 + dig_H3 / 67108864.0 * var_H)))
        var humidity = var_H * (1.0 - dig_H1 * var_H / 524288.0)
        if (humidity > 100.0) {
            humidity = 100.0
        } else if (humidity < 0.0) {
            humidity = 0.0
        }

        // Output data to screen
        System.out.printf("Temperature in Celsius : %.2f C %n", cTemp)
        System.out.printf("Temperature in Fahrenheit : %.2f F %n", fTemp)
        System.out.printf("Pressure : %.2f hPa %n", pressure)
        System.out.printf("Relative Humidity : %.2f %% RH %n", humidity)
    }
}