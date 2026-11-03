package com.example.gateopener

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Прозрачная Activity без интерфейса, которая запускается при нажатии кнопки «Открыть ворота»
 * на виджете и мгновенно совершает телефонный вызов (Intent.ACTION_CALL), не открывая карточку контакта.
 */
class CallActionActivity : AppCompatActivity() {

    companion object {
        private const val REQ_CALL_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initiateGateCall()
    }

    private fun initiateGateCall() {
        val rawNumber = GatePrefs.getPhoneNumber(this)
        if (rawNumber.isBlank()) {
            Toast.makeText(
                this,
                "Укажите номер телефона ворот в настройках приложения",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQ_CALL_PERMISSION
            )
            return
        }

        placeDirectCall(rawNumber)
    }

    private fun placeDirectCall(phoneNumber: String) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.fromParts("tel", phoneNumber, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(callIntent)
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Нет разрешения на совершение прямого звонка",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Ошибка вызова: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CALL_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val number = GatePrefs.getPhoneNumber(this)
                if (number.isNotBlank()) {
                    placeDirectCall(number)
                    return
                }
            } else {
                Toast.makeText(
                    this,
                    "Для открытия ворот по кнопке необходимо разрешить прямые телефонные звонки",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        finish()
    }
}
