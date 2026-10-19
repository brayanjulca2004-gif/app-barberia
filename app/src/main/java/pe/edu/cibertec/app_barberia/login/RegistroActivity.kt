
package pe.edu.cibertec.app_barberia.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody

class
RegistroActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var etTelefono: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var tvVolverLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        etNombre = findViewById(R.id.etNombreRegistro)
        etCorreo = findViewById(R.id.etCorreoRegistro)
        etPassword = findViewById(R.id.etPasswordRegistro)
        etTelefono = findViewById(R.id.etTelefonoRegistro)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        tvVolverLogin = findViewById(R.id.tvVolverLogin)

        btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

        tvVolverLogin.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuario() {
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val password = etPassword.text.toString()
        val telefono = etTelefono.text.toString().trim()

        if (nombre.isEmpty() || correo.isEmpty() ||
            password.isEmpty() || telefono.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Completa todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo válido"
            etCorreo.requestFocus()
            return
        }

        if (telefono.length != 9 || !telefono.all { it.isDigit() }) {
            etTelefono.error = "Ingresa un teléfono de 9 dígitos"
            etTelefono.requestFocus()
            return
        }

        btnRegistrar.isEnabled = false
        btnRegistrar.text = "Registrando..."

        val solicitud = RegistroRequest(
            nombre = nombre,
            correo = correo,
            password = password,
            telefono = telefono
        )

        RetrofitClient.api.registrarUsuario(solicitud)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "CREAR CUENTA"

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@RegistroActivity,
                            "Cuenta creada correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    } else {
                        val detalle = response.errorBody()
                            ?.string()
                            ?.take(200)

                        Toast.makeText(
                            this@RegistroActivity,
                            detalle?.takeIf { it.isNotBlank() }
                                ?: "No se pudo registrar. Código: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<ResponseBody>,
                    t: Throwable
                ) {
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "CREAR CUENTA"

                    Toast.makeText(
                        this@RegistroActivity,
                        "Error de conexión: ${t.localizedMessage ?: "intenta nuevamente"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}