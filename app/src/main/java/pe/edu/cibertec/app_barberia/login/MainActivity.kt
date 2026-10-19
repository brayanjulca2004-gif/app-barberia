package pe.edu.cibertec.app_barberia.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import pe.edu.cibertec.app_barberia.database.BarberiaRoomDatabase
import pe.edu.cibertec.app_barberia.database.entity.UsuarioEntity
import pe.edu.cibertec.app_barberia.home.HomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistrarse: TextView
    private lateinit var imgLogo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        tvRegistrarse = findViewById(R.id.tvRegistrarse)
        imgLogo = findViewById(R.id.imgLogo)

        // Cargar dinámicamente la foto del administrador principal (ID 7) en el logo del Login
        cargarLogoDesdeAdmin()

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

    private fun cargarLogoDesdeAdmin() {
        val idAdmin = 7

        Glide.with(this)
            .load(R.drawable.logo_barberapp)
            .circleCrop()
            .into(imgLogo)

        RetrofitClient.api.obtenerPerfil(idAdmin).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val foto = response.body()?.get("fotoUrl")
                    if (foto != null && !foto.isJsonNull && foto.asString.isNotBlank()) {
                        val url = foto.asString
                        val urlFinal = if (url.startsWith("http")) url
                        else "https://storage.googleapis.com/barberapp-storage-2026/usuarios/$url"

                        Glide.with(this@MainActivity)
                            .load(urlFinal)
                            .circleCrop()
                            .placeholder(R.drawable.logo_barberapp)
                            .error(R.drawable.logo_barberapp)
                            .into(imgLogo)
                    }
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                // Si no hay conexión, se mantiene el logo local predeterminado en círculo
            }
        })
    }

    private fun iniciarSesion(correo: String, password: String) {
        btnIniciarSesion.isEnabled = false

        val solicitud = LoginRequest(
            correo = correo,
            password = password
        )

        RetrofitClient.api.iniciarSesion(solicitud)
            .enqueue(object : Callback<UsuarioResponse> {

                override fun onResponse(
                    call: Call<UsuarioResponse>,
                    response: Response<UsuarioResponse>
                ) {
                    btnIniciarSesion.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val usuario = response.body()!!

                        // 1. Guardar datos básicos de la sesión en SharedPreferences
                        guardarSesionEnPreferencias(
                            usuario.idUsuario,
                            usuario.nombre,
                            usuario.correo,
                            usuario.fotoUrl
                        )

                        // 2. Guardar usuario en SQLite (Room)
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = BarberiaRoomDatabase.getDatabase(applicationContext)
                            val usuarioEntity = UsuarioEntity(
                                idUsuario = usuario.idUsuario,
                                nombre = usuario.nombre,
                                correo = usuario.correo,
                                password = password,
                                fotoUrl = usuario.fotoUrl
                            )
                            db.usuarioDao().guardarUsuario(usuarioEntity)
                        }

                        Toast.makeText(
                            this@MainActivity,
                            "Bienvenido, ${usuario.nombre}",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
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
                    call: Call<UsuarioResponse>,
                    t: Throwable
                ) {
                    // Si falla la conexión al servidor, verificamos contra SQLite (Room)
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = BarberiaRoomDatabase.getDatabase(applicationContext)
                        val usuarioLocal = db.usuarioDao().loginOffline(correo, password)

                        withContext(Dispatchers.Main) {
                            btnIniciarSesion.isEnabled = true

                            if (usuarioLocal != null) {
                                guardarSesionEnPreferencias(
                                    usuarioLocal.idUsuario,
                                    usuarioLocal.nombre,
                                    usuarioLocal.correo,
                                    usuarioLocal.fotoUrl
                                )

                                Toast.makeText(
                                    this@MainActivity,
                                    "Modo Offline: Bienvenido, ${usuarioLocal.nombre}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error de conexión: ${t.localizedMessage ?: "intente nuevamente"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            })
    }

    private fun guardarSesionEnPreferencias(id: Int, nombre: String, correo: String, fotoUrl: String?) {
        getSharedPreferences("BarberApp", MODE_PRIVATE)
            .edit()
            .putInt("idUsuario", id)
            .putString("nombreUsuario", nombre)
            .putString("correoUsuario", correo)
            .putString("fotoUrl", fotoUrl)
            .apply()
    }
}