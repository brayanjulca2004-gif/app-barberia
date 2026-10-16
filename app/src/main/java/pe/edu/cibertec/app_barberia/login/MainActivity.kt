package pe.edu.cibertec.app_barberia.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pe.edu.cibertec.app_barberia.home.HomeActivity
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistrarse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        tvRegistrarse = findViewById(R.id.tvRegistrarse)

        btnIniciarSesion.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (correo.isEmpty()) {
                etCorreo.error = "Ingrese su correo"
                etCorreo.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Ingrese su contraseña"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            iniciarSesion(correo, password)
        }

        tvRegistrarse.setOnClickListener {
            val intent = Intent(this@MainActivity, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun iniciarSesion(correo: String, password: String) {
        btnIniciarSesion.isEnabled = false

        val solicitud = LoginRequest(
            correo = correo,
            password = password
        )

        RetrofitClient.api.iniciarSesion(solicitud)
            .enqueue(object : Callback<pe.edu.cibertec.app_barberia.login.UsuarioResponse> {

                override fun onResponse(
                    call: Call<pe.edu.cibertec.app_barberia.login.UsuarioResponse>,
                    response: Response<pe.edu.cibertec.app_barberia.login.UsuarioResponse>
                ) {
                    btnIniciarSesion.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val usuario = response.body()!!

                        // Guardar datos básicos de la sesión
                        getSharedPreferences("BarberApp", MODE_PRIVATE)
                            .edit()
                            .putInt("idUsuario", usuario.idUsuario)
                            .putString("nombreUsuario", usuario.nombre)
                            .putString("correoUsuario", usuario.correo)
                            .putString("fotoUrl", usuario.fotoUrl)
                            .apply()

                        Toast.makeText(
                            this@MainActivity,
                            "Bienvenido, ${usuario.nombre}",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(this@MainActivity, HomeActivity::class.java)
                        )
                        finish()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Correo o contraseña incorrectos (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<pe.edu.cibertec.app_barberia.login.UsuarioResponse>,
                    t: Throwable
                ) {
                    btnIniciarSesion.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "Error de conexión: ${t.localizedMessage ?: "intente nuevamente"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}