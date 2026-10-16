
package pe.edu.cibertec.app_barberia.perfil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class PerfilActivity : AppCompatActivity() {

    private lateinit var imgFotoPerfil: ImageView
    private lateinit var etNombrePerfil: EditText
    private lateinit var etCorreoPerfil: EditText
    private lateinit var etTelefonoPerfil: EditText
    private lateinit var etPasswordActual: EditText
    private lateinit var etPasswordNueva: EditText
    private lateinit var etPasswordConfirmar: EditText
    private lateinit var tvEstadoPerfil: TextView

    private var idUsuario = -1
    private var fotoSeleccionada: Uri? = null

    private val selectorFoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                fotoSeleccionada = uri
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(imgFotoPerfil)

                subirFoto(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        imgFotoPerfil = findViewById(R.id.imgFotoPerfil)
        etNombrePerfil = findViewById(R.id.etNombrePerfil)
        etCorreoPerfil = findViewById(R.id.etCorreoPerfil)
        etTelefonoPerfil = findViewById(R.id.etTelefonoPerfil)
        etPasswordActual = findViewById(R.id.etPasswordActual)
        etPasswordNueva = findViewById(R.id.etPasswordNueva)
        etPasswordConfirmar = findViewById(R.id.etPasswordConfirmar)
        tvEstadoPerfil = findViewById(R.id.tvEstadoPerfil)

        val btnCambiarFoto =
            findViewById<Button>(R.id.btnCambiarFoto)
        val btnGuardarPerfil =
            findViewById<Button>(R.id.btnGuardarPerfil)
        val btnCambiarPassword =
            findViewById<Button>(R.id.btnCambiarPassword)
        val tvVolverPerfil =
            findViewById<TextView>(R.id.tvVolverPerfil)

        val preferencias = getSharedPreferences("BarberApp", MODE_PRIVATE)
        idUsuario = preferencias.getInt("idUsuario", -1)

        tvVolverPerfil.setOnClickListener {
            finish()
        }

        btnCambiarFoto.setOnClickListener {
            selectorFoto.launch("image/*")
        }

        btnGuardarPerfil.setOnClickListener {
            guardarPerfil()
        }

        btnCambiarPassword.setOnClickListener {
            cambiarPassword()
        }

        if (idUsuario == -1) {
            tvEstadoPerfil.text = "No se encontró la sesión del usuario."
            btnGuardarPerfil.isEnabled = false
            btnCambiarPassword.isEnabled = false
            btnCambiarFoto.isEnabled = false
            return
        }

        cargarPerfil()
    }

    private fun cargarPerfil() {
        tvEstadoPerfil.text = "Cargando perfil..."

        RetrofitClient.api.obtenerPerfil(idUsuario)
            .enqueue(object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (!response.isSuccessful) {
                        tvEstadoPerfil.text =
                            "No se pudo cargar el perfil (${response.code()})."
                        return
                    }

                    val datos = response.body() ?: return

                    etNombrePerfil.setText(
                        datos.get("nombre")?.asString ?: ""
                    )
                    etCorreoPerfil.setText(
                        datos.get("correo")?.asString ?: ""
                    )
                    etTelefonoPerfil.setText(
                        if (datos.get("telefono") == null ||
                            datos.get("telefono").isJsonNull
                        ) "" else datos.get("telefono").asString
                    )

                    val foto = datos.get("fotoUrl")
                    if (foto != null && !foto.isJsonNull &&
                        foto.asString.isNotBlank()
                    ) {
                        Glide.with(this@PerfilActivity)
                            .load(foto.asString)
                            .circleCrop()
                            .placeholder(R.drawable.logo_barberapp)
                            .into(imgFotoPerfil)

                        getSharedPreferences(
                            "BarberApp", MODE_PRIVATE
                        ).edit()
                            .putString("fotoUrl", foto.asString)
                            .apply()
                    }

                    tvEstadoPerfil.text = "Perfil cargado correctamente."
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    tvEstadoPerfil.text =
                        "Error de conexión: ${t.localizedMessage}"
                }
            })
    }

    private fun guardarPerfil() {
        val nombre = etNombrePerfil.text.toString().trim()
        val correo = etCorreoPerfil.text.toString().trim()
        val telefono = etTelefonoPerfil.text.toString().trim()

        if (nombre.isEmpty() || correo.isEmpty()) {
            tvEstadoPerfil.text = "Completa el nombre y el correo."
            return
        }

        val datos = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("correo", correo)
            addProperty("telefono", telefono)
        }

        tvEstadoPerfil.text = "Guardando cambios..."

        RetrofitClient.api.actualizarPerfil(idUsuario, datos)
            .enqueue(object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (response.isSuccessful) {
                        getSharedPreferences(
                            "BarberApp", MODE_PRIVATE
                        ).edit()
                            .putString("nombreUsuario", nombre)
                            .putString("correoUsuario", correo)
                            .apply()

                        tvEstadoPerfil.text =
                            "Datos actualizados correctamente."

                        Toast.makeText(
                            this@PerfilActivity,
                            "Perfil guardado",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        tvEstadoPerfil.text =
                            "No se pudo guardar (${response.code()}). " +
                                    mensajeError(response)
                    }
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    tvEstadoPerfil.text =
                        "Error de conexión: ${t.localizedMessage}"
                }
            })
    }

    private fun cambiarPassword() {
        val actual = etPasswordActual.text.toString()
        val nueva = etPasswordNueva.text.toString()
        val confirmar = etPasswordConfirmar.text.toString()

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            tvEstadoPerfil.text = "Completa los tres campos de contraseña."
            return
        }

        if (nueva.length < 8) {
            tvEstadoPerfil.text =
                "La nueva contraseña debe tener al menos 8 caracteres."
            return
        }

        if (nueva != confirmar) {
            tvEstadoPerfil.text =
                "Las nuevas contraseñas no coinciden."
            return
        }

        val datos = JsonObject().apply {
            addProperty("passwordActual", actual)
            addProperty("passwordNueva", nueva)
            addProperty("passwordConfirmar", confirmar)
        }

        tvEstadoPerfil.text = "Actualizando contraseña..."

        RetrofitClient.api.cambiarPassword(idUsuario, datos)
            .enqueue(object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (response.isSuccessful) {
                        etPasswordActual.text.clear()
                        etPasswordNueva.text.clear()
                        etPasswordConfirmar.text.clear()

                        tvEstadoPerfil.text =
                            "Contraseña actualizada correctamente."
                    } else {
                        tvEstadoPerfil.text =
                            "No se pudo cambiar la contraseña (${response.code()}). " +
                                    mensajeError(response)
                    }
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    tvEstadoPerfil.text =
                        "Error de conexión: ${t.localizedMessage}"
                }
            })
    }

    private fun subirFoto(uri: Uri) {
        try {
            val archivo = copiarUriAArchivo(uri)
            val requestBody = archivo.asRequestBody(
                (contentResolver.getType(uri)
                    ?: "image/jpeg").toMediaTypeOrNull()
            )

            val parte = MultipartBody.Part.createFormData(
                "foto",
                archivo.name,
                requestBody
            )

            tvEstadoPerfil.text = "Subiendo foto..."

            RetrofitClient.api.subirFotoPerfil(idUsuario, parte)
                .enqueue(object : Callback<JsonObject> {

                    override fun onResponse(
                        call: Call<JsonObject>,
                        response: Response<JsonObject>
                    ) {
                        archivo.delete()

                        if (response.isSuccessful) {
                            val datos = response.body()
                            val foto = datos?.get("fotoUrl")

                            if (foto != null && !foto.isJsonNull) {
                                getSharedPreferences(
                                    "BarberApp", MODE_PRIVATE
                                ).edit()
                                    .putString("fotoUrl", foto.asString)
                                    .apply()

                                Glide.with(this@PerfilActivity)
                                    .load(foto.asString)
                                    .circleCrop()
                                    .into(imgFotoPerfil)
                            }

                            tvEstadoPerfil.text =
                                "Foto actualizada correctamente."
                        } else {
                            tvEstadoPerfil.text =
                                "No se pudo subir la foto (${response.code()}). " +
                                        mensajeError(response)
                        }
                    }

                    override fun onFailure(
                        call: Call<JsonObject>,
                        t: Throwable
                    ) {
                        archivo.delete()
                        tvEstadoPerfil.text =
                            "Error al subir la foto: ${t.localizedMessage}"
                    }
                })

        } catch (e: Exception) {
            tvEstadoPerfil.text =
                "No se pudo leer la imagen seleccionada."
        }
    }

    private fun copiarUriAArchivo(uri: Uri): File {
        val nombre = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else null
        } ?: "foto_perfil.jpg"

        val extension = nombre.substringAfterLast(
            '.', "jpg"
        ).replace(Regex("[^A-Za-z0-9]"), "")

        val archivo = File(
            cacheDir,
            "perfil_${System.currentTimeMillis()}.$extension"
        )

        contentResolver.openInputStream(uri).use { entrada ->
            requireNotNull(entrada) { "No se pudo abrir la imagen." }

            FileOutputStream(archivo).use { salida ->
                entrada.copyTo(salida)
            }
        }

        return archivo
    }

    private fun mensajeError(
        response: Response<JsonObject>
    ): String {
        return try {
            response.errorBody()?.string()
                ?.let { org.json.JSONObject(it).optString("mensaje") }
                ?.takeIf { it.isNotBlank() }
                ?: "Revisa los datos e inténtalo nuevamente."
        } catch (e: Exception) {
            "Revisa los datos e inténtalo nuevamente."
        }
    }
}